import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { NotificationEntity } from '@module/message/entity/notification.entity';
import { ConversationEntity } from '@module/message/entity/conversation.entity';
import { MessageEntity } from '@module/message/entity/message.entity';
import { UserEntity } from '@module/user/entity/user.entity';
import { NotificationService } from '@module/message/service/notification.service';
import { ConsumerNotificationService } from '@module/message/service/consumer-notification.service';
import { ConsumerChatService } from '@module/message/service/consumer-chat.service';
import { ConsumerNotificationController } from '@module/message/controller/consumer-notification.controller';
import { ConsumerConversationController } from '@module/message/controller/consumer-conversation.controller';
import { RoleAuthzService } from '@shared/auth/role-authz.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      NotificationEntity,
      ConversationEntity,
      MessageEntity,
      UserEntity,
    ]),
  ],
  controllers: [ConsumerNotificationController, ConsumerConversationController],
  providers: [
    NotificationService,
    ConsumerNotificationService,
    ConsumerChatService,
    RoleAuthzService,
  ],
  exports: [NotificationService],
})
export class MessageModule {}
