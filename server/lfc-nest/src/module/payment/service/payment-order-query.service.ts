import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository, SelectQueryBuilder } from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentOrderReviewEntity } from '@module/payment/entity/payment-order-review.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import {
  PaymentAfterSalesStatus,
  PaymentBizType,
  PaymentOrderStatus,
  PaymentOrderTab,
} from '@shared/enum/payment.enum';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';
import {
  PaymentOrderListItemDto,
  PaymentOrderTabCountsDto,
} from '@module/payment/dto/payment.dto';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { UserEntity } from '@module/user/entity/user.entity';

@Injectable()
export class PaymentOrderQueryService {
  constructor(
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    @InjectRepository(PaymentOrderReviewEntity)
    private readonly reviewRepository: Repository<PaymentOrderReviewEntity>,
    @InjectRepository(PaymentAfterSalesEntity)
    private readonly afterSalesRepository: Repository<PaymentAfterSalesEntity>,
    @InjectRepository(PostEntity)
    private readonly postRepository: Repository<PostEntity>,
    @InjectRepository(ActivityEntity)
    private readonly activityRepository: Repository<ActivityEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {}

  async listForBuyer(
    userId: number,
    tab: PaymentOrderTab,
    page?: number,
    limit?: number,
  ) {
    const { page: normalizedPage, limit: normalizedLimit, skip } =
      normalizePagination(page, limit);

    const qb = this.paymentOrderRepository
      .createQueryBuilder('paymentOrder')
      .where('paymentOrder.userId = :userId', { userId })
      .orderBy('paymentOrder.createdAt', 'DESC');

    this.applyTabFilter(qb, tab, userId);

    const total = await qb.getCount();
    const orders = await qb.skip(skip).take(normalizedLimit).getMany();

    const items = await this.enrichOrders(orders, userId);
    return createPaginatedResult(
      items,
      total,
      normalizedPage,
      normalizedLimit,
    );
  }

  async getTabCounts(userId: number): Promise<PaymentOrderTabCountsDto> {
    const [all, pendingPayment, awaitingReceipt, review, afterSales] =
      await Promise.all([
        this.countByTab(userId, PaymentOrderTab.ALL),
        this.countByTab(userId, PaymentOrderTab.PENDING_PAYMENT),
        this.countByTab(userId, PaymentOrderTab.AWAITING_RECEIPT),
        this.countByTab(userId, PaymentOrderTab.REVIEW),
        this.countByTab(userId, PaymentOrderTab.AFTER_SALES),
      ]);

    return {
      all,
      pendingPayment,
      awaitingReceipt,
      review,
      afterSales,
    };
  }

  private async countByTab(
    userId: number,
    tab: PaymentOrderTab,
  ): Promise<number> {
    const qb = this.paymentOrderRepository
      .createQueryBuilder('paymentOrder')
      .where('paymentOrder.userId = :userId', { userId });
    this.applyTabFilter(qb, tab, userId);
    return qb.getCount();
  }

  private applyTabFilter(
    qb: SelectQueryBuilder<PaymentOrderEntity>,
    tab: PaymentOrderTab,
    userId: number,
  ) {
    switch (tab) {
      case PaymentOrderTab.PENDING_PAYMENT:
        qb.andWhere('paymentOrder.status = :status', {
          status: PaymentOrderStatus.PENDING,
        });
        break;
      case PaymentOrderTab.AWAITING_RECEIPT:
        qb.andWhere('paymentOrder.status = :status', {
          status: PaymentOrderStatus.PAID,
        }).andWhere('paymentOrder.bizType = :bizType', {
          bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
        });
        break;
      case PaymentOrderTab.REVIEW:
        qb.andWhere('paymentOrder.status = :status', {
          status: PaymentOrderStatus.SETTLED,
        })
          .leftJoin(
            PaymentOrderReviewEntity,
            'orderReview',
            'orderReview.paymentOrderId = paymentOrder.id',
          )
          .andWhere('orderReview.id IS NULL');
        break;
      case PaymentOrderTab.AFTER_SALES:
        qb.andWhere(
          `EXISTS (
            SELECT 1 FROM payment_after_sales afterSales
            WHERE afterSales.payment_order_id = paymentOrder.id
              AND afterSales.user_id = :userId
              AND afterSales.status IN (:...activeStatuses)
          )`,
          {
            userId,
            activeStatuses: [
              PaymentAfterSalesStatus.PENDING,
              PaymentAfterSalesStatus.REFUNDING,
              PaymentAfterSalesStatus.APPROVED,
            ],
          },
        );
        break;
      case PaymentOrderTab.ALL:
      default:
        break;
    }
  }

  private async enrichOrders(
    orders: PaymentOrderEntity[],
    viewerUserId: number,
  ): Promise<PaymentOrderListItemDto[]> {
    if (orders.length === 0) {
      return [];
    }

    const orderIds = orders.map((item) => item.id);
    const postIds = orders
      .filter((item) => item.bizType === PaymentBizType.POST_PRODUCT_PURCHASE)
      .map((item) => item.bizId);
    const activityIds = orders
      .filter((item) => item.bizType === PaymentBizType.ACTIVITY_JOIN)
      .map((item) => item.bizId);
    const payeeIds = [...new Set(orders.map((item) => item.payeeId))];

    const [posts, activities, payees, reviews, afterSalesList] =
      await Promise.all([
        postIds.length
          ? this.postRepository.find({ where: { id: In(postIds) } })
          : Promise.resolve([] as PostEntity[]),
        activityIds.length
          ? this.activityRepository.find({ where: { id: In(activityIds) } })
          : Promise.resolve([] as ActivityEntity[]),
        payeeIds.length > 0
          ? this.userRepository.find({ where: { id: In(payeeIds) } })
          : Promise.resolve([] as UserEntity[]),
        orderIds.length
          ? this.reviewRepository.find({
              where: { paymentOrderId: In(orderIds) },
            })
          : Promise.resolve([]),
        this.afterSalesRepository
          .createQueryBuilder('afterSales')
          .where('afterSales.paymentOrderId IN (:...orderIds)', { orderIds })
          .andWhere('afterSales.userId = :viewerUserId', { viewerUserId })
          .orderBy('afterSales.createdAt', 'DESC')
          .getMany(),
      ]);

    const postMap = new Map(posts.map((item) => [item.id, item]));
    const activityMap = new Map(activities.map((item) => [item.id, item]));
    const payeeMap = new Map(payees.map((item) => [item.id, item]));
    const reviewSet = new Set(reviews.map((item) => item.paymentOrderId));
    const afterSalesMap = new Map<number, PaymentAfterSalesEntity>();
    for (const item of afterSalesList) {
      if (!afterSalesMap.has(item.paymentOrderId)) {
        afterSalesMap.set(item.paymentOrderId, item);
      }
    }

    return orders.map((order) => {
      const post =
        order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE
          ? postMap.get(order.bizId)
          : undefined;
      const activity =
        order.bizType === PaymentBizType.ACTIVITY_JOIN
          ? activityMap.get(order.bizId)
          : undefined;
      const payee = payeeMap.get(order.payeeId);
      const afterSales = afterSalesMap.get(order.id);
      const hasReview = reviewSet.has(order.id);
      const canConfirmReceipt =
        order.userId === viewerUserId &&
        order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE &&
        order.status === PaymentOrderStatus.PAID;
      const canPay = order.status === PaymentOrderStatus.PENDING;
      const canReview =
        order.status === PaymentOrderStatus.SETTLED && !hasReview;
      const canApplyAfterSales = this.canApplyAfterSales(order, afterSales);

      return {
        outTradeNo: order.outTradeNo,
        amount: order.amount,
        subject: order.subject,
        status: order.status,
        statusLabel: this.buildStatusLabel(order, afterSales),
        bizType: order.bizType,
        bizId: order.bizId,
        bizTitle: post?.title ?? activity?.title ?? order.subject,
        coverImage:
          post?.images?.[0] ?? activity?.images?.[0] ?? null,
        payeeId: order.payeeId,
        payeeName: payee?.nickname?.trim() || `同学${order.payeeId}`,
        payeeAvatarUrl: payee?.avatarUrl ?? null,
        paidAt: order.paidAt,
        createdAt: order.createdAt,
        canPay,
        canConfirmReceipt,
        canReview,
        canApplyAfterSales,
        hasReview,
        afterSalesStatus: afterSales?.status ?? null,
      };
    });
  }

  private canApplyAfterSales(
    order: PaymentOrderEntity,
    afterSales?: PaymentAfterSalesEntity,
  ): boolean {
    if (order.status === PaymentOrderStatus.REFUNDED) {
      return false;
    }
    if (
      afterSales &&
      [
        PaymentAfterSalesStatus.PENDING,
        PaymentAfterSalesStatus.REFUNDING,
        PaymentAfterSalesStatus.APPROVED,
      ].includes(afterSales.status)
    ) {
      return false;
    }
    if (order.status === PaymentOrderStatus.PENDING) {
      return false;
    }
    if (
      order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE &&
      order.status === PaymentOrderStatus.PAID
    ) {
      return true;
    }
    if (order.status === PaymentOrderStatus.SETTLED) {
      return true;
    }
    return false;
  }

  private buildStatusLabel(
    order: PaymentOrderEntity,
    afterSales?: PaymentAfterSalesEntity,
  ): string {
    if (afterSales) {
      switch (afterSales.status) {
        case PaymentAfterSalesStatus.PENDING:
          return '售后处理中';
        case PaymentAfterSalesStatus.APPROVED:
        case PaymentAfterSalesStatus.REFUNDING:
          return '退款处理中';
        case PaymentAfterSalesStatus.REFUNDED:
          return '已退款';
        case PaymentAfterSalesStatus.REJECTED:
          return '售后已拒绝';
        case PaymentAfterSalesStatus.CANCELLED:
          return '售后已取消';
        default:
          break;
      }
    }

    switch (order.status) {
      case PaymentOrderStatus.PENDING:
        return '待付款';
      case PaymentOrderStatus.PAID:
        return order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE
          ? '待收货'
          : '已报名';
      case PaymentOrderStatus.CONFIRMED:
        return '待分账';
      case PaymentOrderStatus.SETTLED:
        return order.bizType === PaymentBizType.ACTIVITY_JOIN
          ? '已完成'
          : '交易完成';
      case PaymentOrderStatus.REFUNDED:
        return '已退款';
      case PaymentOrderStatus.CLOSED:
        return '已关闭';
      case PaymentOrderStatus.FAILED:
        return '支付失败';
      default:
        return order.status;
    }
  }
}
