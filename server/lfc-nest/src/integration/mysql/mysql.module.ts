import { Module, Global, OnModuleInit } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { DataSource } from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';
import { MessageEntity } from '@module/message/entity/message.entity';

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
          ActivityEntity,
          ActivityParticipantEntity,
          MessageEntity,
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
