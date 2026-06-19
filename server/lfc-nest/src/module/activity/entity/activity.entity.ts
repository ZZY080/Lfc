import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { ActivityStatus } from '@shared/enum/user-role.enum';
import { UserEntity } from '@module/user/entity/user.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';

@Entity({ name: 'activity' })
export class ActivityEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  title: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ type: 'simple-json', nullable: true })
  images: string[];

  @Column()
  location: string;

  @Column({ type: 'double', nullable: true })
  latitude: number | null;

  @Column({ type: 'double', nullable: true })
  longitude: number | null;

  @Column({ name: 'start_time', type: 'datetime' })
  startTime: Date;

  @Column({ name: 'end_time', type: 'datetime' })
  endTime: Date;

  @Column({ name: 'max_participants', type: 'int', default: 0 })
  maxParticipants: number;

  @Column({ type: 'decimal', precision: 10, scale: 2, default: 0 })
  fee: string;

  @Column({ type: 'enum', enum: ActivityStatus, default: ActivityStatus.PENDING })
  status: ActivityStatus;

  /** 管理员审核意见（拒绝时填写） */
  @Column({ name: 'review_comment', type: 'text', nullable: true })
  reviewComment: string | null;

  @Column({ name: 'author_id' })
  authorId: number;

  @ManyToOne(() => UserEntity, (user) => user.activities, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'author_id' })
  author: UserEntity;

  @OneToMany(() => ActivityParticipantEntity, (item) => item.activity)
  participants: ActivityParticipantEntity[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  /** 活动 Tab 推广位截止时间 */
  @Column({ name: 'promoted_until', type: 'datetime', nullable: true })
  promotedUntil: Date | null;

  @Column({ name: 'last_promoted_at', type: 'datetime', nullable: true })
  lastPromotedAt: Date | null;

  /** 当前推广出价（元），用于推广位排序与竞价 */
  @Column({
    name: 'promote_bid_amount',
    type: 'decimal',
    precision: 10,
    scale: 2,
    default: 0,
  })
  promoteBidAmount: string;
}
