import {
  BadRequestException,
  ForbiddenException,
  Inject,
  Injectable,
  NotFoundException,
  forwardRef,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { UserFollowEntity } from '@module/user/entity/user-follow.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  CreatePostBodyDto,
  PostFeedQueryDto,
  PostFeedResultDto,
  UpdatePostBodyDto,
} from '@module/post/dto/consumer-post.dto';
import { ConsumerPostSocialService } from '@module/post/service/consumer-post-social.service';
import { ConsumerPostProductService } from '@module/post/service/consumer-post-product.service';
import { UserAlipayService } from '@module/user/service/user-alipay.service';
import { ConsumerPromotionService } from '@module/promotion/service/consumer-promotion.service';
import { NotificationService } from '@module/message/service/notification.service';
import { PostProductEntity } from '@module/post/entity/post-product.entity';
import { PostStatus } from '@shared/enum/post-status.enum';
import { PostProductStatus } from '@shared/enum/product.enum';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';
import { AmapGeocodeService } from '@integration/amap/amap-geocode.service';
import {
  DEFAULT_POST_CATEGORY,
} from '@shared/enum/post-category.enum';
import { FeedChannelService } from '@module/feed-channel/service/feed-channel.service';

@Injectable()
export class ConsumerPostService {
  private static readonly MAX_IMAGES = 20;
  private static readonly MARKETPLACE_TABS = new Set(['好物', '商品', '二手闲置', '闲置']);

  constructor(
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(UserFollowEntity)
    private readonly followRepository: Repository<UserFollowEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly postSocialService: ConsumerPostSocialService,
    private readonly postProductService: ConsumerPostProductService,
    private readonly userAlipayService: UserAlipayService,
    @Inject(forwardRef(() => ConsumerPromotionService))
    private readonly consumerPromotionService: ConsumerPromotionService,
    private readonly amapGeocodeService: AmapGeocodeService,
    private readonly notificationService: NotificationService,
    private readonly feedChannelService: FeedChannelService,
  ) {}

  async create(userId: number, body: CreatePostBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const normalized = this.normalizePostBody(body);
    const latitude = body.latitude ?? null;
    const longitude = body.longitude ?? null;
    const location = await this.resolvePostLocationText({
      latitude,
      longitude,
      location: body.location,
    });
    const post = this.postRepository.create({
      ...normalized,
      authorId: userId,
      likeCount: 0,
      latitude,
      longitude,
      location,
      isVisible: false,
      status: PostStatus.PENDING,
    });
    const saved = await this.postRepository.save(post);

    if (body.product) {
      const price = Number.parseFloat(String(body.product.price ?? 0));
      if (price > 0) {
        await this.userAlipayService.assertCanReceive(userId, '卖家');
      }
      await this.postProductService.createForPost(saved.id, body.product);
    }

    await this.notificationService.sendPostSubmitted(userId, saved);

    return this.findOne(saved.id, userId);
  }

  findAll() {
    return this.postRepository.find({
      relations: ['author'],
      order: { createdAt: 'DESC' },
    });
  }

  async findFeed(
    query: PostFeedQueryDto,
    userId?: number,
  ): Promise<PostFeedResultDto> {
    const page = query.page ?? 1;
    const limit = Math.min(query.limit ?? 10, 30);
    if (query.tab === '关注') {
      return this.findFollowingFeedPaginated(userId, page, limit);
    }
    const sort = query.sort ?? (query.tab === '最新' ? 'latest' : 'recommend');

    if (sort === 'latest') {
      return this.findFeedPaginated(query, userId, page, limit, 'latest');
    }
    return this.findRecommendFeedWithBoostSlots(query, userId, page, limit);
  }

  private async findFollowingFeedPaginated(
    userId: number | undefined,
    page: number,
    limit: number,
  ): Promise<PostFeedResultDto> {
    if (!userId) {
      return {
        items: [],
        total: 0,
        page,
        limit,
        hasMore: false,
      };
    }

    const follows = await this.followRepository.find({
      where: { followerId: userId },
      select: ['followingId'],
    });
    const authorIds = follows.map((item) => item.followingId);
    if (authorIds.length === 0) {
      return {
        items: [],
        total: 0,
        page,
        limit,
        hasMore: false,
      };
    }

    const skip = (page - 1) * limit;
    const qb = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.author', 'author')
      .where('post.isVisible = :visible', { visible: true })
      .andWhere('post.status = :approved', { approved: PostStatus.APPROVED })
      .andWhere('post.authorId IN (:...authorIds)', { authorIds })
      .orderBy('post.createdAt', 'DESC');

    const [items, total] = await qb.skip(skip).take(limit).getManyAndCount();
    const enrichedItems = await this.enrichPostsWithProduct(items, userId);

    return {
      items: enrichedItems,
      total,
      page,
      limit,
      hasMore: skip + items.length < total,
    };
  }

