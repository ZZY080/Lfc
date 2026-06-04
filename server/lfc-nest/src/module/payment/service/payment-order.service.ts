import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, LessThanOrEqual, Repository } from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import {
  PaymentBizType,
  PaymentChannel,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { ConsumerPostProductService } from '@module/post/service/consumer-post-product.service';
import { PaymentPayoutService } from '@module/payment/service/payment-payout.service';
import { RedisService } from '@integration/redis/service/redis.service';
import { PaymentOrderDetailDto } from '@module/payment/dto/payment.dto';
import { PaymentFeeService } from '@module/payment/service/payment-fee.service';
import { Inject } from '@nestjs/common';
import { paymentConfiguration } from '@config/configuration';
import type { IPaymentConfig } from '@config/configuration';

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

@Injectable()
export class PaymentOrderService {
  private static readonly NOTIFY_LOCK_TTL = 60 * 60 * 24;

  constructor(
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    private readonly consumerActivityService: ConsumerActivityService,
    private readonly consumerPostProductService: ConsumerPostProductService,
    private readonly paymentPayoutService: PaymentPayoutService,
    private readonly paymentFeeService: PaymentFeeService,
    private readonly redisService: RedisService,
    @Inject(paymentConfiguration.KEY)
    private readonly paymentConfig: IPaymentConfig,
  ) {}

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
    return this.paymentOrderRepository.save(
      this.paymentOrderRepository.create({
        outTradeNo: input.outTradeNo,
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
    order.status = PaymentOrderStatus.CLOSED;
    await this.paymentOrderRepository.save(order);
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

    await this.executeSettle(order);
    return this.toOrderDetail(order, userId);
  }

  async processAutoConfirmOrders(): Promise<number> {
    const now = new Date();
    const orders = await this.paymentOrderRepository.find({
      where: {
        bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
        status: PaymentOrderStatus.PAID,
        autoConfirmAt: LessThanOrEqual(now),
      },
      take: 50,
    });

    let count = 0;
    for (const order of orders) {
      try {
        order.status = PaymentOrderStatus.CONFIRMED;
        order.confirmedAt = now;
        await this.paymentOrderRepository.save(order);
        await this.executeSettle(order);
        count += 1;
      } catch {
        // 单笔失败不影响其他订单，稍后重试
      }
    }
    return count;
  }

  async completePaidOrder(input: CompletePaymentOrderInput): Promise<boolean> {
    const locked = await this.redisService.get(input.idempotencyKey);
    if (locked === 'done') {
      return true;
    }

    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo: input.outTradeNo },
    });
    if (!order) {
      return false;
    }

    if (this.isPaidOrBeyond(order.status)) {
      await this.redisService.set(
        input.idempotencyKey,
        'done',
        PaymentOrderService.NOTIFY_LOCK_TTL,
      );
      return true;
    }

    if (order.channel !== input.channel) {
      return false;
    }

    if (
      Number.parseFloat(input.amount).toFixed(2) !==
      Number.parseFloat(order.amount).toFixed(2)
    ) {
      return false;
    }

    order.status = PaymentOrderStatus.PAID;
    order.tradeNo = input.channelTradeNo;
    order.paidAt = new Date();
    if (this.paymentPayoutService.requiresConfirmBeforeSettle(order.bizType)) {
      order.autoConfirmAt = this.buildAutoConfirmDeadline(order.paidAt);
    }
    await this.paymentOrderRepository.save(order);

    await this.fulfillOrder(order);

    if (!this.paymentPayoutService.requiresConfirmBeforeSettle(order.bizType)) {
      await this.executeSettle(order);
    }

    await this.redisService.set(
      input.idempotencyKey,
      'done',
      PaymentOrderService.NOTIFY_LOCK_TTL,
    );
    return true;
  }

  generateOutTradeNo(userId: number): string {
    const suffix = Math.floor(Math.random() * 1_000_000)
      .toString()
      .padStart(6, '0');
    return `LFC${Date.now()}${userId}${suffix}`;
  }

  private async executeSettle(order: PaymentOrderEntity) {
    await this.paymentPayoutService.settlePaymentOrder(order);
    order.status = PaymentOrderStatus.SETTLED;
    order.settledAt = new Date();
    await this.paymentOrderRepository.save(order);
  }

  private buildAutoConfirmDeadline(paidAt: Date): Date {
    const deadline = new Date(paidAt);
    deadline.setDate(deadline.getDate() + this.paymentConfig.autoConfirmDays);
    return deadline;
  }

  private isPaidOrBeyond(status: PaymentOrderStatus): boolean {
    return (
      status === PaymentOrderStatus.PAID ||
      status === PaymentOrderStatus.CONFIRMED ||
      status === PaymentOrderStatus.SETTLED
    );
  }

  private async fulfillOrder(order: PaymentOrderEntity) {
    switch (order.bizType) {
      case PaymentBizType.ACTIVITY_JOIN:
        try {
          await this.consumerActivityService.joinAfterPayment(
            order.userId,
            order.bizId,
          );
        } catch (error) {
          if (error instanceof ConflictException) {
            return;
          }
          throw error;
        }
        return;
      case PaymentBizType.POST_PRODUCT_PURCHASE:
        try {
          await this.consumerPostProductService.completePurchaseAfterPayment(
            order.userId,
            order.bizId,
          );
        } catch (error) {
          if (error instanceof ConflictException) {
            return;
          }
          throw error;
        }
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
