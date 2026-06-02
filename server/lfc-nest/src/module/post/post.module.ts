import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConsumerPostController } from '@module/post/controller/consumer-post.controller';
import { ConsumerPostService } from '@module/post/service/consumer-post.service';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [TypeOrmModule.forFeature([PostEntity, UserEntity])],
  controllers: [ConsumerPostController],
  providers: [ConsumerPostService, RoleAuthzService],
})
export class PostModule {}
