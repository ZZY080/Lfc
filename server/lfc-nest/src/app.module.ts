import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import {
  appConfiguration,
  mysqlConfiguration,
  redisConfiguration,
  alipayConfiguration,
  wechatPayConfiguration,
  paymentConfiguration,
  promotionConfiguration,
} from '@config/configuration';
import { MysqlModule } from '@integration/mysql/mysql.module';
import { RedisModule } from '@integration/redis/redis.module';
import { AuthModule } from '@module/auth/auth.module';
import { PostModule } from '@module/post/post.module';
import { ActivityModule } from '@module/activity/activity.module';
import { MessageModule } from '@module/message/message.module';
import { UserModule } from '@module/user/user.module';
import { AliyunModule } from '@integration/aliyun/aliyun.module';
import { AmapModule } from '@integration/amap/amap.module';
import { AlipayModule } from '@integration/alipay/alipay.module';
import { WechatPayModule } from '@integration/wechat-pay/wechat-pay.module';
import { PaymentModule } from '@module/payment/payment.module';
import { PromotionModule } from '@module/promotion/promotion.module';
import { AdminModule } from '@module/admin/admin.module';
import { FeedChannelModule } from '@module/feed-channel/feed-channel.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [
        appConfiguration,
        mysqlConfiguration,
        redisConfiguration,
        alipayConfiguration,
        wechatPayConfiguration,
        paymentConfiguration,
        promotionConfiguration,
      ],
      envFilePath: `.env.${process.env.NODE_ENV}`,
    }),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => ({
        secret: configService.get<string>('JWT_SECRET'),
        signOptions: { expiresIn: '7d' },
      }),
      global: true,
    }),
    MysqlModule,
    RedisModule,
    AliyunModule,
    AmapModule,
    AlipayModule,
    WechatPayModule,
    AuthModule,
    PostModule,
    ActivityModule,
    MessageModule,
    UserModule,
    PaymentModule,
    PromotionModule,
    AdminModule,
    FeedChannelModule,
  ],
})
export class AppModule {}
