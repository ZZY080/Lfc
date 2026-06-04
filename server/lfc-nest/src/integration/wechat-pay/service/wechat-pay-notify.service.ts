import { Injectable } from '@nestjs/common';
import { PaymentOrderService } from '@module/payment/service/payment-order.service';
import { WechatPayService } from '@integration/wechat-pay/service/wechat-pay.service';

@Injectable()
export class WechatPayNotifyService {
  constructor(
    private readonly wechatPayService: WechatPayService,
    private readonly paymentOrderService: PaymentOrderService,
  ) {}

  async handleNotify(
    headers: Record<string, string>,
    body: string,
  ): Promise<boolean> {
    if (!this.wechatPayService.verifyNotify(headers, body)) {
      return false;
    }

    // TODO: 解析微信支付 V3 回调 JSON，调用 paymentOrderService.completePaidOrder
    return false;
  }
}
