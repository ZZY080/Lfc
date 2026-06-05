import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentPayoutEntity } from '@module/payment/entity/payment-payout.entity';
import { PaymentOrderReviewEntity } from '@module/payment/entity/payment-order-review.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import { ConsumerPaymentController } from '@module/payment/controller/consumer-payment.controller';
import { ConsumerPaymentService } from '@module/payment/service/consumer-payment.service';
import { PaymentOrderService } from '@module/payment/service/payment-order.service';
import { PaymentPayoutService } from '@module/payment/service/payment-payout.service';
import { PaymentFeeService } from '@module/payment/service/payment-fee.service';
import { PaymentOrderLockService } from '@module/payment/service/payment-order-lock.service';
import { PaymentRefundService } from '@module/payment/service/payment-refund.service';
import { PaymentAutoConfirmService } from '@module/payment/service/payment-auto-confirm.service';
import { PaymentOrderQueryService } from '@module/payment/service/payment-order-query.service';
import { PaymentTransactionQueryService } from '@module/payment/service/payment-transaction-query.service';
import { PaymentReviewService } from '@module/payment/service/payment-review.service';
import { PaymentAfterSalesService } from '@module/payment/service/payment-after-sales.service';
import { ActivityModule } from '@module/activity/activity.module';
import { PostModule } from '@module/post/post.module';
import { AlipayModule } from '@integration/alipay/alipay.module';
import { UserAlipayModule } from '@module/user/user-alipay.module';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      PaymentOrderEntity,
      PaymentPayoutEntity,
      PaymentOrderReviewEntity,
      PaymentAfterSalesEntity,
      UserEntity,
      PostEntity,
      ActivityEntity,
    ]),
    forwardRef(() => ActivityModule),
    forwardRef(() => PostModule),
    forwardRef(() => UserAlipayModule),
    forwardRef(() => AlipayModule),
  ],
  controllers: [ConsumerPaymentController],
  providers: [
    PaymentOrderService,
    PaymentOrderLockService,
    PaymentPayoutService,
    PaymentFeeService,
    PaymentAutoConfirmService,
    PaymentOrderQueryService,
    PaymentTransactionQueryService,
    PaymentReviewService,
    PaymentAfterSalesService,
    PaymentRefundService,
    ConsumerPaymentService,
  ],
  exports: [
    PaymentOrderService,
    PaymentPayoutService,
    PaymentFeeService,
    ConsumerPaymentService,
  ],
})
export class PaymentModule {}
