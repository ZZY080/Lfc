import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserFollowEntity } from '@module/user/entity/user-follow.entity';
import { ConsumerPostService } from '@module/post/service/consumer-post.service';
import { ConsumerPostSocialService } from '@module/post/service/consumer-post-social.service';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { UserProfileDetailDto } from '@module/user/dto/user-profile.dto';
import { UpdateUserProfileBodyDto } from '@module/user/dto/update-user.dto';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import {
  generateDefaultNickname,
  normalizeNickname,
} from '@module/user/util/user-nickname.util';
import {
  generateUniqueLfcNo,
  normalizeLfcNo,
} from '@module/user/util/user-lfc-no.util';

@Injectable()
export class ConsumerUserService {
  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(UserFollowEntity)
    private readonly followRepository: Repository<UserFollowEntity>,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(ActivityParticipantEntity)
    private readonly participantRepository: Repository<ActivityParticipantEntity>,
    private readonly consumerPostService: ConsumerPostService,
    private readonly consumerPostSocialService: ConsumerPostSocialService,
    private readonly consumerActivityService: ConsumerActivityService,
  ) {}

  async getMe(userId: number) {
    return this.buildProfileDetail(userId, userId);
  }

  async updateMe(userId: number, body: UpdateUserProfileBodyDto) {
    const user = await this.findUser(userId);
    if (body.nickname !== undefined) {
      user.nickname = normalizeNickname(body.nickname);
    }
    if (body.bio !== undefined) {
      user.bio = body.bio.trim() || null;
    }
    if (body.avatarUrl !== undefined) {
      user.avatarUrl = body.avatarUrl.trim() || null;
    }
    if (body.coverUrl !== undefined) {
      user.coverUrl = body.coverUrl.trim() || null;
    }
    if (body.showCommentsPublic !== undefined) {
      user.showCommentsPublic = body.showCommentsPublic;
    }
    if (body.showFavoritesPublic !== undefined) {
      user.showFavoritesPublic = body.showFavoritesPublic;
    }
    if (body.showLikesPublic !== undefined) {
      user.showLikesPublic = body.showLikesPublic;
    }
    await this.userRepository.save(user);
    return this.buildProfileDetail(userId, userId);
  }

  async getProfile(userId: number, viewerId?: number): Promise<UserProfileDetailDto> {
    await this.findUser(userId);
    return this.buildProfileDetail(userId, viewerId);
  }

  async getProfileByLfcNo(
    lfcNo: string,
    viewerId?: number,
  ): Promise<UserProfileDetailDto> {
    const normalized = normalizeLfcNo(lfcNo);
    if (!normalized) {
      throw new NotFoundException('用户不存在');
    }

    const user = await this.userRepository.findOne({ where: { lfcNo: normalized } });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }

    return this.buildProfileDetail(user.id, viewerId);
  }

  findMyFavorites(userId: number, page?: number, limit?: number) {
    return this.consumerPostSocialService.findFavoritedPostsPaginated(
      userId,
      page,
      limit,
    );
  }

  findMyLikes(userId: number, page?: number, limit?: number) {
    return this.consumerPostSocialService.findLikedPostsPaginated(
      userId,
      page,
      limit,
    );
  }

  findMyComments(userId: number, page?: number, limit?: number) {
    return this.consumerPostSocialService.findMyCommentsPaginated(
      userId,
      page,
      limit,
    );
  }

  findUserPosts(
    userId: number,
    viewerId: number | undefined,
    page?: number,
    limit?: number,
  ) {
    return this.consumerPostService.findByAuthorPaginated(
      userId,
      page,
      limit,
      viewerId,
    );
  }

  findUserActivities(
    userId: number,
    viewerId: number | undefined,
    page?: number,
    limit?: number,
  ) {
    const isSelf = viewerId === userId;
    return this.consumerActivityService.findByAuthorPaginated(
      userId,
      isSelf,
      page,
      limit,
    );
  }

  async findUserFavorites(
    userId: number,
    viewerId: number | undefined,
    page?: number,
    limit?: number,
  ) {
    await this.assertLibraryAccess(userId, viewerId, 'favorites');
    return this.consumerPostSocialService.findFavoritedPostsPaginated(
      userId,
      page,
      limit,
    );
  }

  async findUserLikes(
    userId: number,
    viewerId: number | undefined,
    page?: number,
    limit?: number,
  ) {
    await this.assertLibraryAccess(userId, viewerId, 'likes');
    return this.consumerPostSocialService.findLikedPostsPaginated(
      userId,
      page,
      limit,
    );
  }

  async findUserComments(
    userId: number,
    viewerId: number | undefined,
    page?: number,
    limit?: number,
  ) {
    await this.assertLibraryAccess(userId, viewerId, 'comments');
    return this.consumerPostSocialService.findMyCommentsPaginated(
      userId,
      page,
      limit,
    );
  }

  async toggleFollow(followerId: number, followingId: number) {
    if (followerId === followingId) {
      throw new BadRequestException('不能关注自己');
    }
    await this.findUser(followingId);

    const existing = await this.followRepository.findOne({
      where: { followerId, followingId },
    });

    if (existing) {
      await this.followRepository.remove(existing);
      return { isFollowing: false };
    }

    await this.followRepository.save(
      this.followRepository.create({ followerId, followingId }),
    );
    return { isFollowing: true };
  }

  private async buildProfileDetail(
    userId: number,
    viewerId?: number,
  ): Promise<UserProfileDetailDto> {
    const user = await this.ensureLfcNo(
      await this.ensureNickname(await this.findUser(userId)),
    );
    const isSelf = viewerId === userId;

    const [
      posts,
      activities,
      participationCount,
      followingCount,
      followerCount,
      likeAndFavoriteCount,
      isFollowing,
    ] = await Promise.all([
      this.consumerPostService.findByAuthor(userId, viewerId),
      isSelf
        ? this.consumerActivityService.findMine(userId)
        : this.consumerActivityService.findApprovedByAuthor(userId),
      this.participantRepository.count({ where: { userId } }),
      this.followRepository.count({ where: { followerId: userId } }),
      this.followRepository.count({ where: { followingId: userId } }),
      this.countLikeAndFavorite(userId),
      viewerId && viewerId !== userId
        ? this.followRepository.exist({
            where: { followerId: viewerId, followingId: userId },
          })
        : Promise.resolve(false),
    ]);

    return {
      id: user.id,
      lfcNo: user.lfcNo!,
      studentId: user.studentId,
      nickname: user.nickname,
      bio: user.bio ?? '莲峰校园 · 记录校园生活',
      avatarUrl: user.avatarUrl,
      coverUrl: user.coverUrl,
      postCount: posts.length,
      followingCount,
      followerCount,
      likeAndFavoriteCount,
      isFollowing,
      isSelf,
      showCommentsPublic: user.showCommentsPublic,
      showFavoritesPublic: user.showFavoritesPublic,
      showLikesPublic: user.showLikesPublic,
      posts,
      activities,
      participationCount,
    };
  }

  private async countLikeAndFavorite(userId: number) {
    const raw = await this.postRepository
      .createQueryBuilder('post')
      .select('COALESCE(SUM(post.likeCount), 0)', 'likes')
      .addSelect('COALESCE(SUM(post.favoriteCount), 0)', 'favorites')
      .where('post.authorId = :userId', { userId })
      .getRawOne<{ likes: string; favorites: string }>();

    return Number(raw?.likes ?? 0) + Number(raw?.favorites ?? 0);
  }

  private async ensureLfcNo(user: UserEntity): Promise<UserEntity> {
    if (normalizeLfcNo(user.lfcNo)) {
      return user;
    }

    try {
      user.lfcNo = await generateUniqueLfcNo((candidate) =>
        this.userRepository.exist({ where: { lfcNo: candidate } }),
      );
      return this.userRepository.save(user);
    } catch {
      throw new BadRequestException('生成莲峰号失败，请稍后重试');
    }
  }

  private async ensureNickname(user: UserEntity): Promise<UserEntity> {
    if (normalizeNickname(user.nickname)) {
      return user;
    }

    for (let attempt = 0; attempt < 5; attempt += 1) {
      const nickname = generateDefaultNickname();
      const exists = await this.userRepository.exist({ where: { nickname } });
      if (!exists) {
        user.nickname = nickname;
        return this.userRepository.save(user);
      }
    }

    user.nickname = `${generateDefaultNickname()}${user.id}`;
    return this.userRepository.save(user);
  }

  private async assertLibraryAccess(
    userId: number,
    viewerId: number | undefined,
    type: 'comments' | 'favorites' | 'likes',
  ) {
    if (viewerId === userId) {
      return;
    }

    const user = await this.findUser(userId);
    const allowed =
      type === 'comments'
        ? user.showCommentsPublic
        : type === 'favorites'
          ? user.showFavoritesPublic
          : user.showLikesPublic;

    if (!allowed) {
      throw new ForbiddenException('该内容未公开');
    }
  }

  private async findUser(userId: number) {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) {
      throw new NotFoundException('用户不存在');
    }
    return user;
  }
}
