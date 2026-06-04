import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserFollowEntity } from '@module/user/entity/user-follow.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { ConsumerUserController } from '@module/user/controller/consumer-user.controller';
import { ConsumerUserService } from '@module/user/service/consumer-user.service';
import { PostModule } from '@module/post/post.module';
import { ActivityModule } from '@module/activity/activity.module';

import { UserAlipayModule } from '@module/user/user-alipay.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      UserEntity,
      UserFollowEntity,
      PostEntity,
      ActivityParticipantEntity,
    ]),
    UserAlipayModule,
    PostModule,
    ActivityModule,
  ],
  controllers: [ConsumerUserController],
  providers: [ConsumerUserService],
})
export class UserModule {}
