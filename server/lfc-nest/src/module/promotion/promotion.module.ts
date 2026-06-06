import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ConsumerPromotionController } from '@module/promotion/controller/consumer-promotion.controller';
import { ConsumerPromotionService } from '@module/promotion/service/consumer-promotion.service';
import { PaymentModule } from '@module/payment/payment.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([PostEntity, ActivityEntity]),
    forwardRef(() => PaymentModule),
  ],
  controllers: [ConsumerPromotionController],
  providers: [ConsumerPromotionService],
  exports: [ConsumerPromotionService],
})
export class PromotionModule {}
