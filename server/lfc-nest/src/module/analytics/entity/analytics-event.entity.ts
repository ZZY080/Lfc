import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  PrimaryGeneratedColumn,
} from 'typeorm';

@Entity({ name: 'analytics_event' })
@Index('idx_analytics_event_created_at', ['createdAt'])
@Index('idx_analytics_event_user_created', ['userId', 'createdAt'])
@Index('idx_analytics_event_event_created', ['event', 'createdAt'])
export class AnalyticsEventEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_id', type: 'int', nullable: true })
  userId: number | null;

  @Column({ length: 64 })
  event: string;

  @Column({ type: 'simple-json', nullable: true })
  properties: Record<string, unknown> | null;

  @Column({ type: 'varchar', length: 16, nullable: true })
  platform: string | null;

  @Column({ name: 'session_id', type: 'varchar', length: 64, nullable: true })
  sessionId: string | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
