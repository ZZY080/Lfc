import { Module, Global, OnModuleInit } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { DataSource } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { PostProductEntity } from '@module/post/entity/post-product.entity';
import { PostLikeEntity } from '@module/post/entity/post-like.entity';
import { PostFavoriteEntity } from '@module/post/entity/post-favorite.entity';
import { PostCommentEntity } from '@module/post/entity/post-comment.entity';
import { PostCommentLikeEntity } from '@module/post/entity/post-comment-like.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { ActivityLikeEntity } from '@module/activity/entity/activity-like.entity';
import { ActivityFavoriteEntity } from '@module/activity/entity/activity-favorite.entity';
import { NotificationEntity } from '@module/message/entity/notification.entity';
import { ConversationEntity } from '@module/message/entity/conversation.entity';
import { UserFollowEntity } from '@module/user/entity/user-follow.entity';
import { MessageEntity } from '@module/message/entity/message.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentPayoutEntity } from '@module/payment/entity/payment-payout.entity';
import { PaymentOrderReviewEntity } from '@module/payment/entity/payment-order-review.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';

@Global()
@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        type: 'mysql',
        host: configService.get<string>('MYSQL_HOST'),
        port: parseInt(configService.get<string>('MYSQL_PORT') || '3306', 10),
        username: configService.get<string>('MYSQL_USERNAME'),
        password: configService.get<string>('MYSQL_PASSWORD'),
        database: configService.get<string>('MYSQL_DATABASE'),
        synchronize: process.env.MYSQL_SYNCHRONIZE !== 'false',
        entities: [
          UserEntity,
          PostEntity,
          PostProductEntity,
          PostLikeEntity,
          PostFavoriteEntity,
          PostCommentEntity,
          PostCommentLikeEntity,
          ActivityEntity,
          ActivityParticipantEntity,
          ActivityLikeEntity,
          ActivityFavoriteEntity,
          NotificationEntity,
          ConversationEntity,
          MessageEntity,
          UserFollowEntity,
          PaymentOrderEntity,
          PaymentPayoutEntity,
          PaymentOrderReviewEntity,
          PaymentAfterSalesEntity,
        ],
        charset: 'utf8mb4',
        timezone: '+08:00',
      }),
    }),
  ],
  exports: [TypeOrmModule],
})
export class MysqlModule implements OnModuleInit {
  constructor(private readonly dataSource: DataSource) {}

  async onModuleInit() {
    if (this.dataSource.isInitialized) {
      console.log('Mysql连接成功');
    } else {
      console.error('Mysql连接失败');
    }
  }
}
