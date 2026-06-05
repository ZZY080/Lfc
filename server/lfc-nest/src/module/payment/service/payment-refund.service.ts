import {
  BadRequestException,
  Injectable,
  Logger,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import {
  PaymentPayoutEntity,
  PaymentPayoutStatus,
} from '@module/payment/entity/payment-payout.entity';
import {
  PaymentBizType,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { ConsumerPostProductService } from '@module/post/service/consumer-post-product.service';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostProductStatus } from '@shared/enum/product.enum';

@Injectable()
export class PaymentRefundService {
  private readonly logger = new Logger(PaymentRefundService.name);

  constructor(
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    @InjectRepository(PaymentPayoutEntity)
    private readonly payoutRepository: Repository<PaymentPayoutEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly alipayService: AlipayService,
    private readonly consumerPostProductService: ConsumerPostProductService,
    private readonly consumerActivityService: ConsumerActivityService,
  ) {}

  async refundOrder(order: PaymentOrderEntity, reason: string): Promise<void> {
    if (order.status === PaymentOrderStatus.REFUNDED) {
      return;
    }
    if (
      order.status !== PaymentOrderStatus.PAID &&
      order.status !== PaymentOrderStatus.CONFIRMED &&
      order.status !== PaymentOrderStatus.SETTLED
    ) {
      throw new BadRequestException('当前订单状态不可退款');
    }
    if (!order.tradeNo) {
      throw new BadRequestException('缺少支付宝交易号，无法退款');
    }

    const outRequestNo = `REFUND${order.outTradeNo}`;
    const payout = await this.payoutRepository.findOne({
      where: { paymentOrderId: order.id },
    });
    const royaltyReturn =
      order.status === PaymentOrderStatus.SETTLED &&
      payout?.status === PaymentPayoutStatus.SUCCESS
        ? await this.buildRoyaltyReturn(order)
        : null;

    await this.alipayService.refundTrade({
      outTradeNo: order.outTradeNo,
      tradeNo: order.tradeNo,
      refundAmount: order.amount,
      outRequestNo,
      refundReason: reason,
      royaltyReturn,
    });

    await this.revertFulfillment(order);

    order.status = PaymentOrderStatus.REFUNDED;
    order.activeKey = null;
    await this.paymentOrderRepository.save(order);

    this.logger.log(
      `订单已退款 outTradeNo=${order.outTradeNo} reason=${reason}`,
    );
  }

  private async buildRoyaltyReturn(order: PaymentOrderEntity) {
    const payee = await this.userRepository.findOne({
      where: { id: order.payeeId },
    });
    if (!payee?.alipayUserId && !payee?.alipayLoginId) {
      throw new BadRequestException('收款方支付宝信息缺失，无法回退分账');
    }
    const settlement = order.payeeAmount || order.amount;
    return {
      payeeUserId: payee.alipayUserId,
      payeeLoginId: payee.alipayLoginId,
      amount: settlement,
    };
  }

  private async revertFulfillment(order: PaymentOrderEntity) {
    if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
      await this.consumerPostProductService.revertPurchaseAfterRefund(
        order.bizId,
        order.userId,
      );
      return;
    }
    if (order.bizType === PaymentBizType.ACTIVITY_JOIN) {
      try {
        await this.consumerActivityService.leave(order.userId, order.bizId);
      } catch {
        // 未报名或已取消，忽略
      }
    }
  }

  async isOrderFulfilled(order: PaymentOrderEntity): Promise<boolean> {
    if (order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE) {
      const product = await this.consumerPostProductService.findByPostId(
        order.bizId,
      );
      return (
        product?.status === PostProductStatus.SOLD &&
        product.buyerId === order.userId
      );
    }
    if (order.bizType === PaymentBizType.ACTIVITY_JOIN) {
      return this.consumerActivityService.isJoined(order.userId, order.bizId);
    }
    return false;
  }

  async isOrderUnfulfillableDueToConflict(
    order: PaymentOrderEntity,
  ): Promise<boolean> {
    if (order.bizType !== PaymentBizType.POST_PRODUCT_PURCHASE) {
      return false;
    }
    const product = await this.consumerPostProductService.findByPostId(
      order.bizId,
    );
    return (
      product?.status === PostProductStatus.SOLD &&
      product.buyerId !== order.userId
    );
  }
}
