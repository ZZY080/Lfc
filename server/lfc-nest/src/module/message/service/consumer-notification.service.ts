import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { NotificationEntity } from '@module/message/entity/notification.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class ConsumerNotificationService {
  constructor(
    @InjectRepository(NotificationEntity)
    private readonly notificationRepository: Repository<NotificationEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async findPaginated(userId: number, page?: number, limit?: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);
    const [items, total] = await this.notificationRepository.findAndCount({
      where: { userId },
      order: { createdAt: 'DESC' },
      skip,
      take: normalizedLimit,
    });
    return createPaginatedResult(
      items,
      total,
      normalizedPage,
      normalizedLimit,
    );
  }

  async getUnreadCount(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const count = await this.notificationRepository.count({
      where: { userId, isRead: false },
    });
    return { count };
  }

  async findOne(userId: number, id: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const notification = await this.notificationRepository.findOne({ where: { id } });
    if (!notification) {
      throw new NotFoundException('通知不存在');
    }
    if (notification.userId !== userId) {
      throw new ForbiddenException('无权查看该通知');
    }
    if (!notification.isRead) {
      notification.isRead = true;
      await this.notificationRepository.save(notification);
    }
    return notification;
  }

  async markAllRead(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    await this.notificationRepository.update({ userId, isRead: false }, { isRead: true });
    return { message: '已全部标记为已读' };
  }

  async remove(userId: number, id: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const notification = await this.notificationRepository.findOne({ where: { id } });
    if (!notification) {
      throw new NotFoundException('通知不存在');
    }
    if (notification.userId !== userId) {
      throw new ForbiddenException('无权删除该通知');
    }
    await this.notificationRepository.remove(notification);
    return { message: '删除成功' };
  }
}
