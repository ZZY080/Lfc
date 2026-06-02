import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MessageEntity } from '@module/message/entity/message.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';

@Injectable()
export class ConsumerMessageService {
  constructor(
    @InjectRepository(MessageEntity)
    private readonly messageRepository: Repository<MessageEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async findAll(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    return this.messageRepository.find({
      where: { userId },
      order: { createdAt: 'DESC' },
    });
  }

  async getUnreadCount(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const count = await this.messageRepository.count({
      where: { userId, isRead: false },
    });
    return { count };
  }

  async findOne(userId: number, id: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const message = await this.messageRepository.findOne({ where: { id } });
    if (!message) {
      throw new NotFoundException('消息不存在');
    }
    if (message.userId !== userId) {
      throw new ForbiddenException('无权查看该消息');
    }
    if (!message.isRead) {
      message.isRead = true;
      await this.messageRepository.save(message);
    }
    return message;
  }

  async markRead(userId: number, id: number) {
    const message = await this.findOne(userId, id);
    return message;
  }

  async markAllRead(userId: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    await this.messageRepository.update({ userId, isRead: false }, { isRead: true });
    return { message: '已全部标记为已读' };
  }

  async remove(userId: number, id: number) {
    await this.roleAuthzService.assertRole(userId, UserRole.CONSUMER);
    const message = await this.messageRepository.findOne({ where: { id } });
    if (!message) {
      throw new NotFoundException('消息不存在');
    }
    if (message.userId !== userId) {
      throw new ForbiddenException('无权删除该消息');
    }
    await this.messageRepository.remove(message);
    return { message: '删除成功' };
  }
}
