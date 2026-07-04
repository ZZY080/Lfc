import { Injectable, ServiceUnavailableException, Inject } from '@nestjs/common';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { PaymentFeeService } from '@module/payment/service/payment-fee.service';
import { CreatePaymentResultDto } from '@module/payment/dto/payment.dto';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import {
  PaymentChannel,
} from '@shared/enum/payment.enum';
import { shouldEnableAlipayRoyalty } from '@module/payment/util/payment-order.util';
import { alipayConfiguration, type IAlipayConfig } from '@config/configuration';

@Injectable()
export class PaymentCheckoutService {
  constructor(
    private readonly alipayService: AlipayService,
    private readonly paymentFeeService: PaymentFeeService,
    @Inject(alipayConfiguration.KEY)
    private readonly alipayConfig: IAlipayConfig,
  ) {}

  assertAlipayConfigured(): void {
    if (!this.alipayService.isConfigured()) {
      throw new ServiceUnavailableException('支付宝支付未配置');
    }
  }

  buildPaymentResultForOrder(order: PaymentOrderEntity): CreatePaymentResultDto {
    const settlement = this.paymentFeeService.calculateSettlement(order.amount);
    const platformFee = order.platformFee || settlement.platformFee;
    const payeeAmount = order.payeeAmount || settlement.payeeAmount;
    const config = this.paymentFeeService.getPublicConfig();

    const { orderStr } = this.alipayService.createAppPayOrder({
      outTradeNo: order.outTradeNo,
      totalAmount: order.amount,
      subject: order.subject,
      enableRoyalty:
        this.alipayConfig.royaltyEnabled &&
        shouldEnableAlipayRoyalty(order.bizType),
    });

    return {
      outTradeNo: order.outTradeNo,
      channel: PaymentChannel.ALIPAY,
      amount: order.amount,
      platformFee,
      payeeAmount,
      platformFeeRateLabel: config.platformFeeRateLabel,
      subject: order.subject,
      status: order.status,
      payeeId: order.payeeId,
      alipay: { orderStr },
    };
  }
}
