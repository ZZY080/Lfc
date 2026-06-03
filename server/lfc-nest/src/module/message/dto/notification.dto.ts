import { MessageRelatedType, MessageType } from '@shared/enum/message-type.enum';

export interface NotificationDto {
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

export interface SendNotificationDto {
  userId: number;
  title: string;
  content: string;
  type: MessageType;
  relatedType?: MessageRelatedType | null;
  relatedId?: number | null;
}
