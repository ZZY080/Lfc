import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { MessageEntity } from '@module/message/entity/message.entity';
import { SendMessageDto } from '@module/message/dto/message.dto';
import { ActivityStatus } from '@shared/enum/user-role.enum';
import {
  MessageRelatedType,
  MessageType,
} from '@shared/enum/message-type.enum';

@Injectable()
export class MessageService {
  constructor(
    @InjectRepository(MessageEntity)
    private readonly messageRepository: Repository<MessageEntity>,
  ) {}

  async send(payload: SendMessageDto) {
    return this.messageRepository.save(
      this.messageRepository.create({
        userId: payload.userId,
        title: payload.title,
        content: payload.content,
        type: payload.type,
        relatedType: payload.relatedType ?? null,
        relatedId: payload.relatedId ?? null,
        isRead: false,
      }),
    );
  }

  async sendWelcome(userId: number) {
    return this.send({
      userId,
      title: '欢迎加入莲峰校园',
      content:
        '你好！欢迎注册莲峰校园平台。你可以在这里发布校园笔记、组织活动，并及时收到审核与报名通知。',
      type: MessageType.SYSTEM,
      relatedType: MessageRelatedType.SYSTEM,
    });
  }

  async sendActivitySubmitted(userId: number, activity: ActivityEntity) {
    return this.send({
      userId,
      title: '活动已提交审核',
      content: `你发布的活动「${activity.title}」已提交，管理员审核通过后将展示在活动列表。`,
      type: MessageType.ACTIVITY_SUBMITTED,
      relatedType: MessageRelatedType.ACTIVITY,
      relatedId: activity.id,
    });
  }

  async sendActivityReview(
    userId: number,
    activity: ActivityEntity,
    status: ActivityStatus,
  ) {
    if (status === ActivityStatus.APPROVED) {
      return this.send({
        userId,
        title: '活动审核通过',
        content: `恭喜！你发布的活动「${activity.title}」已通过审核，同学们现在可以报名参加了。`,
        type: MessageType.ACTIVITY_APPROVED,
        relatedType: MessageRelatedType.ACTIVITY,
        relatedId: activity.id,
      });
    }

    return this.send({
      userId,
      title: '活动审核未通过',
      content: `你发布的活动「${activity.title}」未通过审核，请修改后重新提交。`,
      type: MessageType.ACTIVITY_REJECTED,
      relatedType: MessageRelatedType.ACTIVITY,
      relatedId: activity.id,
    });
  }

  async sendActivityJoin(
    authorId: number,
    activity: ActivityEntity,
    joinerStudentId: string,
  ) {
    return this.send({
      userId: authorId,
      title: '有人报名了你的活动',
      content: `学号 ${joinerStudentId} 报名了你的活动「${activity.title}」，快去看看吧。`,
      type: MessageType.ACTIVITY_JOIN,
      relatedType: MessageRelatedType.ACTIVITY,
      relatedId: activity.id,
    });
  }
}
