import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { NotificationEntity } from '@module/message/entity/notification.entity';
import { SendNotificationDto } from '@module/message/dto/notification.dto';
import { ActivityStatus } from '@shared/enum/user-role.enum';
import { PostStatus } from '@shared/enum/post-status.enum';
import {
  MessageRelatedType,
  MessageType,
} from '@shared/enum/message-type.enum';

@Injectable()
export class NotificationService {
  constructor(
    @InjectRepository(NotificationEntity)
    private readonly notificationRepository: Repository<NotificationEntity>,
  ) {}

  async send(payload: SendNotificationDto) {
    return this.notificationRepository.save(
      this.notificationRepository.create({
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
      content: `你发布的活动「${activity.title}」未通过审核。审核意见：${activity.reviewComment || '请修改后重新提交'}`,
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

  async sendPostSubmitted(userId: number, post: PostEntity) {
    return this.send({
      userId,
      title: '帖子已提交审核',
      content: `你发布的笔记「${post.title}」已提交，管理员审核通过后将展示在公域。`,
      type: MessageType.POST_SUBMITTED,
      relatedType: MessageRelatedType.POST,
      relatedId: post.id,
    });
  }

  async sendPostReview(
    userId: number,
    post: PostEntity,
    status: PostStatus,
  ) {
    if (status === PostStatus.APPROVED) {
      return this.send({
        userId,
        title: '帖子审核通过',
        content: `恭喜！你发布的笔记「${post.title}」已通过审核，同学们现在可以看到啦。`,
        type: MessageType.POST_APPROVED,
        relatedType: MessageRelatedType.POST,
        relatedId: post.id,
      });
    }

    return this.send({
      userId,
      title: '帖子审核未通过',
      content: `你发布的笔记「${post.title}」未通过审核。审核意见：${post.reviewComment || '请修改后重新提交'}`,
      type: MessageType.POST_REJECTED,
      relatedType: MessageRelatedType.POST,
      relatedId: post.id,
    });
  }

  async sendAfterSalesSubmitted(
    userId: number,
    orderSubject: string,
    outTradeNo: string,
    orderId: number,
  ) {
    return this.send({
      userId,
      title: '退款申请已提交',
      content: `订单「${orderSubject}」的退款申请已提交，请等待管理员审核。审核通过后将原路退款。`,
      type: MessageType.AFTER_SALES_SUBMITTED,
      relatedType: MessageRelatedType.ORDER,
      relatedId: orderId,
    });
  }

  async sendAfterSalesApproved(
    userId: number,
    orderSubject: string,
    orderId: number,
  ) {
    return this.send({
      userId,
      title: '退款审核通过',
      content: `订单「${orderSubject}」的退款申请已通过，款项将原路退回至你的支付宝账户，通常 1–7 个工作日到账。`,
      type: MessageType.AFTER_SALES_APPROVED,
      relatedType: MessageRelatedType.ORDER,
      relatedId: orderId,
    });
  }

  async sendAfterSalesRejected(
    userId: number,
    orderSubject: string,
    orderId: number,
    rejectReason: string,
  ) {
    return this.send({
      userId,
      title: '退款申请未通过',
      content: `订单「${orderSubject}」的退款申请未通过。审核意见：${rejectReason}`,
      type: MessageType.AFTER_SALES_REJECTED,
      relatedType: MessageRelatedType.ORDER,
      relatedId: orderId,
    });
  }

  async sendAfterSalesPendingToAdmin(
    adminUserId: number,
    orderSubject: string,
    outTradeNo: string,
    orderId: number,
  ) {
    return this.send({
      userId: adminUserId,
      title: '待审核退款申请',
      content: `用户提交了订单「${orderSubject}」（${outTradeNo}）的退款申请，请尽快在管理后台审核。`,
      type: MessageType.AFTER_SALES_PENDING_ADMIN,
      relatedType: MessageRelatedType.ORDER,
      relatedId: orderId,
    });
  }
}
