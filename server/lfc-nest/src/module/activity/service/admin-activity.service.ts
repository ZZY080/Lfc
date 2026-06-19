import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { ActivityStatus, UserRole } from '@shared/enum/user-role.enum';
import { ReviewActivityBodyDto } from '@module/activity/dto/activity.dto';
import { AdminUpdateActivityBodyDto } from '@module/activity/dto/admin-activity.dto';
import { AdminActivityListQueryDto } from '@module/admin/dto/admin-query.dto';
import { NotificationService } from '@module/message/service/notification.service';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class AdminActivityService {
  constructor(
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly notificationService: NotificationService,
  ) {}

  async findPending(userId: number, query: AdminActivityListQueryDto) {
    return this.findAll(userId, {
      ...query,
      status: ActivityStatus.PENDING,
    });
  }

  async findAll(userId: number, query: AdminActivityListQueryDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);
    const qb = this.activityRepository
      .createQueryBuilder('activity')
      .leftJoinAndSelect('activity.author', 'author')
      .leftJoinAndSelect('activity.participants', 'participants');

    if (query.status) {
      qb.andWhere('activity.status = :status', { status: query.status });
    }

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere(
        '(activity.title LIKE :keyword OR activity.description LIKE :keyword OR activity.location LIKE :keyword)',
        { keyword },
      );
    }

    qb.orderBy('activity.createdAt', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();

    return createPaginatedResult(items, total, page, limit);
  }

  async findOne(userId: number, id: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    const activity = await this.activityRepository.findOne({
      where: { id },
      relations: ['author', 'participants'],
    });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }
    return activity;
  }

  async update(userId: number, id: number, body: AdminUpdateActivityBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    const activity = await this.getActivityOrThrow(id);
    const previousStatus = activity.status;

    if (body.title !== undefined) activity.title = body.title;
    if (body.description !== undefined) activity.description = body.description;
    if (body.location !== undefined) activity.location = body.location;
    if (body.maxParticipants !== undefined) {
      activity.maxParticipants = body.maxParticipants;
    }
    if (body.fee !== undefined) activity.fee = String(body.fee);
    if (body.status !== undefined) {
      if (body.status === ActivityStatus.REJECTED) {
        const comment = body.reviewComment?.trim();
        if (!comment) {
          throw new BadRequestException('拒绝时需填写审核意见');
        }
        activity.reviewComment = comment;
      } else if (body.status === ActivityStatus.APPROVED) {
        activity.reviewComment = null;
      }
      activity.status = body.status;
    }

    const saved = await this.activityRepository.save(activity);

    if (
      body.status !== undefined &&
      body.status !== previousStatus &&
      (body.status === ActivityStatus.APPROVED ||
        body.status === ActivityStatus.REJECTED)
    ) {
      await this.notificationService.sendActivityReview(
        activity.authorId,
        saved,
        body.status,
      );
    }

    return saved;
  }

  async review(userId: number, id: number, body: ReviewActivityBodyDto) {
    return this.update(userId, id, {
      status: body.status,
      reviewComment: body.reviewComment,
    });
  }

  async remove(userId: number, id: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    await this.getActivityOrThrow(id);
    await this.activityRepository.delete(id);
    return { message: '活动已删除' };
  }

  private async getActivityOrThrow(id: number) {
    const activity = await this.activityRepository.findOne({ where: { id } });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }
    return activity;
  }
}
