import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { UserEntity } from '@module/user/entity/user.entity';

@Entity({ name: 'payment_order_review' })
export class PaymentOrderReviewEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Index({ unique: true })
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

  @Column({ type: 'tinyint' })
  rating: number;

  @Column({ type: 'varchar', length: 500, nullable: true })
  content: string | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
