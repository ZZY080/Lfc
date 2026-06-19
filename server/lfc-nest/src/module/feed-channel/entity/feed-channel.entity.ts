import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity({ name: 'feed_channel' })
export class FeedChannelEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ type: 'varchar', length: 32, unique: true })
  name: string;

  @Column({ name: 'sort_order', type: 'int', default: 0 })
  sortOrder: number;

  /** 「推荐」专属频道，不可从「我的频道」移除 */
  @Column({ name: 'is_recommend', type: 'boolean', default: false })
  isRecommend: boolean;

  /** 新用户默认展示在「我的频道」 */
  @Column({ name: 'default_in_my', type: 'boolean', default: false })
  defaultInMy: boolean;

  @Column({ name: 'is_active', type: 'boolean', default: true })
  isActive: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
