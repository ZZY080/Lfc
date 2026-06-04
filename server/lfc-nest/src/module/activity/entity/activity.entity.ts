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
}
