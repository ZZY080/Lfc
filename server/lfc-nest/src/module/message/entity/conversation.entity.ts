import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  Unique,
  UpdateDateColumn,
} from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { MessageEntity } from '@module/message/entity/message.entity';

@Entity({ name: 'conversation' })
@Unique(['userOneId', 'userTwoId'])
export class ConversationEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_one_id' })
  userOneId: number;

  @Column({ name: 'user_two_id' })
  userTwoId: number;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_one_id' })
  userOne: UserEntity;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_two_id' })
  userTwo: UserEntity;

  @Column({ name: 'last_message_content', type: 'varchar', length: 500, nullable: true })
  lastMessageContent: string | null;

  @Column({ name: 'last_message_at', type: 'datetime', nullable: true })
  lastMessageAt: Date | null;

  @Column({ name: 'user_one_unread_count', type: 'int', default: 0 })
  userOneUnreadCount: number;

  @Column({ name: 'user_two_unread_count', type: 'int', default: 0 })
  userTwoUnreadCount: number;

  @Column({ name: 'user_one_hidden', type: 'boolean', default: false })
  userOneHidden: boolean;

  @Column({ name: 'user_two_hidden', type: 'boolean', default: false })
  userTwoHidden: boolean;

  @OneToMany(() => MessageEntity, (message) => message.conversation)
  messages: MessageEntity[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
