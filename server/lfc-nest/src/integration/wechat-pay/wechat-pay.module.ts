import { Module, forwardRef } from '@nestjs/common';
import { WechatPayService } from '@integration/wechat-pay/service/wechat-pay.service';
import { WechatPayNotifyService } from '@integration/wechat-pay/service/wechat-pay-notify.service';
import { WechatPayNotifyController } from '@integration/wechat-pay/controller/wechat-pay-notify.controller';
import { PaymentModule } from '@module/payment/payment.module';

@Module({
  imports: [forwardRef(() => PaymentModule)],
  controllers: [WechatPayNotifyController],
  providers: [WechatPayService, WechatPayNotifyService],
  exports: [WechatPayService],
})
export class WechatPayModule {}
