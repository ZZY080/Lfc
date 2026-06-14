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
import { DEFAULT_POST_CATEGORY } from '@shared/enum/post-category.enum';

@Entity({ name: 'post' })
export class PostEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  title: string;

  /** 笔记类型（Feed 频道），如校园生活、美食 */
  @Column({ type: 'varchar', length: 32, default: DEFAULT_POST_CATEGORY })
  category: string;

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

  /** 是否在公域 Feed/搜索展示；作者本人仍可见 */
  @Column({ name: 'is_visible', type: 'boolean', default: true })
  isVisible: boolean;

  /** 发布时定位（可选），用于展示距离 */
  @Column({ type: 'double', nullable: true })
  latitude: number | null;

  @Column({ type: 'double', nullable: true })
  longitude: number | null;

  /** 发布时地址文案（可选），用于 Feed 展示 */
  @Column({ type: 'varchar', length: 255, nullable: true })
  location: string | null;

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
