import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
  VersionColumn,
} from 'typeorm';
import {
  PaymentBizType,
  PaymentChannel,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { UserEntity } from '@module/user/entity/user.entity';

@Entity({ name: 'payment_order' })
export class PaymentOrderEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Index({ unique: true })
  @Column({ name: 'out_trade_no', length: 64 })
  outTradeNo: string;

  /** 非终态订单唯一键，终态时置空 */
  @Index({ unique: true })
  @Column({ name: 'active_key', type: 'varchar', length: 128, nullable: true })
  activeKey: string | null;

  @Column({ name: 'user_id' })
  userId: number;

  /** C2C 收款方：闲置卖家或活动发起人 */
  @Column({ name: 'payee_id', type: 'int' })
  payeeId: number;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'payee_id' })
  payee: UserEntity;

  @Column({ name: 'biz_type', type: 'enum', enum: PaymentBizType })
  bizType: PaymentBizType;

  @Column({ name: 'biz_id', type: 'int' })
  bizId: number;

  @Column({ type: 'decimal', precision: 10, scale: 2 })
  amount: string;

  /** 平台服务费（留在商户账户） */
  @Column({ name: 'platform_fee', type: 'decimal', precision: 10, scale: 2, default: 0 })
  platformFee: string;

  /** 卖家/发起人实际到账金额 */
  @Column({ name: 'payee_amount', type: 'decimal', precision: 10, scale: 2, default: 0 })
  payeeAmount: string;

  @Column({ length: 128 })
  subject: string;

  @Column({
    type: 'enum',
    enum: PaymentChannel,
    default: PaymentChannel.ALIPAY,
  })
  channel: PaymentChannel;

  @Column({
    type: 'enum',
    enum: PaymentOrderStatus,
    default: PaymentOrderStatus.PENDING,
  })
  status: PaymentOrderStatus;

  @Column({ name: 'trade_no', type: 'varchar', length: 64, nullable: true })
  tradeNo: string | null;

  @Index({ unique: true })
  @Column({
    name: 'channel_trade_key',
    type: 'varchar',
    length: 96,
    nullable: true,
  })
  channelTradeKey: string | null;

  @Column({ name: 'paid_at', type: 'datetime', nullable: true })
  paidAt: Date | null;

  @Column({ name: 'confirmed_at', type: 'datetime', nullable: true })
  confirmedAt: Date | null;

  @Column({ name: 'settled_at', type: 'datetime', nullable: true })
  settledAt: Date | null;

  /** 自动确认收货截止时间（仅闲置商品） */
  @Column({ name: 'auto_confirm_at', type: 'datetime', nullable: true })
  autoConfirmAt: Date | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @VersionColumn({ default: 0 })
  version: number;
}
