import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PaymentOrderReviewEntity } from '@module/payment/entity/payment-order-review.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentOrderStatus } from '@shared/enum/payment.enum';
import { CreateOrderReviewBodySchema } from '@module/payment/schema/payment-order.schema';

@Injectable()
export class PaymentReviewService {
  constructor(
    @InjectRepository(PaymentOrderReviewEntity)
    private readonly reviewRepository: Repository<PaymentOrderReviewEntity>,
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
  ) {}

  async createReview(
    userId: number,
    outTradeNo: string,
    body: CreateOrderReviewBodySchema,
  ) {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo, userId },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    if (order.status !== PaymentOrderStatus.SETTLED) {
      throw new BadRequestException('仅已完成订单可评价');
    }

    const existing = await this.reviewRepository.findOne({
      where: { paymentOrderId: order.id },
    });
    if (existing) {
      throw new ConflictException('该订单已评价');
    }

    const review = await this.reviewRepository.save(
      this.reviewRepository.create({
        paymentOrderId: order.id,
        userId,
        rating: body.rating,
        content: body.content?.trim() || null,
      }),
    );

    return {
      id: review.id,
      rating: review.rating,
      content: review.content,
      createdAt: review.createdAt,
    };
  }
}
