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
import { PaymentAfterSalesStatus } from '@shared/enum/payment.enum';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { UserEntity } from '@module/user/entity/user.entity';

@Entity({ name: 'payment_after_sales' })
export class PaymentAfterSalesEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Index()
  @Column({ name: 'payment_order_id' })
  paymentOrderId: number;

  @ManyToOne(() => PaymentOrderEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'payment_order_id' })
  paymentOrder: PaymentOrderEntity;

  @Column({ name: 'user_id' })
  userId: number;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @Column({ type: 'varchar', length: 255 })
  reason: string;

  @Column({
    type: 'enum',
    enum: PaymentAfterSalesStatus,
    default: PaymentAfterSalesStatus.PENDING,
  })
  status: PaymentAfterSalesStatus;

  @Column({ name: 'refund_amount', type: 'decimal', precision: 10, scale: 2 })
  refundAmount: string;

  @Column({ name: 'processed_at', type: 'datetime', nullable: true })
  processedAt: Date | null;

  @Column({ name: 'reject_reason', type: 'varchar', length: 255, nullable: true })
  rejectReason: string | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
