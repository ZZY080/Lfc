import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  CreatePostBodyDto,
  PostFeedQueryDto,
  PostFeedResultDto,
  UpdatePostBodyDto,
} from '@module/post/dto/consumer-post.dto';
import { ConsumerPostSocialService } from '@module/post/service/consumer-post-social.service';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class ConsumerPostService {
  private static readonly MAX_IMAGES = 20;

  constructor(
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly postSocialService: ConsumerPostSocialService,
  ) {}

  async create(userId: number, body: CreatePostBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const normalized = this.normalizePostBody(body);
    const post = this.postRepository.create({
      ...normalized,
      authorId: userId,
      likeCount: 0,
    });
    return this.postRepository.save(post);
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
    const skip = (page - 1) * limit;
    const sort = query.sort ?? (query.tab === '最新' ? 'latest' : 'recommend');

    const qb = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.author', 'author');

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere('(post.title LIKE :keyword OR post.content LIKE :keyword)', {
        keyword,
      });
    } else if (query.tab && !['推荐', '最新'].includes(query.tab)) {
      const tabKeyword = `%${query.tab.trim()}%`;
      qb.andWhere('(post.title LIKE :tabKeyword OR post.content LIKE :tabKeyword)', {
        tabKeyword,
      });
    }

    if (sort === 'latest') {
      qb.orderBy('post.createdAt', 'DESC');
    } else {
      qb.orderBy('post.id', 'DESC');
    }

    const [items, total] = await qb.skip(skip).take(limit).getManyAndCount();
    const enrichedItems = await this.postSocialService.enrichPosts(items, userId);

    return {
      items: enrichedItems,
      total,
      page,
      limit,
      hasMore: skip + items.length < total,
    };
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
    return this.postSocialService.enrichPosts(posts, userId);
  }

  async findByAuthor(authorId: number, viewerId?: number) {
    const posts = await this.postRepository.find({
      where: { authorId },
      relations: ['author'],
      order: { createdAt: 'DESC' },
    });
    return this.postSocialService.enrichPosts(posts, viewerId);
  }

  async findByAuthorPaginated(
    authorId: number,
    page?: number,
    limit?: number,
    viewerId?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const [posts, total] = await this.postRepository.findAndCount({
      where: { authorId },
      relations: ['author'],
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    const items = await this.postSocialService.enrichPosts(posts, viewerId);
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
    return this.postSocialService.enrichPost(post, userId);
  }

  async update(userId: number, id: number, body: UpdatePostBodyDto) {
    const post = await this.findOne(id);
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权修改该信息');
    }
    const normalized = this.normalizePostBody({
      title: body.title ?? post.title,
      content: body.content ?? post.content,
      images: body.images ?? post.images ?? [],
    });
    Object.assign(post, normalized);
    return this.postRepository.save(post);
  }

  async remove(userId: number, id: number) {
    const post = await this.findOne(id);
    if (post.authorId !== userId) {
      throw new ForbiddenException('无权删除该信息');
    }
    await this.postRepository.remove(post);
    return { message: '删除成功' };
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
    return { title, content, images };
  }
}