  private async findFeedPaginated(
    query: PostFeedQueryDto,
    userId: number | undefined,
    page: number,
    limit: number,
    sort: 'latest' | 'recommend',
  ): Promise<PostFeedResultDto> {
    const skip = (page - 1) * limit;
    const qb = this.buildFeedQueryBuilder(query);
    this.applyFeedOrdering(qb, sort);
    const [items, total] = await qb.skip(skip).take(limit).getManyAndCount();
    const enrichedItems = await this.enrichPostsWithProduct(items, userId);

    return {
      items: enrichedItems,
      total,
      page,
      limit,
      hasMore: skip + items.length < total,
    };
  }

  private async findRecommendFeedWithBoostSlots(
    query: PostFeedQueryDto,
    userId: number | undefined,
    page: number,
    limit: number,
  ): Promise<PostFeedResultDto> {
    const maxSlots = (await this.consumerPromotionService.getPublicConfig())
      .postMaxFeedSlots;
    const activeBoostedCount =
      await this.consumerPromotionService.countActiveBoostedPosts();
    const firstPageBoostedCount =
      this.consumerPromotionService.getFirstPageBoostedSlotCount(
        activeBoostedCount,
      );

    let boostedPosts: PostEntity[] = [];
    if (page === 1 && firstPageBoostedCount > 0) {
      const boostedQb = this.buildFeedQueryBuilder(query);
      boostedQb
        .andWhere('post.boosted_until > :now', { now: new Date() })
        .orderBy('post.boostBidAmount', 'DESC')
        .addOrderBy('post.lastBoostedAt', 'DESC')
        .take(maxSlots);
      boostedPosts = await boostedQb.getMany();
    }
    const boostedIds = boostedPosts.map((item) => item.id);

    const regularSkip =
      page <= 1 ? 0 : (page - 1) * limit - firstPageBoostedCount;
    const regularTake =
      page === 1
        ? Math.max(limit - boostedPosts.length, 0)
        : limit;

    const regularQb = this.buildFeedQueryBuilder(query);
    if (boostedIds.length > 0) {
      regularQb.andWhere('post.id NOT IN (:...boostedIds)', { boostedIds });
    }
    this.applyFeedOrdering(regularQb, 'latest');

    const [regularPosts, totalRegular] = await regularQb
      .skip(Math.max(regularSkip, 0))
      .take(regularTake)
      .getManyAndCount();

    const total = totalRegular + activeBoostedCount;
    const pagePosts =
      page === 1 ? [...boostedPosts, ...regularPosts] : regularPosts;
    const enrichedItems = await this.enrichPostsWithProduct(pagePosts, userId);

    return {
      items: enrichedItems,
      total,
      page,
      limit,
      hasMore: page * limit < total,
    };
  }

  private buildFeedQueryBuilder(query: PostFeedQueryDto) {
    const isMarketplaceTab =
      query.tab != null && ConsumerPostService.MARKETPLACE_TABS.has(query.tab);

    const qb = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.author', 'author')
      .andWhere('post.isVisible = :visible', { visible: true })
      .andWhere('post.status = :approved', { approved: PostStatus.APPROVED });

    if (isMarketplaceTab) {
      qb.innerJoin(
        PostProductEntity,
        'product',
        'product.postId = post.id AND product.status = :onSale',
        { onSale: PostProductStatus.ON_SALE },
      );
    } else if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere('(post.title LIKE :keyword OR post.content LIKE :keyword)', {
        keyword,
      });
    } else if (query.tab && !['推荐', '最新'].includes(query.tab)) {
      const tab = query.tab.trim();
      const tabKeyword = `%${tab}%`;
      qb.andWhere(
        `(post.category = :tab OR (post.category IS NULL AND (post.title LIKE :tabKeyword OR post.content LIKE :tabKeyword)))`,
        { tab, tabKeyword },
      );
    }

