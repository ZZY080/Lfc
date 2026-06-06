import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import {
  In,
  LessThan,
  LessThanOrEqual,
  OptimisticLockVersionMismatchError,
  QueryFailedError,
  Repository,
} from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import {
  PaymentBizType,
  PaymentChannel,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { ConsumerPostProductService } from '@module/post/service/consumer-post-product.service';
import { PaymentPayoutService } from '@module/payment/service/payment-payout.service';
import { PaymentRefundService } from '@module/payment/service/payment-refund.service';
import { PaymentOrderLockService } from '@module/payment/service/payment-order-lock.service';
import { RedisService } from '@integration/redis/service/redis.service';
import { PaymentOrderDetailDto } from '@module/payment/dto/payment.dto';
import { PaymentFeeService } from '@module/payment/service/payment-fee.service';
import { Inject } from '@nestjs/common';
import { paymentConfiguration } from '@config/configuration';
import type { IPaymentConfig } from '@config/configuration';
import {
  buildPaymentActiveKey,
  PAYMENT_PENDING_TTL_MS,
} from '@module/payment/util/payment-order.util';

export interface CreatePendingPaymentOrderInput {
  outTradeNo: string;
  userId: number;
  payeeId: number;
  bizType: PaymentBizType;
  bizId: number;
  amount: string;
  subject: string;
  channel: PaymentChannel;
}

export interface CompletePaymentOrderInput {
  outTradeNo: string;
  channel: PaymentChannel;
  channelTradeNo: string | null;
  amount: string;
  idempotencyKey: string;
}

export interface AcquirePostProductOrderInput {
  userId: number;
  postId: number;
  payeeId: number;
  amount: string;
  subject: string;
  channel: PaymentChannel;
}

export interface AcquireActivityJoinOrderInput {
  userId: number;
  activityId: number;
  payeeId: number;
  amount: string;
  subject: string;
  channel: PaymentChannel;
}

@Injectable()
export class PaymentOrderService {
  private static readonly NOTIFY_LOCK_TTL = 60 * 60 * 24;
  private readonly logger = new Logger(PaymentOrderService.name);

  constructor(
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    private readonly consumerActivityService: ConsumerActivityService,
    private readonly consumerPostProductService: ConsumerPostProductService,
    private readonly paymentPayoutService: PaymentPayoutService,
    private readonly paymentRefundService: PaymentRefundService,
    private readonly paymentOrderLockService: PaymentOrderLockService,
    private readonly paymentFeeService: PaymentFeeService,
    private readonly redisService: RedisService,
    @Inject(paymentConfiguration.KEY)
    private readonly paymentConfig: IPaymentConfig,
  ) {}

  async acquirePostProductOrder(
    input: AcquirePostProductOrderInput,
  ): Promise<PaymentOrderEntity> {
    const scopeKey = `${input.userId}:${PaymentBizType.POST_PRODUCT_PURCHASE}:${input.postId}`;
    return this.paymentOrderLockService.withCreateLock(scopeKey, async () => {
      await this.closeStalePendingOrdersForUser(
        input.userId,
        PaymentBizType.POST_PRODUCT_PURCHASE,
        input.postId,
      );

      const existingActive = await this.findActiveOrderForUser(
        input.userId,
        PaymentBizType.POST_PRODUCT_PURCHASE,
        input.postId,
        input.channel,
      );
      if (existingActive) {
        return existingActive;
      }

      const blocking = await this.findBlockingProductOrder(
        input.postId,
        input.userId,
      );
      if (blocking) {
        throw new BadRequestException('商品已被他人购买');
      }

      await this.paymentOrderLockService.assertProductAvailableForUser(
        input.postId,
        input.userId,
      );
      const locked = await this.paymentOrderLockService.tryAcquireProductLock(
        input.postId,
        input.userId,
      );
      if (!locked) {
        throw new ConflictException('其他用户正在购买，请稍后再试');
      }

      try {
        return await this.createPendingOrder({
          outTradeNo: this.generateOutTradeNo(input.userId),
          userId: input.userId,
          payeeId: input.payeeId,
          bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
          bizId: input.postId,
          amount: input.amount,
          subject: input.subject,
          channel: input.channel,
        });
      } catch (error) {
        await this.paymentOrderLockService.releaseProductLock(
          input.postId,
          input.userId,
        );
        throw error;
      }
    });
  }

  async acquireActivityJoinOrder(
    input: AcquireActivityJoinOrderInput,
  ): Promise<PaymentOrderEntity> {
    const scopeKey = `${input.userId}:${PaymentBizType.ACTIVITY_JOIN}:${input.activityId}`;
    return this.paymentOrderLockService.withCreateLock(scopeKey, async () => {
      await this.closeStalePendingOrdersForUser(
        input.userId,
        PaymentBizType.ACTIVITY_JOIN,
        input.activityId,
      );

      const existingActive = await this.findActiveOrderForUser(
        input.userId,
        PaymentBizType.ACTIVITY_JOIN,
        input.activityId,
        input.channel,
      );
      if (existingActive) {
        return existingActive;
      }

      if (
        await this.consumerActivityService.isJoined(
          input.userId,
          input.activityId,
        )
      ) {
        throw new ConflictException('您已报名该活动');
      }

      return this.createPendingOrder({
        outTradeNo: this.generateOutTradeNo(input.userId),
        userId: input.userId,
        payeeId: input.payeeId,
        bizType: PaymentBizType.ACTIVITY_JOIN,
        bizId: input.activityId,
        amount: input.amount,
        subject: input.subject,
        channel: input.channel,
      });
    });
  }

  async findPendingOrder(input: {
    userId: number;
    bizType: PaymentBizType;
    bizId: number;
    channel: PaymentChannel;
  }): Promise<PaymentOrderEntity | null> {
    return this.paymentOrderRepository.findOne({
      where: {
        userId: input.userId,
        bizType: input.bizType,
        bizId: input.bizId,
        channel: input.channel,
        status: PaymentOrderStatus.PENDING,
      },
      order: { createdAt: 'DESC' },
    });
  }

  async findActiveOrderForUser(
    userId: number,
    bizType: PaymentBizType,
    bizId: number,
    channel: PaymentChannel,
  ): Promise<PaymentOrderEntity | null> {
    const pending = await this.findPendingOrder({
      userId,
      bizType,
      bizId,
      channel,
    });
    if (pending) {
      return pending;
    }

    return this.paymentOrderRepository.findOne({
      where: {
        userId,
        bizType,
        bizId,
        channel,
        status: In([
          PaymentOrderStatus.PAID,
          PaymentOrderStatus.CONFIRMED,
          PaymentOrderStatus.SETTLED,
        ]),
      },
      order: { createdAt: 'DESC' },
    });
  }

  async findProductOrderForBuyer(
    userId: number,
    postId: number,
  ): Promise<PaymentOrderDetailDto | null> {
    const order = await this.paymentOrderRepository.findOne({
      where: {
        userId,
        bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
        bizId: postId,
        status: In([
          PaymentOrderStatus.PAID,
          PaymentOrderStatus.CONFIRMED,
          PaymentOrderStatus.SETTLED,
        ]),
      },
      order: { createdAt: 'DESC' },
    });
    if (!order) {
      return null;
    }
    return this.toOrderDetail(order, userId);
  }

  async createPendingOrder(
    input: CreatePendingPaymentOrderInput,
  ): Promise<PaymentOrderEntity> {
    const settlement = this.paymentFeeService.calculateSettlement(input.amount);
    const activeKey = buildPaymentActiveKey(
      input.bizType,
      input.bizId,
      input.userId,
    );

    try {
      return await this.paymentOrderRepository.save(
        this.paymentOrderRepository.create({
          outTradeNo: input.outTradeNo,
          activeKey,
          userId: input.userId,
          payeeId: input.payeeId,
          bizType: input.bizType,
          bizId: input.bizId,
          amount: input.amount,
          platformFee: settlement.platformFee,
          payeeAmount: settlement.payeeAmount,
          subject: input.subject,
          channel: input.channel,
          status: PaymentOrderStatus.PENDING,
        }),
      );
    } catch (error) {
      if (this.isDuplicateActiveKeyError(error)) {
        const existing = await this.paymentOrderRepository.findOne({
          where: { activeKey },
        });
        if (existing) {
          return existing;
        }
      }
      throw error;
    }
  }

  async findOrderForUser(
    userId: number,
    outTradeNo: string,
  ): Promise<PaymentOrderDetailDto> {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo, userId },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    return this.toOrderDetail(order, userId);
  }

  async cancelPendingOrder(userId: number, outTradeNo: string) {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo, userId },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    if (order.status !== PaymentOrderStatus.PENDING) {
      throw new BadRequestException('仅待付款订单可取消');
    }
    await this.closeOrder(order);
    return this.toOrderDetail(order, userId);
  }

  async repayPendingOrder(userId: number, outTradeNo: string) {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo, userId },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    if (order.status !== PaymentOrderStatus.PENDING) {
      throw new BadRequestException('订单状态不可支付');
    }

    if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
      const blocking = await this.findBlockingProductOrder(
        order.bizId,
        order.userId,
      );
      if (blocking) {
        await this.closeOrder(order);
        throw new BadRequestException('商品已被他人购买');
      }
      await this.paymentOrderLockService.refreshProductLock(
        order.bizId,
        order.userId,
      );
    }

    return order;
  }

  async confirmReceipt(userId: number, outTradeNo: string) {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    if (order.userId !== userId) {
      throw new ForbiddenException('仅买家可确认收货');
    }
    if (order.bizType !== PaymentBizType.POST_PRODUCT_PURCHASE) {
      throw new BadRequestException('该订单不支持确认收货');
    }
    if (order.status === PaymentOrderStatus.SETTLED) {
      return this.toOrderDetail(order, userId);
    }
    if (order.status === PaymentOrderStatus.REFUNDED) {
      throw new BadRequestException('订单已退款');
    }
    if (
      order.status !== PaymentOrderStatus.PAID &&
      order.status !== PaymentOrderStatus.CONFIRMED
    ) {
      throw new BadRequestException('当前订单状态不可确认收货');
    }

    if (order.status === PaymentOrderStatus.PAID) {
      order.status = PaymentOrderStatus.CONFIRMED;
      order.confirmedAt = new Date();
      await this.paymentOrderRepository.save(order);
    }

    const settled = await this.tryFinalizeSettle(order);
    if (!settled) {
      throw new BadRequestException('分账失败，请稍后重试或联系客服');
    }
    return this.toOrderDetail(order, userId);
  }

  async closeExpiredPendingOrders(): Promise<number> {
    const cutoff = new Date(Date.now() - PAYMENT_PENDING_TTL_MS);
    const staleOrders = await this.paymentOrderRepository.find({
      where: {
        status: PaymentOrderStatus.PENDING,
        createdAt: LessThan(cutoff),
      },
      take: 100,
      order: { createdAt: 'ASC' },
    });

    let count = 0;
    for (const order of staleOrders) {
      try {
        await this.closeOrder(order);
        count += 1;
      } catch (error) {
        this.logger.warn(
          `关闭超时待支付订单失败 order=${order.outTradeNo}: ${String(error)}`,
        );
      }
    }
    return count;
  }

  async processAutoConfirmOrders(): Promise<number> {
    const now = new Date();
    const orders = await this.paymentOrderRepository.find({
      where: {
        status: PaymentOrderStatus.PAID,
        autoConfirmAt: LessThanOrEqual(now),
        bizType: In([
          PaymentBizType.POST_PRODUCT_PURCHASE,
          PaymentBizType.ACTIVITY_JOIN,
        ]),
      },
      take: 50,
    });

    let count = 0;
    for (const order of orders) {
      try {
        order.status = PaymentOrderStatus.CONFIRMED;
        order.confirmedAt = now;
        await this.paymentOrderRepository.save(order);
        if (await this.tryFinalizeSettle(order)) {
          count += 1;
        }
      } catch (error) {
        this.logger.warn(
          `自动确认分账失败 order=${order.outTradeNo}: ${String(error)}`,
        );
      }
    }
    return count;
  }

  async retryPendingSettlement(): Promise<number> {
    const orders = await this.paymentOrderRepository.find({
      where: [
        {
          bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
          status: PaymentOrderStatus.CONFIRMED,
        },
        {
          bizType: PaymentBizType.ACTIVITY_JOIN,
          status: PaymentOrderStatus.CONFIRMED,
        },
      ],
      take: 50,
      order: { updatedAt: 'ASC' },
    });

    let count = 0;
    for (const order of orders) {
      try {
        await this.syncPaidOrder(order);
        if (await this.tryFinalizeSettle(order)) {
          count += 1;
        }
      } catch (error) {
        this.logger.warn(
          `分账重试失败 order=${order.outTradeNo}: ${String(error)}`,
        );
      }
    }
    return count;
  }

  async completePaidOrder(input: CompletePaymentOrderInput): Promise<boolean> {
    const processed = await this.paymentOrderLockService.withNotifyLock(
      input.outTradeNo,
      () => this.processPaidNotify(input),
    );
    return processed ?? false;
  }

  generateOutTradeNo(userId: number): string {
    const suffix = Math.floor(Math.random() * 1_000_000)
      .toString()
      .padStart(6, '0');
    return `LFC${Date.now()}${userId}${suffix}`;
  }

  private async processPaidNotify(
    input: CompletePaymentOrderInput,
  ): Promise<boolean> {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo: input.outTradeNo },
    });
    if (!order) {
      this.logger.error(
        `支付宝回调找不到订单 outTradeNo=${input.outTradeNo} tradeNo=${input.channelTradeNo ?? ''}`,
      );
      return false;
    }

    if (order.channel !== input.channel) {
      return false;
    }

    const locked = await this.redisService.get(input.idempotencyKey);
    if (locked === 'done') {
      if (order.status !== PaymentOrderStatus.REFUNDED) {
        await this.syncPaidOrder(order);
      }
      return true;
    }

    if (this.isPaidOrBeyond(order.status)) {
      if (order.status !== PaymentOrderStatus.REFUNDED) {
        await this.syncPaidOrder(order);
      }
      await this.markNotifyDone(input.idempotencyKey);
      return true;
    }

    if (
      Number.parseFloat(input.amount).toFixed(2) !==
      Number.parseFloat(order.amount).toFixed(2)
    ) {
      this.logger.error(
        `支付宝回调金额不匹配 order=${order.outTradeNo} expected=${order.amount} actual=${input.amount}`,
      );
      return false;
    }

    if (input.channelTradeNo) {
      const channelTradeKey = this.buildChannelTradeKey(
        input.channel,
        input.channelTradeNo,
      );
      const duplicateTrade = await this.paymentOrderRepository.findOne({
        where: { channelTradeKey },
      });
      if (duplicateTrade && duplicateTrade.id !== order.id) {
        this.logger.error(
          `支付宝交易号已被其他订单使用 tradeNo=${input.channelTradeNo} existing=${duplicateTrade.outTradeNo}`,
        );
        return true;
      }
    }

    const marked = await this.markOrderPaid(order, input);
    if (!marked) {
      return false;
    }

    await this.syncPaidOrder(marked);

    const latest = await this.paymentOrderRepository.findOne({
      where: { id: marked.id },
    });
    if (latest && this.shouldDeferNotifyDone(latest)) {
      return true;
    }

    await this.markNotifyDone(input.idempotencyKey);
    return true;
  }

  private async markOrderPaid(
    order: PaymentOrderEntity,
    input: CompletePaymentOrderInput,
  ): Promise<PaymentOrderEntity | null> {
    if (order.status !== PaymentOrderStatus.PENDING) {
      return order;
    }

    const paidAt = new Date();
    order.status = PaymentOrderStatus.PAID;
    order.tradeNo = input.channelTradeNo;
    order.paidAt = paidAt;
    if (input.channelTradeNo) {
      order.channelTradeKey = this.buildChannelTradeKey(
        input.channel,
        input.channelTradeNo,
      );
    }
    if (this.paymentPayoutService.requiresConfirmBeforeSettle(order.bizType)) {
      if (order.bizType === PaymentBizType.ACTIVITY_JOIN) {
        order.autoConfirmAt = await this.buildActivitySettleDeadline(
          order.bizId,
          paidAt,
        );
      } else {
        order.autoConfirmAt = this.buildAutoConfirmDeadline(paidAt);
      }
    }

    try {
      return await this.paymentOrderRepository.save(order);
    } catch (error) {
      if (error instanceof OptimisticLockVersionMismatchError) {
        const latest = await this.paymentOrderRepository.findOne({
          where: { id: order.id },
        });
        if (latest && this.isPaidOrBeyond(latest.status)) {
          return latest;
        }
      }
      if (
        input.channelTradeNo &&
        this.isDuplicateChannelTradeKeyError(error)
      ) {
        const existing = await this.paymentOrderRepository.findOne({
          where: {
            channelTradeKey: this.buildChannelTradeKey(
              input.channel,
              input.channelTradeNo,
            ),
          },
        });
        if (existing) {
          return existing;
        }
      }
      throw error;
    }
  }

  private async syncPaidOrder(order: PaymentOrderEntity) {
    await this.ensureOrderFulfilled(order);

    const refreshed = await this.paymentOrderRepository.findOne({
      where: { id: order.id },
    });
    if (!refreshed || refreshed.status === PaymentOrderStatus.REFUNDED) {
      if (
        refreshed?.bizType === PaymentBizType.POST_PRODUCT_PURCHASE &&
        refreshed.status === PaymentOrderStatus.REFUNDED
      ) {
        await this.paymentOrderLockService.releaseProductLock(
          refreshed.bizId,
          refreshed.userId,
        );
      }
      return;
    }

    if (this.paymentPayoutService.requiresConfirmBeforeSettle(refreshed.bizType)) {
      if (refreshed.status === PaymentOrderStatus.CONFIRMED) {
        await this.tryFinalizeSettle(refreshed);
      }
      return;
    }

    if (refreshed.status === PaymentOrderStatus.PAID) {
      await this.tryFinalizeSettle(refreshed);
    }
  }

  private async tryFinalizeSettle(order: PaymentOrderEntity): Promise<boolean> {
    if (
      order.status === PaymentOrderStatus.SETTLED ||
      order.status === PaymentOrderStatus.REFUNDED
    ) {
      return order.status === PaymentOrderStatus.SETTLED;
    }

    if (this.paymentPayoutService.requiresConfirmBeforeSettle(order.bizType)) {
      if (order.status !== PaymentOrderStatus.CONFIRMED) {
        return false;
      }
    } else if (order.status !== PaymentOrderStatus.PAID) {
      return false;
    }

    const settled = await this.paymentPayoutService.settlePaymentOrder(order);
    if (!settled) {
      return false;
    }

    order.status = PaymentOrderStatus.SETTLED;
    order.settledAt = new Date();
    await this.paymentOrderRepository.save(order);
    return true;
  }

  private async ensureOrderFulfilled(order: PaymentOrderEntity) {
    if (order.status === PaymentOrderStatus.REFUNDED) {
      return;
    }

    if (await this.paymentRefundService.isOrderFulfilled(order)) {
      if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
        await this.paymentOrderLockService.releaseProductLock(
          order.bizId,
          order.userId,
        );
      }
      return;
    }

    if (await this.paymentRefundService.isOrderUnfulfillableDueToConflict(order)) {
      await this.refundWithLog(order, '商品已被他人购买');
      return;
    }

    try {
      await this.fulfillOrder(order);
      if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
        await this.paymentOrderLockService.releaseProductLock(
          order.bizId,
          order.userId,
        );
      }
    } catch (error) {
      if (error instanceof ConflictException) {
        if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
          await this.refundWithLog(order, '商品已被他人购买');
        }
        return;
      }
      if (error instanceof BadRequestException) {
        await this.refundWithLog(
          order,
          error.message || '订单无法履约，已自动退款',
        );
        return;
      }
      throw error;
    }
  }

  private async refundWithLog(order: PaymentOrderEntity, reason: string) {
    try {
      await this.paymentRefundService.refundOrder(order, reason);
      order.activeKey = null;
      await this.paymentOrderRepository.save(order);
      if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
        await this.paymentOrderLockService.releaseProductLock(
          order.bizId,
          order.userId,
        );
      }
    } catch (error) {
      this.logger.error(
        `自动退款失败 order=${order.outTradeNo} reason=${reason}: ${String(error)}`,
      );
      throw error;
    }
  }

  private async closeOrder(order: PaymentOrderEntity) {
    order.status = PaymentOrderStatus.CLOSED;
    order.activeKey = null;
    await this.paymentOrderRepository.save(order);
    if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
      await this.paymentOrderLockService.releaseProductLock(
        order.bizId,
        order.userId,
      );
    }
  }

  private async closeStalePendingOrdersForUser(
    userId: number,
    bizType: PaymentBizType,
    bizId: number,
  ) {
    const cutoff = new Date(Date.now() - PAYMENT_PENDING_TTL_MS);
    const staleOrders = await this.paymentOrderRepository.find({
      where: {
        userId,
        bizType,
        bizId,
        status: PaymentOrderStatus.PENDING,
        createdAt: LessThan(cutoff),
      },
    });
    for (const order of staleOrders) {
      await this.closeOrder(order);
    }
  }

  private async findBlockingProductOrder(postId: number, userId: number) {
    const order = await this.paymentOrderRepository.findOne({
      where: {
        bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
        bizId: postId,
        status: In([
          PaymentOrderStatus.PAID,
          PaymentOrderStatus.CONFIRMED,
          PaymentOrderStatus.SETTLED,
        ]),
      },
      order: { paidAt: 'ASC' },
    });
    if (!order || order.userId === userId) {
      return null;
    }
    return order;
  }

  private shouldDeferNotifyDone(order: PaymentOrderEntity): boolean {
    if (order.status === PaymentOrderStatus.REFUNDED) {
      return false;
    }
    if (order.bizType === PaymentBizType.ACTIVITY_JOIN) {
      return order.status === PaymentOrderStatus.PAID;
    }
    return false;
  }

  private async markNotifyDone(idempotencyKey: string) {
    await this.redisService.set(
      idempotencyKey,
      'done',
      PaymentOrderService.NOTIFY_LOCK_TTL,
    );
  }

  private buildChannelTradeKey(
    channel: PaymentChannel,
    tradeNo: string,
  ): string {
    return `${channel}:${tradeNo}`;
  }

  private isDuplicateActiveKeyError(error: unknown): boolean {
    return (
      error instanceof QueryFailedError &&
      String(error.message).includes('active_key')
    );
  }

  private isDuplicateChannelTradeKeyError(error: unknown): boolean {
    return (
      error instanceof QueryFailedError &&
      String(error.message).includes('channel_trade_key')
    );
  }

  private buildAutoConfirmDeadline(paidAt: Date): Date {
    const deadline = new Date(paidAt);
    deadline.setDate(deadline.getDate() + this.paymentConfig.autoConfirmDays);
    return deadline;
  }

  private async buildActivitySettleDeadline(
    activityId: number,
    paidAt: Date,
  ): Promise<Date> {
    try {
      const activity = await this.consumerActivityService.findOne(activityId);
      const endTime = new Date(activity.endTime);
      if (!Number.isNaN(endTime.getTime()) && endTime.getTime() > paidAt.getTime()) {
        return endTime;
      }
    } catch (error) {
      this.logger.warn(
        `读取活动结束时间失败 activityId=${activityId}: ${String(error)}`,
      );
    }
    return this.buildAutoConfirmDeadline(paidAt);
  }

  private isPaidOrBeyond(status: PaymentOrderStatus): boolean {
    return (
      status === PaymentOrderStatus.PAID ||
      status === PaymentOrderStatus.CONFIRMED ||
      status === PaymentOrderStatus.SETTLED ||
      status === PaymentOrderStatus.REFUNDED
    );
  }

  private async fulfillOrder(order: PaymentOrderEntity) {
    switch (order.bizType) {
      case PaymentBizType.ACTIVITY_JOIN:
        await this.consumerActivityService.joinAfterPayment(
          order.userId,
          order.bizId,
        );
        return;
      case PaymentBizType.POST_PRODUCT_PURCHASE:
        await this.consumerPostProductService.completePurchaseAfterPayment(
          order.userId,
          order.bizId,
        );
        return;
      default:
        throw new BadRequestException('未知支付业务类型');
    }
  }

  private toOrderDetail(
    order: PaymentOrderEntity,
    viewerUserId: number,
  ): PaymentOrderDetailDto {
    const canConfirmReceipt =
      order.userId === viewerUserId &&
      order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE &&
      order.status === PaymentOrderStatus.PAID;

    return {
      outTradeNo: order.outTradeNo,
      channel: order.channel,
      amount: order.amount,
      platformFee: order.platformFee,
      payeeAmount: order.payeeAmount,
      subject: order.subject,
      status: order.status,
      bizType: order.bizType,
      bizId: order.bizId,
      payeeId: order.payeeId,
      tradeNo: order.tradeNo,
      paidAt: order.paidAt,
      confirmedAt: order.confirmedAt,
      settledAt: order.settledAt,
      autoConfirmAt: order.autoConfirmAt,
      canConfirmReceipt,
      createdAt: order.createdAt,
    };
  }
}
