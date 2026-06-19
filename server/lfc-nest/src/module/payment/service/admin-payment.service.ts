import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentAfterSalesEntity } from '@module/payment/entity/payment-after-sales.entity';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import { AdminPaymentOrderListQueryDto } from '@module/admin/dto/admin-query.dto';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class AdminPaymentService {
  constructor(
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    @InjectRepository(PaymentAfterSalesEntity)
    private readonly afterSalesRepository: Repository<PaymentAfterSalesEntity>,
    private readonly roleAuthzService: RoleAuthzService,
  ) {}

  async findOrders(adminUserId: number, query: AdminPaymentOrderListQueryDto) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);
    const qb = this.paymentOrderRepository
      .createQueryBuilder('order')
      .leftJoinAndSelect('order.user', 'user')
      .leftJoinAndSelect('order.payee', 'payee');

    if (query.status) {
      qb.andWhere('order.status = :status', { status: query.status });
    }

    if (query.bizType) {
      qb.andWhere('order.bizType = :bizType', { bizType: query.bizType });
    }

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere(
        '(order.outTradeNo LIKE :keyword OR order.subject LIKE :keyword OR order.tradeNo LIKE :keyword)',
        { keyword },
      );
    }

    qb.orderBy('order.createdAt', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();

    return createPaginatedResult(
      items.map((order) => this.mapOrder(order)),
      total,
      page,
      limit,
    );
  }

  async findOrder(adminUserId: number, orderId: number) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const order = await this.paymentOrderRepository.findOne({
      where: { id: orderId },
      relations: ['user', 'payee'],
    });
    if (!order) {
      throw new NotFoundException('订单不存在');
    }
    const afterSales = await this.afterSalesRepository.find({
      where: { paymentOrderId: order.id },
      relations: ['user'],
      order: { createdAt: 'DESC' },
    });
    return {
      ...this.mapOrder(order),
      afterSales: afterSales.map((item) => ({
        id: item.id,
        reason: item.reason,
        status: item.status,
        refundAmount: item.refundAmount,
        rejectReason: item.rejectReason,
        processedAt: item.processedAt,
        createdAt: item.createdAt,
      })),
    };
  }

  private mapOrder(order: PaymentOrderEntity) {
    return {
      id: order.id,
      outTradeNo: order.outTradeNo,
      amount: order.amount,
      platformFee: order.platformFee,
      payeeAmount: order.payeeAmount,
      subject: order.subject,
      status: order.status,
      bizType: order.bizType,
      bizId: order.bizId,
      channel: order.channel,
      userId: order.userId,
      payeeId: order.payeeId,
      tradeNo: order.tradeNo,
      paidAt: order.paidAt,
      confirmedAt: order.confirmedAt,
      settledAt: order.settledAt,
      createdAt: order.createdAt,
      user: order.user
        ? {
            id: order.user.id,
            email: order.user.email,
            realName: order.user.realName,
            nickname: order.user.nickname,
          }
        : null,
      payee: order.payee
        ? {
            id: order.payee.id,
            email: order.payee.email,
            realName: order.payee.realName,
            nickname: order.payee.nickname,
          }
        : null,
    };
  }
}
