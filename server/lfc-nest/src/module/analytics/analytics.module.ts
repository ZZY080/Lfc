import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AnalyticsEventEntity } from '@module/analytics/entity/analytics-event.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { AnalyticsIngestService } from '@module/analytics/service/analytics-ingest.service';
import { AdminAnalyticsService } from '@module/analytics/service/admin-analytics.service';
import { ConsumerAnalyticsController } from '@module/analytics/controller/consumer-analytics.controller';
import { AdminAnalyticsController } from '@module/analytics/controller/admin-analytics.controller';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      AnalyticsEventEntity,
      UserEntity,
      PostEntity,
      ActivityEntity,
      PaymentOrderEntity,
    ]),
  ],
  controllers: [ConsumerAnalyticsController, AdminAnalyticsController],
  providers: [AnalyticsIngestService, AdminAnalyticsService, RoleAuthzService],
  exports: [AnalyticsIngestService],
})
export class AnalyticsModule {}
