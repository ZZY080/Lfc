import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MessageEntity } from '@module/message/entity/message.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { MessageService } from '@module/message/service/message.service';
import { ConsumerMessageService } from '@module/message/service/consumer-message.service';
import { ConsumerMessageController } from '@module/message/controller/consumer-message.controller';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [TypeOrmModule.forFeature([MessageEntity, UserEntity])],
  controllers: [ConsumerMessageController],
  providers: [MessageService, ConsumerMessageService, RoleAuthzService],
  exports: [MessageService],
})
export class MessageModule {}
