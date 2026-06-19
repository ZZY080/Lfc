import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PostEntity } from '@module/post/entity/post.entity';
import { PostProductEntity } from '@module/post/entity/post-product.entity';
import { PostLikeEntity } from '@module/post/entity/post-like.entity';
import { PostFavoriteEntity } from '@module/post/entity/post-favorite.entity';
import { PostCommentLikeEntity } from '@module/post/entity/post-comment-like.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { UserFollowEntity } from '@module/user/entity/user-follow.entity';
import { ConsumerPostController } from '@module/post/controller/consumer-post.controller';
import { AdminPostController } from '@module/post/controller/admin-post.controller';
import { AdminCommentController } from '@module/post/controller/admin-comment.controller';
import { ConsumerGeocodeController } from '@module/post/controller/consumer-geocode.controller';
import { ConsumerPostService } from '@module/post/service/consumer-post.service';
import { ConsumerPostSocialService } from '@module/post/service/consumer-post-social.service';
import { ConsumerPostProductService } from '@module/post/service/consumer-post-product.service';
import { AdminPostService } from '@module/post/service/admin-post.service';
import { AdminCommentService } from '@module/post/service/admin-comment.service';
import { UserAlipayModule } from '@module/user/user-alipay.module';
import { PromotionModule } from '@module/promotion/promotion.module';
import { MessageModule } from '@module/message/message.module';
import { FeedChannelModule } from '@module/feed-channel/feed-channel.module';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      PostEntity,
      PostProductEntity,
      PostLikeEntity,
      PostFavoriteEntity,
      PostCommentEntity,
      PostCommentLikeEntity,
      UserEntity,
      UserFollowEntity,
    ]),
    forwardRef(() => UserAlipayModule),
    forwardRef(() => PromotionModule),
    MessageModule,
    FeedChannelModule,
  ],
  controllers: [
    ConsumerPostController,
    ConsumerGeocodeController,
    AdminPostController,
    AdminCommentController,
  ],
  providers: [
    ConsumerPostService,
    ConsumerPostSocialService,
    ConsumerPostProductService,
    AdminPostService,
    AdminCommentService,
    RoleAuthzService,
  ],
  exports: [
    ConsumerPostService,
    ConsumerPostSocialService,
    ConsumerPostProductService,
  ],
})
export class PostModule {}
