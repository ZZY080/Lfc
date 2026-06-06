import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { UserEntity } from '@module/user/entity/user.entity';

@Entity({ name: 'post' })
export class PostEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  title: string;

  @Column({ type: 'text' })
  content: string;

  @Column({ type: 'simple-json', nullable: true })
  images: string[];

  @Column({ name: 'like_count', default: 0 })
  likeCount: number;

  @Column({ name: 'favorite_count', default: 0 })
  favoriteCount: number;

  @Column({ name: 'comment_count', default: 0 })
  commentCount: number;

  @Column({ name: 'view_count', default: 0 })
  viewCount: number;

  @Column({ name: 'author_id' })
  authorId: number;

  @ManyToOne(() => UserEntity, (user) => user.posts, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'author_id' })
  author: UserEntity;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  /** 擦亮加权截止时间 */
  @Column({ name: 'boosted_until', type: 'datetime', nullable: true })
  boostedUntil: Date | null;

  @Column({ name: 'last_boosted_at', type: 'datetime', nullable: true })
  lastBoostedAt: Date | null;

  /** 当前擦亮出价（元），用于推广位排序与竞价 */
  @Column({
    name: 'boost_bid_amount',
    type: 'decimal',
    precision: 10,
    scale: 2,
    default: 0,
  })
  boostBidAmount: string;
}
