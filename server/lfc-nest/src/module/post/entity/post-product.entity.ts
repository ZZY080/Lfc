import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  OneToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import {
  DeliveryMethod,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
} from '@shared/enum/product.enum';
import { PostEntity } from '@module/post/entity/post.entity';

@Entity({ name: 'post_product' })
export class PostProductEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'post_id', unique: true })
  postId: number;

  @OneToOne(() => PostEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'post_id' })
  post: PostEntity;

  @Column({ type: 'decimal', precision: 10, scale: 2 })
  price: string;

  @Column({
    name: 'original_price',
    type: 'decimal',
    precision: 10,
    scale: 2,
    nullable: true,
  })
  originalPrice: string | null;

  @Column({
    type: 'enum',
    enum: PostProductCategory,
    default: PostProductCategory.GENERAL,
  })
  category: PostProductCategory;

  @Column({
    type: 'enum',
    enum: ProductCondition,
    default: ProductCondition.GOOD,
  })
  condition: ProductCondition;

  @Column({
    name: 'delivery_method',
    type: 'enum',
    enum: DeliveryMethod,
    default: DeliveryMethod.PICKUP,
  })
  deliveryMethod: DeliveryMethod;

  @Column({
    type: 'enum',
    enum: PostProductStatus,
    default: PostProductStatus.ON_SALE,
  })
  status: PostProductStatus;

  @Column({ name: 'buyer_id', type: 'int', nullable: true })
  buyerId: number | null;

  @Column({ name: 'sold_at', type: 'datetime', nullable: true })
  soldAt: Date | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
