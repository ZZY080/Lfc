import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { ActivityLikeEntity } from '@module/activity/entity/activity-like.entity';
import { ActivityFavoriteEntity } from '@module/activity/entity/activity-favorite.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConsumerActivityController } from '@module/activity/controller/consumer-activity.controller';
import { AdminActivityController } from '@module/activity/controller/admin-activity.controller';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { ConsumerActivitySocialService } from '@module/activity/service/consumer-activity-social.service';
import { AdminActivityService } from '@module/activity/service/admin-activity.service';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { MessageModule } from '@module/message/message.module';
import { UserAlipayModule } from '@module/user/user-alipay.module';
import { PromotionModule } from '@module/promotion/promotion.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      ActivityEntity,
      ActivityParticipantEntity,
      ActivityLikeEntity,
      ActivityFavoriteEntity,
      UserEntity,
    ]),
    MessageModule,
    forwardRef(() => UserAlipayModule),
    forwardRef(() => PromotionModule),
  ],
  controllers: [ConsumerActivityController, AdminActivityController],
  providers: [
    ConsumerActivityService,
    ConsumerActivitySocialService,
    AdminActivityService,
    RoleAuthzService,
  ],
  exports: [ConsumerActivityService, ConsumerActivitySocialService],
})
export class ActivityModule {}
