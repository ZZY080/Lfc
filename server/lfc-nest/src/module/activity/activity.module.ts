import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConsumerActivityController } from '@module/activity/controller/consumer-activity.controller';
import { AdminActivityController } from '@module/activity/controller/admin-activity.controller';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { AdminActivityService } from '@module/activity/service/admin-activity.service';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { MessageModule } from '@module/message/message.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      ActivityEntity,
      ActivityParticipantEntity,
      UserEntity,
    ]),
    MessageModule,
  ],
  controllers: [ConsumerActivityController, AdminActivityController],
  providers: [ConsumerActivityService, AdminActivityService, RoleAuthzService],
})
export class ActivityModule {}
