import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { PostProductEntity } from '@module/post/entity/post-product.entity';
import { PostLikeEntity } from '@module/post/entity/post-like.entity';
import { PostFavoriteEntity } from '@module/post/entity/post-favorite.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  AdminPostListQueryDto,
  AdminUpdatePostBodyDto,
  AdminUpdatePostVisibilityBodyDto,
  ReviewPostBodyDto,
} from '@module/post/dto/admin-post.dto';
import { AdminUpdatePostProductBodyDto } from '@module/post/dto/admin-post-detail.dto';
import { NotificationService } from '@module/message/service/notification.service';
import { PostStatus } from '@shared/enum/post-status.enum';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class AdminPostService {
  private static readonly MAX_IMAGES = 20;

  constructor(
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(PostProductEntity)
    private readonly productRepository: Repository<PostProductEntity>,
    @InjectRepository(PostLikeEntity)
    private readonly likeRepository: Repository<PostLikeEntity>,
    @InjectRepository(PostFavoriteEntity)
    private readonly favoriteRepository: Repository<PostFavoriteEntity>,
    @InjectRepository(PostCommentEntity)
    private readonly commentRepository: Repository<PostCommentEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly notificationService: NotificationService,
  ) {}

  async findPending(adminUserId: number, query: AdminPostListQueryDto) {
    return this.findAll(adminUserId, {
      ...query,
      status: PostStatus.PENDING,
    });
  }

  async findAll(adminUserId: number, query: AdminPostListQueryDto) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);
    const qb = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.author', 'author');

    if (query.visibility === 'visible') {
      qb.andWhere('post.isVisible = :isVisible', { isVisible: true });
    } else if (query.visibility === 'hidden') {
      qb.andWhere('post.isVisible = :isVisible', { isVisible: false });
    }

    if (query.status) {
      qb.andWhere('post.status = :status', { status: query.status });
    }

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere('(post.title LIKE :keyword OR post.content LIKE :keyword)', {
        keyword,
      });
    }

    qb.orderBy('post.createdAt', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();

    return createPaginatedResult(items, total, page, limit);
  }

  async findOne(adminUserId: number, postId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    return this.getPostWithAuthorOrThrow(postId);
  }

  async findDetail(adminUserId: number, postId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const post = await this.getPostWithAuthorOrThrow(postId);
    const product = await this.productRepository.findOne({
      where: { postId },
    });
    return { ...post, product };
  }

  async findComments(
    adminUserId: number,
    postId: number,
    page?: number,
    limit?: number,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    await this.getPostOrThrow(postId);
    const pagination = normalizePagination(page, limit);
    const [items, total] = await this.commentRepository.findAndCount({
      where: { postId },
      relations: ['author'],
      order: { createdAt: 'DESC' },
      skip: pagination.skip,
      take: pagination.limit,
    });
    return createPaginatedResult(
      items,
      total,
      pagination.page,
      pagination.limit,
    );
  }

  async findLikes(
    adminUserId: number,
    postId: number,
    page?: number,
    limit?: number,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    await this.getPostOrThrow(postId);
    const pagination = normalizePagination(page, limit);
    const [items, total] = await this.likeRepository.findAndCount({
      where: { postId },
      relations: ['user'],
      order: { createdAt: 'DESC' },
      skip: pagination.skip,
      take: pagination.limit,
    });
    return createPaginatedResult(
      items,
      total,
      pagination.page,
      pagination.limit,
    );
  }

  async findFavorites(
    adminUserId: number,
    postId: number,
    page?: number,
    limit?: number,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    await this.getPostOrThrow(postId);
    const pagination = normalizePagination(page, limit);
    const [items, total] = await this.favoriteRepository.findAndCount({
      where: { postId },
      relations: ['user'],
      order: { createdAt: 'DESC' },
      skip: pagination.skip,
      take: pagination.limit,
    });
    return createPaginatedResult(
      items,
      total,
      pagination.page,
      pagination.limit,
    );
  }

  async removeLike(adminUserId: number, postId: number, likeId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const post = await this.getPostOrThrow(postId);
    const like = await this.likeRepository.findOne({
      where: { id: likeId, postId },
    });
    if (!like) {
      throw new NotFoundException('点赞记录不存在');
    }
    await this.likeRepository.delete(likeId);
    post.likeCount = Math.max(0, post.likeCount - 1);
    await this.postRepository.save(post);
    return { message: '已移除点赞' };
  }

  async removeFavorite(
    adminUserId: number,
    postId: number,
    favoriteId: number,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const post = await this.getPostOrThrow(postId);
    const favorite = await this.favoriteRepository.findOne({
      where: { id: favoriteId, postId },
    });
    if (!favorite) {
      throw new NotFoundException('收藏记录不存在');
    }
    await this.favoriteRepository.delete(favoriteId);
    post.favoriteCount = Math.max(0, post.favoriteCount - 1);
    await this.postRepository.save(post);
    return { message: '已移除收藏' };
  }

  async updateProduct(
    adminUserId: number,
    postId: number,
    body: AdminUpdatePostProductBodyDto,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    await this.getPostOrThrow(postId);
    const product = await this.productRepository.findOne({ where: { postId } });
    if (!product) {
      throw new NotFoundException('该帖子未关联商品');
    }

    if (body.price !== undefined) product.price = String(body.price);
    if (body.originalPrice !== undefined) {
      product.originalPrice =
        body.originalPrice === null ? null : String(body.originalPrice);
    }
    if (body.category !== undefined) product.category = body.category;
    if (body.condition !== undefined) product.condition = body.condition;
    if (body.deliveryMethod !== undefined) {
      product.deliveryMethod = body.deliveryMethod;
    }
    if (body.status !== undefined) product.status = body.status;

    return this.productRepository.save(product);
  }

  async removeProduct(adminUserId: number, postId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    await this.getPostOrThrow(postId);
    const product = await this.productRepository.findOne({ where: { postId } });
    if (!product) {
      throw new NotFoundException('该帖子未关联商品');
    }
    await this.productRepository.delete(product.id);
    return { message: '商品关联已删除' };
  }

  async update(
    adminUserId: number,
    postId: number,
    body: AdminUpdatePostBodyDto,
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const post = await this.getPostOrThrow(postId);
    const previousStatus = post.status;

    if (body.title !== undefined) post.title = body.title;
    if (body.content !== undefined) post.content = body.content;
    if (body.category !== undefined) post.category = body.category;
    if (body.images !== undefined) {
      const images = body.images.filter(Boolean);
      if (images.length > AdminPostService.MAX_IMAGES) {
        throw new BadRequestException(
          `最多上传${AdminPostService.MAX_IMAGES}张图片`,
        );
      }
      post.images = images;
    }
    if (body.location !== undefined) post.location = body.location;
    if (body.latitude !== undefined) post.latitude = body.latitude;
    if (body.longitude !== undefined) post.longitude = body.longitude;
    if (body.isVisible !== undefined) post.isVisible = body.isVisible;
    if (body.status !== undefined) {
      this.applyReviewStatus(post, body.status, body.reviewComment);
    }

    const saved = await this.postRepository.save(post);
    await this.notifyReviewIfNeeded(
      post.authorId,
      saved,
      body.status,
      previousStatus,
    );
    return saved;
  }

  async review(
    adminUserId: number,
    postId: number,
    body: ReviewPostBodyDto,
  ) {
    return this.update(adminUserId, postId, {
      status: body.status,
      reviewComment: body.reviewComment,
    });
  }

  async updateVisibility(
    adminUserId: number,
    postId: number,
    body: AdminUpdatePostVisibilityBodyDto,
  ) {
    return this.update(adminUserId, postId, { isVisible: body.isVisible });
  }

  async remove(adminUserId: number, postId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    await this.getPostOrThrow(postId);
    await this.postRepository.delete(postId);
    return { message: '帖子已删除' };
  }

  private async getPostOrThrow(postId: number) {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post) {
      throw new NotFoundException('帖子不存在');
    }
    return post;
  }

  private async getPostWithAuthorOrThrow(postId: number) {
    const post = await this.postRepository.findOne({
      where: { id: postId },
      relations: ['author'],
    });
    if (!post) {
      throw new NotFoundException('帖子不存在');
    }
    return post;
  }

  private applyReviewStatus(
    post: PostEntity,
    status: PostStatus,
    reviewComment?: string,
  ) {
    if (status === PostStatus.REJECTED) {
      const comment = reviewComment?.trim();
      if (!comment) {
        throw new BadRequestException('拒绝时需填写审核意见');
      }
      post.reviewComment = comment;
      post.isVisible = false;
    } else if (status === PostStatus.APPROVED) {
      post.reviewComment = null;
      post.isVisible = true;
    } else if (status === PostStatus.OFF_SHELF) {
      post.isVisible = false;
    }
    post.status = status;
  }

  private async notifyReviewIfNeeded(
    authorId: number,
    post: PostEntity,
    nextStatus?: PostStatus,
    previousStatus?: PostStatus,
  ) {
    if (
      !nextStatus ||
      nextStatus === previousStatus ||
      (nextStatus !== PostStatus.APPROVED && nextStatus !== PostStatus.REJECTED)
    ) {
      return;
    }
    await this.notificationService.sendPostReview(authorId, post, nextStatus);
  }
}
