import { ChatMessageType } from '@module/message/entity/message.entity';

export interface CreateConversationBodyDto {
  peerUserId: number;
}

export interface SendChatMessageBodyDto {
  content: string;
  messageType?: ChatMessageType;
}

export interface ConversationListItemDto {
  id: number;
  peerUserId: number;
  peerStudentId: string;
  peerNickname: string | null;
  peerAvatarUrl: string | null;
  lastMessageContent: string | null;
  lastMessageAt: Date | null;
  unreadCount: number;
  createdAt: Date;
  updatedAt: Date;
}
