import {
  Column,
  CreateDateColumn,
  Entity,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { UserRole, UserStatus } from '@shared/enum/user-role.enum';
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

  @Column({ name: 'real_name', type: 'varchar', length: 32 })
  realName: string;

  @Column({ name: 'student_card_url' })
  studentCardUrl: string;

  @Column({ name: 'lfc_no', type: 'varchar', length: 16, unique: true, nullable: true })
  lfcNo: string | null;

  @Column({ type: 'varchar', length: 50, nullable: true })
  nickname: string | null;

  @Column({ type: 'varchar', length: 200, nullable: true })
  bio: string | null;

  @Column({ name: 'avatar_url', type: 'varchar', length: 500, nullable: true })
  avatarUrl: string | null;

  @Column({ name: 'cover_url', type: 'varchar', length: 500, nullable: true })
  coverUrl: string | null;

  @Column({ name: 'show_comments_public', type: 'boolean', default: false })
  showCommentsPublic: boolean;

  @Column({ name: 'show_favorites_public', type: 'boolean', default: false })
  showFavoritesPublic: boolean;

  @Column({ name: 'show_likes_public', type: 'boolean', default: false })
  showLikesPublic: boolean;

  /** 发现页「我的频道」Tab 配置 */
  @Column({ name: 'feed_channels', type: 'json', nullable: true })
  feedChannels: string[] | null;

  /** 支付宝登录号（手机号或邮箱），用于 C2C 收款 */
  @Column({ name: 'alipay_login_id', type: 'varchar', length: 64, nullable: true })
  alipayLoginId: string | null;

  /** 支付宝 userId（2088 开头），OAuth 授权获得，分账推荐使用 */
  @Column({ name: 'alipay_user_id', type: 'varchar', length: 64, nullable: true })
  alipayUserId: string | null;

  /** 支付宝实名（转账校验用，建议填写） */
  @Column({ name: 'alipay_real_name', type: 'varchar', length: 32, nullable: true })
  alipayRealName: string | null;

  @Column({ name: 'alipay_bound_at', type: 'datetime', nullable: true })
  alipayBoundAt: Date | null;

  @Column({ name: 'alipay_royalty_bound_at', type: 'datetime', nullable: true })
  alipayRoyaltyBoundAt: Date | null;

  @Column({ type: 'enum', enum: UserRole, default: UserRole.CONSUMER })
  role: UserRole;

  @Column({
    type: 'enum',
    enum: UserStatus,
    default: UserStatus.ACTIVE,
  })
  status: UserStatus;

  @Column({ name: 'ban_reason', type: 'varchar', length: 255, nullable: true })
  banReason: string | null;

  @Column({ name: 'banned_at', type: 'datetime', nullable: true })
  bannedAt: Date | null;

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
