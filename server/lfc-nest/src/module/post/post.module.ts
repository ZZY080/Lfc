import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { PostLikeEntity } from '@module/post/entity/post-like.entity';
import { PostFavoriteEntity } from '@module/post/entity/post-favorite.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConsumerPostController } from '@module/post/controller/consumer-post.controller';
import { ConsumerPostService } from '@module/post/service/consumer-post.service';
import { ConsumerPostSocialService } from '@module/post/service/consumer-post-social.service';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      PostEntity,
      PostLikeEntity,
      PostFavoriteEntity,
      PostCommentEntity,
      UserEntity,
    ]),
  ],
  controllers: [ConsumerPostController],
  providers: [ConsumerPostService, ConsumerPostSocialService, RoleAuthzService],
  exports: [ConsumerPostService, ConsumerPostSocialService],
})
export class PostModule {}
