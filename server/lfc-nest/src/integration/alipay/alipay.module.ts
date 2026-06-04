import { Module, forwardRef } from '@nestjs/common';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { AlipayNotifyService } from '@integration/alipay/service/alipay-notify.service';
import { AlipayNotifyController } from '@integration/alipay/controller/alipay-notify.controller';
import { PaymentModule } from '@module/payment/payment.module';

@Module({
  imports: [forwardRef(() => PaymentModule)],
  controllers: [AlipayNotifyController],
  providers: [AlipayService, AlipayNotifyService],
  exports: [AlipayService],
})
export class AlipayModule {}
