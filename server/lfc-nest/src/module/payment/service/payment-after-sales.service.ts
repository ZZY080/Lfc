import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import {
  PaymentAfterSalesStatus,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { ApplyAfterSalesBodySchema } from '@module/payment/schema/payment-order.schema';
import { PaymentRefundService } from '@module/payment/service/payment-refund.service';

@Injectable()
export class PaymentAfterSalesService {
  constructor(
    @InjectRepository(PaymentAfterSalesEntity)
    private readonly afterSalesRepository: Repository<PaymentAfterSalesEntity>,
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    private readonly paymentRefundService: PaymentRefundService,
  ) {}

  async apply(userId: number, outTradeNo: string, body: ApplyAfterSalesBodySchema) {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo, userId },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    this.assertCanApply(order);

    const active = await this.afterSalesRepository.findOne({
      where: {
        paymentOrderId: order.id,
        userId,
      },
      order: { createdAt: 'DESC' },
    });
    if (
      active &&
      [
        PaymentAfterSalesStatus.PENDING,
        PaymentAfterSalesStatus.REFUNDING,
        PaymentAfterSalesStatus.APPROVED,
      ].includes(active.status)
    ) {
      throw new ConflictException('该订单已有进行中的售后申请');
    }

    const afterSales = await this.afterSalesRepository.save(
      this.afterSalesRepository.create({
        paymentOrderId: order.id,
        userId,
        reason: body.reason.trim(),
        refundAmount: order.amount,
        status: PaymentAfterSalesStatus.PENDING,
      }),
    );

    await this.processRefund(order, afterSales);
    return this.toDto(afterSales);
  }

  async cancel(userId: number, outTradeNo: string) {
    const order = await this.paymentOrderRepository.findOne({
      where: { outTradeNo, userId },
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }

    const afterSales = await this.afterSalesRepository.findOne({
      where: { paymentOrderId: order.id, userId },
      order: { createdAt: 'DESC' },
    });
    if (!afterSales || afterSales.status !== PaymentAfterSalesStatus.PENDING) {
      throw new BadRequestException('当前售后申请不可取消');
    }

    afterSales.status = PaymentAfterSalesStatus.CANCELLED;
    await this.afterSalesRepository.save(afterSales);
    return this.toDto(afterSales);
  }

  private assertCanApply(order: PaymentOrderEntity) {
    if (order.status === PaymentOrderStatus.REFUNDED) {
      throw new BadRequestException('订单已退款');
    }
    if (order.status === PaymentOrderStatus.PENDING) {
      throw new BadRequestException('未支付订单请直接取消');
    }
    if (
      order.status === PaymentOrderStatus.PAID ||
      order.status === PaymentOrderStatus.CONFIRMED ||
      order.status === PaymentOrderStatus.SETTLED
    ) {
      return;
    }
    throw new BadRequestException('当前订单状态不可申请售后');
  }

  private async processRefund(
    order: PaymentOrderEntity,
    afterSales: PaymentAfterSalesEntity,
  ) {
    afterSales.status = PaymentAfterSalesStatus.REFUNDING;
    await this.afterSalesRepository.save(afterSales);

    try {
      await this.paymentRefundService.refundOrder(order, afterSales.reason);

      afterSales.status = PaymentAfterSalesStatus.REFUNDED;
      afterSales.processedAt = new Date();
      await this.afterSalesRepository.save(afterSales);
    } catch (error) {
      afterSales.status = PaymentAfterSalesStatus.PENDING;
      await this.afterSalesRepository.save(afterSales);
      throw error;
    }
  }

  private toDto(afterSales: PaymentAfterSalesEntity) {
    return {
      id: afterSales.id,
      reason: afterSales.reason,
      status: afterSales.status,
      refundAmount: afterSales.refundAmount,
      processedAt: afterSales.processedAt,
      rejectReason: afterSales.rejectReason,
      createdAt: afterSales.createdAt,
    };
  }
}
