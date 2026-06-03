import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { ActivityStatus, UserRole } from '@shared/enum/user-role.enum';
import { ReviewActivityBodyDto } from '@module/activity/dto/activity.dto';
import { NotificationService } from '@module/message/service/notification.service';

@Injectable()
export class AdminActivityService {
  constructor(
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly notificationService: NotificationService,
  ) {}

  async findPending(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    return this.activityRepository.find({
      where: { status: ActivityStatus.PENDING },
      relations: ['author'],
      order: { createdAt: 'ASC' },
    });
  }

  async findAll(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    return this.activityRepository.find({
      relations: ['author', 'participants'],
      order: { createdAt: 'DESC' },
    });
  }

  async review(userId: number, id: number, body: ReviewActivityBodyDto) {
    await this.roleAuthzService.assertRole(userId, UserRole.ADMIN);
    const activity = await this.activityRepository.findOne({
      where: { id },
      relations: ['author'],
    });
    if (!activity) {
      throw new NotFoundException('活动不存在');
    }
    activity.status = body.status;
    const saved = await this.activityRepository.save(activity);
    await this.notificationService.sendActivityReview(
      activity.authorId,
      saved,
      body.status,
    );
    return saved;
  }
}
