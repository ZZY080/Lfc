import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In, IsNull } from 'typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { PostLikeEntity } from '@module/post/entity/post-like.entity';
import { PostFavoriteEntity } from '@module/post/entity/post-favorite.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
import { PostCommentLikeEntity } from '@module/post/entity/post-comment-like.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import { CreatePostCommentBodyDto } from '@module/post/dto/post-social.dto';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class ConsumerPostSocialService {
  constructor(
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(PostLikeEntity)
    private readonly likeRepository: Repository<PostLikeEntity>,
    @InjectRepository(PostFavoriteEntity)
    private readonly favoriteRepository: Repository<PostFavoriteEntity>,
    @InjectRepository(PostCommentEntity)
    private readonly commentRepository: Repository<PostCommentEntity>,
    @InjectRepository(PostCommentLikeEntity)
    private readonly commentLikeRepository: Repository<PostCommentLikeEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async enrichPost(post: PostEntity, userId?: number) {
    if (!userId) {
      return {
        ...post,
        isLiked: false,
        isFavorited: false,
      };
    }

    const [isLiked, isFavorited] = await Promise.all([
      this.likeRepository.exist({ where: { postId: post.id, userId } }),
      this.favoriteRepository.exist({ where: { postId: post.id, userId } }),
    ]);

    return {
      ...post,
      isLiked,
      isFavorited,
    };
  }

  async enrichPosts(posts: PostEntity[], userId?: number) {
    if (!userId || posts.length === 0) {
      return posts.map((post) => ({
        ...post,
        isLiked: false,
        isFavorited: false,
      }));
    }

    const postIds = posts.map((post) => post.id);
    const [likes, favorites] = await Promise.all([
      this.likeRepository.find({
        where: { userId, postId: In(postIds) },
        select: ['postId'],
      }),
      this.favoriteRepository.find({
        where: { userId, postId: In(postIds) },
        select: ['postId'],
      }),
    ]);

    const likedIds = new Set(likes.map((item) => item.postId));
    const favoritedIds = new Set(favorites.map((item) => item.postId));

    return posts.map((post) => ({
      ...post,
      isLiked: likedIds.has(post.id),
      isFavorited: favoritedIds.has(post.id),
    }));
  }

  async toggleLike(userId: number, postId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const post = await this.findPost(postId);

    const existing = await this.likeRepository.findOne({
      where: { postId, userId },
    });

    if (existing) {
      await this.likeRepository.remove(existing);
      post.likeCount = Math.max(0, post.likeCount - 1);
    } else {
      await this.likeRepository.save(
        this.likeRepository.create({ postId, userId }),
      );
      post.likeCount += 1;
    }

    await this.postRepository.save(post);
    return this.buildSocialState(post, userId);
  }

  async toggleFavorite(userId: number, postId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const post = await this.findPost(postId);

    const existing = await this.favoriteRepository.findOne({
      where: { postId, userId },
    });

    if (existing) {
      await this.favoriteRepository.remove(existing);
      post.favoriteCount = Math.max(0, post.favoriteCount - 1);
    } else {
      await this.favoriteRepository.save(
        this.favoriteRepository.create({ postId, userId }),
      );
      post.favoriteCount += 1;
    }

    await this.postRepository.save(post);
    return this.buildSocialState(post, userId);
  }

  async findComments(
    postId: number,
    page?: number,
    limit?: number,
    sort: 'default' | 'newest' = 'default',
    userId?: number,
  ) {
    await this.findPost(postId);
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const order = sort === 'newest' ? 'DESC' : 'ASC';

    const [topLevel, total] = await this.commentRepository.findAndCount({
      where: { postId, parentId: IsNull(), isVisible: true },
      relations: ['author'],
      order: { createdAt: order as 'ASC' | 'DESC' },
      skip,
      take: normalizedLimit,
    });

    if (topLevel.length === 0) {
      return createPaginatedResult([], total, normalizedPage, normalizedLimit);
    }

    const rootIds = topLevel.map((comment) => comment.id);
    const [replyCounts, previewReplies] = await Promise.all([
      this.countRepliesByRoot(postId, rootIds),
      this.loadPreviewReplies(postId, rootIds, userId),
    ]);

    const enrichedTop = await this.enrichComments(topLevel, userId);
    const items = enrichedTop.map((comment) => ({
      ...comment,
      replyCount: replyCounts.get(comment.id) ?? 0,
      previewReplies: previewReplies.get(comment.id) ?? [],
    }));

    return createPaginatedResult(
      items,
      total,
      normalizedPage,
      normalizedLimit,
    );
  }

  async findCommentReplies(
    postId: number,
    rootCommentId: number,
    page?: number,
    limit?: number,
    userId?: number,
  ) {
    await this.findPost(postId);
    const root = await this.commentRepository.findOne({
      where: { id: rootCommentId, postId, parentId: IsNull() },
    });
    if (!root) {
      throw new NotFoundException('评论不存在');
    }

    const normalizedPage = Math.max(page ?? 1, 1);
    const normalizedLimit = Math.min(Math.max(limit ?? 20, 1), 50);
    const skip = (normalizedPage - 1) * normalizedLimit;

    const [replies, total] = await this.commentRepository.findAndCount({
      where: { postId, rootId: rootCommentId },
      relations: ['author'],
      order: { createdAt: 'ASC' },
      skip,
      take: normalizedLimit,
    });

    const items = await this.enrichComments(replies, userId);
    return createPaginatedResult(
      items,
      total,
      normalizedPage,
      normalizedLimit,
    );
  }

  private async countRepliesByRoot(postId: number, rootIds: number[]) {
    const counts = new Map<number, number>();
    if (rootIds.length === 0) {
      return counts;
    }

    const rows = await this.commentRepository
      .createQueryBuilder('comment')
      .select('comment.rootId', 'rootId')
      .addSelect('COUNT(*)', 'count')
      .where('comment.postId = :postId', { postId })
      .andWhere('comment.rootId IN (:...rootIds)', { rootIds })
      .groupBy('comment.rootId')
      .getRawMany<{ rootId: string; count: string }>();

    rows.forEach((row) => {
      counts.set(Number(row.rootId), Number(row.count));
    });
    return counts;
  }

  private async loadPreviewReplies(
    postId: number,
    rootIds: number[],
    userId?: number,
    previewSize = 2,
  ) {
    const previews = new Map<number, PostCommentEntity[]>();
    if (rootIds.length === 0) {
      return previews;
    }

    await Promise.all(
      rootIds.map(async (rootId) => {
        const replies = await this.commentRepository.find({
          where: { postId, rootId },
          relations: ['author'],
          order: { createdAt: 'ASC' },
          take: previewSize,
        });
        previews.set(rootId, replies);
      }),
    );

    const flatReplies = [...previews.values()].flat();
    const enriched = await this.enrichComments(flatReplies, userId);
    const enrichedMap = new Map(enriched.map((reply) => [reply.id, reply]));

    const result = new Map<number, typeof enriched>();
    previews.forEach((replies, rootId) => {
      result.set(
        rootId,
        replies.flatMap((reply) => {
          const item = enrichedMap.get(reply.id);
          return item ? [item] : [];
        }),
      );
    });
    return result;
  }

  async toggleCommentLike(userId: number, commentId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const comment = await this.commentRepository.findOne({
      where: { id: commentId },
    });
    if (!comment) {
      throw new NotFoundException('评论不存在');
    }

    const existing = await this.commentLikeRepository.findOne({
      where: { commentId, userId },
    });

    if (existing) {
      await this.commentLikeRepository.remove(existing);
      comment.likeCount = Math.max(0, comment.likeCount - 1);
    } else {
      await this.commentLikeRepository.save(
        this.commentLikeRepository.create({ commentId, userId }),
      );
      comment.likeCount += 1;
    }

    await this.commentRepository.save(comment);

    return {
      commentId: comment.id,
      likeCount: comment.likeCount,
      isLiked: !existing,
    };
  }

  async createComment(
    userId: number,
    postId: number,
    body: CreatePostCommentBodyDto,
  ) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const post = await this.findPost(postId);
    const content = body.content.trim();

    const parent = body.parentId
      ? await this.commentRepository.findOne({
          where: { id: body.parentId, postId },
        })
      : null;
    if (body.parentId && !parent) {
      throw new BadRequestException('回复的评论不存在');
    }
    const rootId = parent ? (parent.rootId ?? parent.id) : null;

    const comment = await this.commentRepository.save(
      this.commentRepository.create({
        postId,
        userId,
        content,
        parentId: body.parentId ?? null,
        rootId,
      }),
    );

    post.commentCount += 1;
    await this.postRepository.save(post);

    const saved = await this.commentRepository.findOne({
      where: { id: comment.id },
      relations: ['author'],
    });
    if (!saved) {
      throw new NotFoundException('评论不存在');
    }
    const [enriched] = await this.enrichComments([saved], userId);
    return enriched;
  }

  async removeComment(userId: number, commentId: number) {
    const comment = await this.commentRepository.findOne({
      where: { id: commentId },
    });
    if (!comment) {
      throw new NotFoundException('评论不存在');
    }
    if (comment.userId !== userId) {
      throw new ForbiddenException('无权删除该评论');
    }

    const post = await this.findPost(comment.postId);
    await this.commentRepository.remove(comment);
    post.commentCount = Math.max(0, post.commentCount - 1);
    await this.postRepository.save(post);

    return { message: '删除成功' };
  }

  async findMyComments(userId: number) {
    const comments = await this.commentRepository.find({
      where: { userId },
      relations: ['author'],
      order: { createdAt: 'DESC' },
    });
    return this.attachPostsToComments(comments, userId);
  }

  async findMyCommentsPaginated(userId: number, page?: number, limit?: number) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const [comments, total] = await this.commentRepository.findAndCount({
      where: { userId },
      relations: ['author'],
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    const items = await this.attachPostsToComments(comments, userId);
    return createPaginatedResult(items, total, normalizedPage, normalizedLimit);
  }

  async findLikedPosts(userId: number) {
    const likes = await this.likeRepository.find({
      where: { userId },
      order: { createdAt: 'DESC' },
    });
    return this.findPostsByIds(
      likes.map((item) => item.postId),
      userId,
    );
  }

  async findLikedPostsPaginated(userId: number, page?: number, limit?: number) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);

    const baseQb = this.likeRepository
      .createQueryBuilder('like')
      .innerJoin('like.post', 'post')
      .where('like.userId = :userId', { userId });

    const total = await baseQb.getCount();
    const likes = await baseQb
      .orderBy('like.createdAt', 'DESC')
      .skip(skip)
      .take(normalizedLimit)
      .getMany();

    const savedAtMap = new Map(
      likes.map((item) => [item.postId, item.createdAt]),
    );
    const items = await this.findPostsByIds(
      likes.map((item) => item.postId),
      userId,
      savedAtMap,
    );
    return createPaginatedResult(items, total, normalizedPage, normalizedLimit);
  }

  async findFavoritedPosts(userId: number) {
    const favorites = await this.favoriteRepository.find({
      where: { userId },
      order: { createdAt: 'DESC' },
    });
    return this.findPostsByIds(
      favorites.map((item) => item.postId),
      userId,
    );
  }

  async findFavoritedPostsPaginated(
    userId: number,
    page?: number,
    limit?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);

    const baseQb = this.favoriteRepository
      .createQueryBuilder('favorite')
      .innerJoin('favorite.post', 'post')
      .where('favorite.userId = :userId', { userId });

    const total = await baseQb.getCount();
    const favorites = await baseQb
      .orderBy('favorite.createdAt', 'DESC')
      .skip(skip)
      .take(normalizedLimit)
      .getMany();

    const savedAtMap = new Map(
      favorites.map((item) => [item.postId, item.createdAt]),
    );
    const items = await this.findPostsByIds(
      favorites.map((item) => item.postId),
      userId,
      savedAtMap,
    );
    return createPaginatedResult(items, total, normalizedPage, normalizedLimit);
  }

  private async enrichComments(
    comments: PostCommentEntity[],
    userId?: number,
  ) {
    if (comments.length === 0) {
      return [];
    }

    if (!userId) {
      return comments.map((comment) => ({
        ...comment,
        isLiked: false,
      }));
    }

    const commentIds = comments.map((comment) => comment.id);
    const likes = await this.commentLikeRepository.find({
      where: { userId, commentId: In(commentIds) },
      select: ['commentId'],
    });
    const likedIds = new Set(likes.map((item) => item.commentId));

    return comments.map((comment) => ({
      ...comment,
      isLiked: likedIds.has(comment.id),
    }));
  }

  private async attachPostsToComments(
    comments: PostCommentEntity[],
    userId: number,
  ) {
    if (comments.length === 0) {
      return [];
    }

    const enrichedComments = await this.enrichComments(comments, userId);

    const postIds = [...new Set(enrichedComments.map((item) => item.postId))];
    const posts = await this.postRepository.find({
      where: { id: In(postIds) },
      relations: ['author'],
    });
    const enrichedPosts = await this.enrichPosts(posts, userId);
    const enrichedMap = new Map(enrichedPosts.map((post) => [post.id, post]));

    return enrichedComments.map((comment) => ({
      ...comment,
      post: enrichedMap.get(comment.postId) ?? null,
    }));
  }

  private async findPostsByIds(
    postIds: number[],
    userId: number,
    savedAtMap?: Map<number, Date>,
  ) {
    if (postIds.length === 0) {
      return [];
    }
    const posts = await this.postRepository.find({
      where: { id: In(postIds) },
      relations: ['author'],
    });
    const postMap = new Map(posts.map((post) => [post.id, post]));
    const ordered = postIds
      .map((id) => postMap.get(id))
      .filter((post): post is PostEntity => Boolean(post));
    const enriched = await this.enrichPosts(ordered, userId);
    if (!savedAtMap) {
      return enriched;
    }
    return enriched.map((post) => ({
      ...post,
      savedAt: savedAtMap.get(post.id)?.toISOString(),
    }));
  }

  private async buildSocialState(post: PostEntity, userId: number) {
    const [isLiked, isFavorited] = await Promise.all([
      this.likeRepository.exist({ where: { postId: post.id, userId } }),
      this.favoriteRepository.exist({ where: { postId: post.id, userId } }),
    ]);

    return {
      likeCount: post.likeCount,
      favoriteCount: post.favoriteCount,
      commentCount: post.commentCount,
      isLiked,
      isFavorited,
    };
  }

  private async findPost(postId: number) {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post) {
      throw new NotFoundException('信息不存在');
    }
    return post;
  }
}
