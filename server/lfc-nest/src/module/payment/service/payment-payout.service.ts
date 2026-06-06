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
import { UserEntity } from '@module/user/entity/user.entity';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { PaymentFeeService } from '@module/payment/service/payment-fee.service';
import {
  PaymentBizType,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { isPlatformDirectRevenueBizType } from '@module/payment/util/payment-order.util';

@Injectable()
export class PaymentPayoutService {
  private readonly logger = new Logger(PaymentPayoutService.name);

  constructor(
    @InjectRepository(PaymentPayoutEntity)
    private readonly payoutRepository: Repository<PaymentPayoutEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly alipayService: AlipayService,
    private readonly paymentFeeService: PaymentFeeService,
  ) {}

  async settlePaymentOrder(order: PaymentOrderEntity): Promise<boolean> {
    if (order.status === PaymentOrderStatus.SETTLED) {
      return true;
    }

    const existing = await this.payoutRepository.findOne({
      where: { paymentOrderId: order.id },
    });
    if (existing?.status === PaymentPayoutStatus.SUCCESS) {
      return true;
    }

    if (!order.tradeNo) {
      await this.saveFailedPayout(order, existing, '缺少支付宝交易号，无法分账');
      return false;
    }

    const payee = await this.userRepository.findOne({
      where: { id: order.payeeId },
    });
    if (!payee?.alipayUserId && !payee?.alipayLoginId) {
      this.logger.error(
        `分账失败: 收款方未绑定支付宝 payeeId=${order.payeeId} order=${order.outTradeNo}`,
      );
      await this.saveFailedPayout(order, existing, '收款方未绑定支付宝账号');
      return false;
    }

    const settlement = this.paymentFeeService.calculateSettlement(order.amount);
    const payeeAmount = order.payeeAmount || settlement.payeeAmount;

    // 擦亮/推广等增值服：下单时未开启分账，资金已在商户支付宝账户，无需再调分账 API
    if (
      isPlatformDirectRevenueBizType(order.bizType) ||
      Number.parseFloat(payeeAmount) <= 0
    ) {
      return true;
    }

    const platformFee = order.platformFee || settlement.platformFee;

    const outBizNo = existing?.outBizNo ?? this.generateOutBizNo(order.id);
    let payout = existing;
    if (!payout) {
      payout = await this.payoutRepository.save(
        this.payoutRepository.create({
          outBizNo,
          paymentOrderId: order.id,
          payeeId: order.payeeId,
          amount: payeeAmount,
          platformFee,
          status: PaymentPayoutStatus.PENDING,
        }),
      );
    }

    try {
      const result = await this.alipayService.settleOrderRoyalty({
        outRequestNo: outBizNo,
        tradeNo: order.tradeNo,
        payeeUserId: payee.alipayUserId,
        payeeLoginId: payee.alipayLoginId,
        payeeRealName: payee.alipayLoginId ? payee.alipayRealName : null,
        payeeAmount,
        desc: order.subject,
      });
      payout.status = PaymentPayoutStatus.SUCCESS;
      payout.alipayOrderId = result.settleNo;
      payout.errorMessage = null;
      payout.settledAt = new Date();
      await this.payoutRepository.save(payout);
      return true;
    } catch (error) {
      const message =
        error instanceof BadRequestException
          ? String(error.message)
          : '支付宝分账失败';
      this.logger.error(
        `分账失败 order=${order.outTradeNo} payee=${order.payeeId}: ${message}`,
      );
      payout.status = PaymentPayoutStatus.FAILED;
      payout.errorMessage = message.slice(0, 255);
      await this.payoutRepository.save(payout);
      throw error;
    }
  }

  requiresConfirmBeforeSettle(bizType: PaymentBizType): boolean {
    if (isPlatformDirectRevenueBizType(bizType)) {
      return false;
    }
    return (
      bizType === PaymentBizType.POST_PRODUCT_PURCHASE ||
      bizType === PaymentBizType.ACTIVITY_JOIN
    );
  }

  private async saveFailedPayout(
    order: PaymentOrderEntity,
    existing: PaymentPayoutEntity | null,
    message: string,
  ) {
    if (existing) {
      existing.status = PaymentPayoutStatus.FAILED;
      existing.errorMessage = message;
      await this.payoutRepository.save(existing);
      return;
    }
    await this.payoutRepository.save(
      this.payoutRepository.create({
        outBizNo: this.generateOutBizNo(order.id),
        paymentOrderId: order.id,
        payeeId: order.payeeId,
        amount: order.payeeAmount || order.amount,
        platformFee: order.platformFee || '0.00',
        status: PaymentPayoutStatus.FAILED,
        errorMessage: message,
      }),
    );
  }

  private generateOutBizNo(paymentOrderId: number): string {
    return `SETTLE${Date.now()}${paymentOrderId}`;
  }
}
