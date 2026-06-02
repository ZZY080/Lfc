import { MessageRelatedType, MessageType } from '@shared/enum/message-type.enum';

export interface MessageDto {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: MessageType;
  relatedType: MessageRelatedType | null;
  relatedId: number | null;
  isRead: boolean;
  createdAt: Date;
}

export interface UnreadCountDto {
  count: number;
}

export interface SendMessageDto {
  userId: number;
  title: string;
  content: string;
  type: MessageType;
  relatedType?: MessageRelatedType | null;
  relatedId?: number | null;
}
