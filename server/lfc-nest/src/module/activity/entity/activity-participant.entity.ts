import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  Unique,
} from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';

@Entity({ name: 'activity_participant' })
@Unique(['activityId', 'userId'])
export class ActivityParticipantEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'activity_id' })
  activityId: number;

  @Column({ name: 'user_id' })
  userId: number;

  @ManyToOne(() => ActivityEntity, (activity) => activity.participants, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'activity_id' })
  activity: ActivityEntity;

  @ManyToOne(() => UserEntity, (user) => user.participations, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @CreateDateColumn({ name: 'joined_at' })
  joinedAt: Date;
}
