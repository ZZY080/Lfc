import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { ActivityLikeEntity } from '@module/activity/entity/activity-like.entity';
import { ActivityFavoriteEntity } from '@module/activity/entity/activity-favorite.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityStatus } from '@shared/enum/user-role.enum';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class ConsumerActivitySocialService {
  constructor(
    @InjectRepository(ActivityLikeEntity)
    private readonly likeRepository: Repository<ActivityLikeEntity>,
    @InjectRepository(ActivityFavoriteEntity)
    private readonly favoriteRepository: Repository<ActivityFavoriteEntity>,
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
  ) {}

  async enrichActivities<T extends ActivityEntity & { isJoined?: boolean }>(
    activities: T[],
    viewerId?: number,
  ) {
    if (activities.length === 0) {
      return [] as Array<
        T & {
          likeCount: number;
          favoriteCount: number;
          isLiked: boolean;
          isFavorited: boolean;
        }
      >;
    }

    const ids = activities.map((item) => item.id);
    const [likeCounts, favoriteCounts, userLikes, userFavorites] =
      await Promise.all([
        this.countByActivityIds(this.likeRepository, ids),
        this.countByActivityIds(this.favoriteRepository, ids),
        viewerId
          ? this.likeRepository.find({
              where: { userId: viewerId, activityId: In(ids) },
            })
          : Promise.resolve([]),
        viewerId
          ? this.favoriteRepository.find({
              where: { userId: viewerId, activityId: In(ids) },
            })
          : Promise.resolve([]),
      ]);

    const likedIds = new Set(userLikes.map((item) => item.activityId));
    const favoritedIds = new Set(userFavorites.map((item) => item.activityId));

    return activities.map((activity) => ({
      ...activity,
      likeCount: likeCounts.get(activity.id) ?? 0,
      favoriteCount: favoriteCounts.get(activity.id) ?? 0,
      isLiked: likedIds.has(activity.id),
      isFavorited: favoritedIds.has(activity.id),
    }));
  }

  private async countByActivityIds(
    repository: Repository<ActivityLikeEntity | ActivityFavoriteEntity>,
    activityIds: number[],
  ) {
    if (activityIds.length === 0) {
      return new Map<number, number>();
    }

    const rows = await repository
      .createQueryBuilder('item')
      .select('item.activityId', 'activityId')
      .addSelect('COUNT(*)', 'count')
      .where('item.activityId IN (:...activityIds)', { activityIds })
      .groupBy('item.activityId')
      .getRawMany<{ activityId: string; count: string }>();

    return new Map(
      rows.map((row) => [Number(row.activityId), Number(row.count)]),
    );
  }

  async toggleLike(userId: number, activityId: number) {
    const activity = await this.activityRepository.findOne({
      where: { id: activityId },
    });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }

    const existing = await this.likeRepository.findOne({
      where: { activityId, userId },
    });
    if (existing) {
      await this.likeRepository.remove(existing);
    } else {
      await this.likeRepository.save(
        this.likeRepository.create({ activityId, userId }),
      );
    }

    const [isLiked, isFavorited, likeCount, favoriteCount] = await Promise.all([
      this.likeRepository.exist({ where: { activityId, userId } }),
      this.favoriteRepository.exist({ where: { activityId, userId } }),
      this.likeRepository.count({ where: { activityId } }),
      this.favoriteRepository.count({ where: { activityId } }),
    ]);

    return { isLiked, isFavorited, likeCount, favoriteCount };
  }

  async toggleFavorite(userId: number, activityId: number) {
    const activity = await this.activityRepository.findOne({
      where: { id: activityId },
    });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }

    const existing = await this.favoriteRepository.findOne({
      where: { activityId, userId },
    });
    if (existing) {
      await this.favoriteRepository.remove(existing);
    } else {
      await this.favoriteRepository.save(
        this.favoriteRepository.create({ activityId, userId }),
      );
    }

    const [isLiked, isFavorited, likeCount, favoriteCount] = await Promise.all([
      this.likeRepository.exist({ where: { activityId, userId } }),
      this.favoriteRepository.exist({ where: { activityId, userId } }),
      this.likeRepository.count({ where: { activityId } }),
      this.favoriteRepository.count({ where: { activityId } }),
    ]);

    return { isLiked, isFavorited, likeCount, favoriteCount };
  }

  async findLikedActivitiesPaginated(
    userId: number,
    page?: number,
    limit?: number,
    viewerId?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const [likes, total] = await this.likeRepository.findAndCount({
      where: { userId },
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    const items = await this.findActivitiesByIds(
      likes.map((item) => item.activityId),
      viewerId,
    );
    return createPaginatedResult(items, total, normalizedPage, normalizedLimit);
  }

  async findFavoritedActivitiesPaginated(
    userId: number,
    page?: number,
    limit?: number,
    viewerId?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const [favorites, total] = await this.favoriteRepository.findAndCount({
      where: { userId },
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    const items = await this.findActivitiesByIds(
      favorites.map((item) => item.activityId),
      viewerId,
    );
    return createPaginatedResult(items, total, normalizedPage, normalizedLimit);
  }

  private async findActivitiesByIds(activityIds: number[], viewerId?: number) {
    if (activityIds.length === 0) {
      return [];
    }

    const activities = await this.activityRepository.find({
      where: { id: In(activityIds), status: ActivityStatus.APPROVED },
      relations: ['author', 'participants', 'participants.user'],
    });
    const activityMap = new Map(activities.map((item) => [item.id, item]));

    const ordered = activityIds
      .map((id) => activityMap.get(id))
      .filter((item): item is ActivityEntity => item != null)
      .map((activity) => ({
        ...activity,
        isJoined: viewerId
          ? activity.participants.some((item) => item.userId === viewerId)
          : false,
      }));

    return this.enrichActivities(ordered, viewerId);
  }
}
