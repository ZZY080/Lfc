import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import {
  MessageRelatedType,
  MessageType,
} from '@shared/enum/message-type.enum';
import { UserEntity } from '@module/user/entity/user.entity';

@Entity({ name: 'message' })
export class MessageEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_id', type: 'int' })
  userId: number;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @Column()
  title: string;

  @Column({ type: 'text' })
  content: string;

  @Column({ type: 'enum', enum: MessageType })
  type: MessageType;

  @Column({
    name: 'related_type',
    type: 'enum',
    enum: MessageRelatedType,
    nullable: true,
  })
  relatedType: MessageRelatedType | null;

  @Column({ name: 'related_id', type: 'int', nullable: true })
  relatedId: number | null;

  @Column({ name: 'is_read', type: 'boolean', default: false })
  isRead: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
