import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { UserEntity } from '@module/user/entity/user.entity';

export enum PaymentPayoutStatus {
  PENDING = 'PENDING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
}

@Entity({ name: 'payment_payout' })
export class PaymentPayoutEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Index({ unique: true })
  @Column({ name: 'out_biz_no', length: 64 })
  outBizNo: string;

  @Column({ name: 'payment_order_id' })
  paymentOrderId: number;

  @ManyToOne(() => PaymentOrderEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'payment_order_id' })
  paymentOrder: PaymentOrderEntity;

  @Column({ name: 'payee_id' })
  payeeId: number;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'payee_id' })
  payee: UserEntity;

  @Column({ type: 'decimal', precision: 10, scale: 2 })
  amount: string;

  @Column({ name: 'platform_fee', type: 'decimal', precision: 10, scale: 2, default: 0 })
  platformFee: string;

  @Column({
    type: 'enum',
    enum: PaymentPayoutStatus,
    default: PaymentPayoutStatus.PENDING,
  })
  status: PaymentPayoutStatus;

  @Column({ name: 'alipay_order_id', type: 'varchar', length: 64, nullable: true })
  alipayOrderId: string | null;

  @Column({ name: 'error_message', type: 'varchar', length: 255, nullable: true })
  errorMessage: string | null;

  @Column({ name: 'settled_at', type: 'datetime', nullable: true })
  settledAt: Date | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
