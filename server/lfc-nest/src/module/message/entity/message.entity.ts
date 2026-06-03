import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { ConversationEntity } from '@module/message/entity/conversation.entity';

export enum ChatMessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
}

@Entity({ name: 'chat_message' })
export class MessageEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'conversation_id' })
  conversationId: number;

  @Column({ name: 'sender_id' })
  senderId: number;

  @ManyToOne(() => ConversationEntity, (conversation) => conversation.messages, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'conversation_id' })
  conversation: ConversationEntity;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'sender_id' })
  sender: UserEntity;

  @Column({ type: 'text' })
  content: string;

  @Column({
    name: 'message_type',
    type: 'enum',
    enum: ChatMessageType,
    default: ChatMessageType.TEXT,
  })
  messageType: ChatMessageType;

  @Column({ name: 'is_read', type: 'boolean', default: false })
  isRead: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
