import { IsInt, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';
import {
  CreateConversationBodyDto,
  SendChatMessageBodyDto,
} from '@module/message/dto/chat.dto';
import { ChatMessageType } from '@module/message/entity/message.entity';

export class CreateConversationBodySchema implements CreateConversationBodyDto {
  @IsInt()
  peerUserId: number;
}

export class SendChatMessageBodySchema implements SendChatMessageBodyDto {
  @IsString()
  @MinLength(1, { message: '消息内容不能为空' })
  @MaxLength(2000, { message: '消息不能超过2000字' })
  content: string;

  @IsOptional()
  messageType?: ChatMessageType;
}
