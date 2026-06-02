import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import {
  appConfiguration,
  mysqlConfiguration,
  redisConfiguration,
} from '@config/configuration';
import { MysqlModule } from '@integration/mysql/mysql.module';
import { RedisModule } from '@integration/redis/redis.module';
import { AuthModule } from '@module/auth/auth.module';
import { PostModule } from '@module/post/post.module';
import { ActivityModule } from '@module/activity/activity.module';
import { MessageModule } from '@module/message/message.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [appConfiguration, mysqlConfiguration, redisConfiguration],
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
    AuthModule,
    PostModule,
    ActivityModule,
    MessageModule,
  ],
})
export class AppModule {}