    return qb;
  }

  private applyFeedOrdering(
    qb: ReturnType<Repository<PostEntity>['createQueryBuilder']>,
    sort: 'latest' | 'recommend',
  ) {
    if (sort === 'latest') {
      qb.orderBy('post.createdAt', 'DESC');
      return;
    }

    qb.addSelect(
      'CASE WHEN post.boosted_until IS NOT NULL AND post.boosted_until > CURRENT_TIMESTAMP THEN 1 ELSE 0 END',
      'boost_rank',
    )
      .orderBy('boost_rank', 'DESC')
      .addOrderBy('post.lastBoostedAt', 'DESC')
      .addOrderBy('post.id', 'DESC');
  }

  findMine(userId: number, keyword?: string) {
    return this.findMinePosts(userId, keyword);
  }

  async findMinePosts(userId: number, keyword?: string) {
    const qb = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.author', 'author')
      .where('post.authorId = :userId', { userId });

    if (keyword?.trim()) {
      const normalized = `%${keyword.trim()}%`;
      qb.andWhere('(post.title LIKE :keyword OR post.content LIKE :keyword)', {
        keyword: normalized,
      });
    }

    qb.orderBy('post.createdAt', 'DESC');
    const posts = await qb.getMany();
    return this.enrichPostsWithProduct(posts, userId);
  }

  async findByAuthor(authorId: number, viewerId?: number) {
    const isSelf = viewerId != null && viewerId === authorId;
    const posts = await this.postRepository.find({
      where: isSelf
        ? { authorId }
        : { authorId, isVisible: true, status: PostStatus.APPROVED },
      relations: ['author'],
      order: { createdAt: 'DESC' },
    });
    return this.enrichPostsWithProduct(posts, viewerId);
  }

  async findByAuthorPaginated(
    authorId: number,
    page?: number,
    limit?: number,
    viewerId?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const isSelf = viewerId != null && viewerId === authorId;
    const [posts, total] = await this.postRepository.findAndCount({
      where: isSelf
        ? { authorId }
        : { authorId, isVisible: true, status: PostStatus.APPROVED },
      relations: ['author'],
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    const items = await this.enrichPostsWithProduct(posts, viewerId);
    return createPaginatedResult(items, total, normalizedPage, normalizedLimit);
  }

  async findOne(id: number, userId?: number) {
    const post = await this.postRepository.findOne({
      where: { id },
      relations: ['author'],
    });
    if (!post) {
      throw new NotFoundException('信息不存在');
    }
    const isAuthor = userId != null && post.authorId === userId;
    if (
      !isAuthor &&
      (post.status !== PostStatus.APPROVED || !post.isVisible)
    ) {
      throw new NotFoundException('信息不存在');
    }
    await this.postRepository.increment({ id }, 'viewCount', 1);
    post.viewCount = (post.viewCount ?? 0) + 1;
    const [enriched] = await this.enrichPostsWithProduct([post], userId);
    return enriched;
  }

  async update(userId: number, id: number, body: UpdatePostBodyDto) {
    const post = await this.postRepository.findOne({
      where: { id },
      relations: ['author'],
    });
    if (!post) {
      throw new NotFoundException('信息不存在');
    }
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权修改该信息');
    }
    const normalized = this.normalizePostBody({
      title: body.title ?? post.title,
      content: body.content ?? post.content,
      images: body.images ?? post.images ?? [],
      category: body.category ?? post.category,
    });
    Object.assign(post, normalized);
    if (body.latitude !== undefined) {
      post.latitude = body.latitude;
    }
    if (body.longitude !== undefined) {
      post.longitude = body.longitude;
    }
    if (body.location !== undefined) {
      post.location = body.location?.trim() || null;
    }
    if (!post.location && post.latitude != null && post.longitude != null) {
      post.location = await this.resolvePostLocationText({
        latitude: post.latitude,
        longitude: post.longitude,
        location: null,
      });
    }
    const shouldResubmit = post.status === PostStatus.REJECTED;
    if (shouldResubmit) {
      post.status = PostStatus.PENDING;
      post.reviewComment = null;
      post.isVisible = false;
    }
    const saved = await this.postRepository.save(post);

    if (shouldResubmit) {
      await this.notificationService.sendPostSubmitted(userId, saved);
    }

    if (body.product === null) {
      const existing = await this.postProductService.findByPostId(id);
      if (existing) {
        await this.postProductService.offShelf(id, userId);
      }
    } else if (body.product) {
      const existing = await this.postProductService.findByPostId(id);
      if (existing) {
        await this.postProductService.updateForPost(id, userId, body.product);
      } else {
        await this.postProductService.createForPost(id, body.product);
      }
    }

    return this.findOne(saved.id, userId);
  }

  async remove(userId: number, id: number) {
    const post = await this.postRepository.findOne({
      where: { id },
      relations: ['author'],
    });
    if (!post) {
      throw new NotFoundException('信息不存在');
    }
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权删除该信息');
    }
    await this.postRepository.remove(post);
    return { message: '删除成功' };
  }

  async offShelf(userId: number, postId: number) {
    const post = await this.assertOwnedPost(userId, postId);
    if (post.status === PostStatus.OFF_SHELF && !post.isVisible) {
      return this.findOne(postId, userId);
    }
    post.isVisible = false;
    post.status = PostStatus.OFF_SHELF;
    post.boostedUntil = null;
    await this.postRepository.save(post);
    const product = await this.postProductService.findByPostId(postId);
    if (product?.status === PostProductStatus.ON_SALE) {
      await this.postProductService.offShelf(postId, userId);
    }
    return this.findOne(postId, userId);
  }

  async onShelf(userId: number, postId: number) {
    const post = await this.assertOwnedPost(userId, postId);
    if (post.status === PostStatus.APPROVED && post.isVisible) {
      return this.findOne(postId, userId);
    }
    post.isVisible = false;
    post.status = PostStatus.PENDING;
    post.reviewComment = null;
    await this.postRepository.save(post);
    await this.notificationService.sendPostSubmitted(userId, post);
    return this.findOne(postId, userId);
  }

  offShelfProduct(userId: number, postId: number) {
    return this.postProductService.offShelf(postId, userId);
  }

  onShelfProduct(userId: number, postId: number) {
    return this.postProductService.onShelf(postId, userId);
  }

  private async assertOwnedPost(userId: number, postId: number) {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post) {
      throw new NotFoundException('信息不存在');
    }
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权操作该信息');
    }
    return post;
  }

  private async enrichPostsWithProduct(posts: PostEntity[], userId?: number) {
    const productMap = await this.postProductService.findMapByPostIds(
      posts.map((post) => post.id),
    );
    const socialPosts = await this.postSocialService.enrichPosts(posts, userId);
    const locatedPosts = await Promise.all(
      socialPosts.map((post) => this.ensurePostLocation(post)),
    );
    return locatedPosts.map((post) => {
      const product = productMap.get(post.id);
      const withProduct = {
        ...post,
        product: product ? this.postProductService.toDto(product) : null,
      };
      return this.consumerPromotionService.attachPostPromotion(
        withProduct as PostEntity,
        userId,
      );
    });
  }

  private async ensurePostLocation(post: PostEntity): Promise<PostEntity> {
    if (post.location || post.latitude == null || post.longitude == null) {
      return post;
    }

    const location = await this.amapGeocodeService.reverseGeocode(
      post.longitude,
      post.latitude,
    );
    if (!location) {
      return post;
    }

    await this.postRepository.update({ id: post.id }, { location });
    return { ...post, location };
  }

  private async resolvePostLocationText(body: {
    latitude?: number | null;
    longitude?: number | null;
    location?: string | null;
  }): Promise<string | null> {
    const trimmed = body.location?.trim();
    if (trimmed) {
      return trimmed;
    }
    if (body.latitude == null || body.longitude == null) {
      return null;
    }
    return this.amapGeocodeService.reverseGeocode(
      body.longitude,
      body.latitude,
    );
  }

  private normalizePostBody(body: CreatePostBodyDto) {
    const content = body.content?.trim() ?? '';
    const images = (body.images ?? []).filter(Boolean);
    if (images.length > ConsumerPostService.MAX_IMAGES) {
      throw new BadRequestException(
        `最多上传${ConsumerPostService.MAX_IMAGES}张图片`,
      );
    }
    if (!content && images.length === 0) {
      throw new BadRequestException('请填写文字或上传至少一张图片');
    }
    const title =
      body.title?.trim() ||
      content.slice(0, 30) ||
      (images.length > 0 ? '图片笔记' : '校园笔记');
    const category = this.normalizeCategory(body.category);
    return { title, content, images, category };
  }

  private normalizeCategory(category?: string | null) {
    const trimmed = category?.trim();
    if (!trimmed) {
      return DEFAULT_POST_CATEGORY;
    }
    if (!this.feedChannelService.isValidPublishCategory(trimmed)) {
      throw new BadRequestException('笔记类型不正确');
    }
    return trimmed;
  }
}
