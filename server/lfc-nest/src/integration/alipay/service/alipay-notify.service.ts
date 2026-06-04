import { Injectable } from '@nestjs/common';
import { PaymentOrderService } from '@module/payment/service/payment-order.service';
import { PaymentChannel } from '@shared/enum/payment.enum';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { AlipayNotifyPayload } from '@integration/alipay/dto/alipay.dto';

@Injectable()
export class AlipayNotifyService {
  constructor(
    private readonly alipayService: AlipayService,
    private readonly paymentOrderService: PaymentOrderService,
  ) {}

  async handleNotify(payload: AlipayNotifyPayload): Promise<boolean> {
    if (!this.alipayService.verifyNotify(payload)) {
      return false;
    }

    const outTradeNo = payload.out_trade_no;
    const tradeStatus = payload.trade_status;
    if (!outTradeNo) {
      return false;
    }

    if (tradeStatus !== 'TRADE_SUCCESS' && tradeStatus !== 'TRADE_FINISHED') {
      return true;
    }

    if (!payload.total_amount) {
      return false;
    }

    return this.paymentOrderService.completePaidOrder({
      outTradeNo,
      channel: PaymentChannel.ALIPAY,
      channelTradeNo: payload.trade_no ?? null,
      amount: payload.total_amount,
      idempotencyKey: `alipay:notify:${outTradeNo}`,
    });
  }
}
