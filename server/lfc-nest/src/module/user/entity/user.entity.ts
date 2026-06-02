import {
  Column,
  CreateDateColumn,
  Entity,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { UserRole } from '@shared/enum/user-role.enum';
import { PostEntity } from '@module/post/entity/post.entity';
import { ActivityEntity } from '@module/activity/entity/activity.entity';
import { ActivityParticipantEntity } from '@module/activity/entity/activity-participant.entity';

@Entity({ name: 'user' })
export class UserEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ unique: true })
  email: string;

  @Column()
  password: string;

  @Column({ name: 'student_id', unique: true })
  studentId: string;

  @Column({ name: 'student_card_url' })
  studentCardUrl: string;

  @Column({ type: 'enum', enum: UserRole, default: UserRole.CONSUMER })
  role: UserRole;

  @OneToMany(() => PostEntity, (post) => post.author)
  posts: PostEntity[];

  @OneToMany(() => ActivityEntity, (activity) => activity.author)
  activities: ActivityEntity[];

  @OneToMany(() => ActivityParticipantEntity, (item) => item.user)
  participations: ActivityParticipantEntity[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
