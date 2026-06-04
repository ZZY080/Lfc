import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { PostLikeEntity } from '@module/post/entity/post-like.entity';
import { PostFavoriteEntity } from '@module/post/entity/post-favorite.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
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

  async findComments(postId: number) {
    await this.findPost(postId);
    return this.commentRepository.find({
      where: { postId },
      relations: ['author'],
      order: { createdAt: 'ASC' },
    });
  }

  async createComment(
    userId: number,
    postId: number,
    body: CreatePostCommentBodyDto,
  ) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const post = await this.findPost(postId);
    const content = body.content.trim();

    if (body.parentId) {
      const parent = await this.commentRepository.findOne({
        where: { id: body.parentId, postId },
      });
      if (!parent) {
        throw new BadRequestException('回复的评论不存在');
      }
    }

    const comment = await this.commentRepository.save(
      this.commentRepository.create({
        postId,
        userId,
        content,
        parentId: body.parentId ?? null,
      }),
    );

    post.commentCount += 1;
    await this.postRepository.save(post);

    return this.commentRepository.findOne({
      where: { id: comment.id },
      relations: ['author'],
    });
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

  private async attachPostsToComments(
    comments: PostCommentEntity[],
    userId: number,
  ) {
    if (comments.length === 0) {
      return [];
    }

    const postIds = [...new Set(comments.map((item) => item.postId))];
    const posts = await this.postRepository.find({
      where: { id: In(postIds) },
      relations: ['author'],
    });
    const enrichedPosts = await this.enrichPosts(posts, userId);
    const enrichedMap = new Map(enrichedPosts.map((post) => [post.id, post]));

    return comments.map((comment) => ({
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
