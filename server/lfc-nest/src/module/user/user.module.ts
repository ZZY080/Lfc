import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserFollowEntity } from '@module/user/entity/user-follow.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { ConsumerUserController } from '@module/user/controller/consumer-user.controller';
import { AdminUserController } from '@module/user/controller/admin-user.controller';
import { ConsumerUserService } from '@module/user/service/consumer-user.service';
import { AdminUserService } from '@module/user/service/admin-user.service';
import { PostModule } from '@module/post/post.module';
import { ActivityModule } from '@module/activity/activity.module';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

import { UserAlipayModule } from '@module/user/user-alipay.module';
import { FeedChannelModule } from '@module/feed-channel/feed-channel.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      UserEntity,
      UserFollowEntity,
      PostEntity,
      ActivityParticipantEntity,
    ]),
    UserAlipayModule,
    FeedChannelModule,
    PostModule,
    ActivityModule,
  ],
  controllers: [ConsumerUserController, AdminUserController],
  providers: [ConsumerUserService, AdminUserService, RoleAuthzService],
})
export class UserModule {}
