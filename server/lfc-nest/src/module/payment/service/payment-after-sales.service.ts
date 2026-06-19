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
import { UserEntity } from '@module/user/entity/user.entity';
import {
  PaymentAfterSalesStatus,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { ApplyAfterSalesBodySchema } from '@module/payment/schema/payment-order.schema';
import { PaymentRefundService } from '@module/payment/service/payment-refund.service';
import { RoleAuthzService } from '@shared/auth/role-authz.service';
import { UserRole } from '@shared/enum/user-role.enum';
import { NotificationService } from '@module/message/service/notification.service';
import {
  createPaginatedResult,
  normalizePagination,
} from '@shared/dto/paginated-result.dto';

@Injectable()
export class PaymentAfterSalesService {
  constructor(
    @InjectRepository(PaymentAfterSalesEntity)
    private readonly afterSalesRepository: Repository<PaymentAfterSalesEntity>,
    @InjectRepository(PaymentOrderEntity)
    private readonly paymentOrderRepository: Repository<PaymentOrderEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    private readonly paymentRefundService: PaymentRefundService,
    private readonly roleAuthzService: RoleAuthzService,
    private readonly notificationService: NotificationService,
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

    await this.notificationService.sendAfterSalesSubmitted(
      userId,
      order.subject,
      order.outTradeNo,
      order.id,
    );
    await this.notifyAdminsPendingAfterSales(order);

    return this.toDto(afterSales);
  }

  async findAllForAdmin(
    adminUserId: number,
    query: {
      page?: number;
      limit?: number;
      status?: PaymentAfterSalesStatus;
      keyword?: string;
    },
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const { page, limit, skip } = normalizePagination(query.page, query.limit);
    const qb = this.afterSalesRepository
      .createQueryBuilder('afterSales')
      .leftJoinAndSelect('afterSales.user', 'user')
      .leftJoinAndSelect('afterSales.paymentOrder', 'paymentOrder')
      .leftJoinAndSelect('paymentOrder.payee', 'payee');

    if (query.status) {
      qb.andWhere('afterSales.status = :status', { status: query.status });
    }

    if (query.keyword?.trim()) {
      const keyword = `%${query.keyword.trim()}%`;
      qb.andWhere(
        '(afterSales.reason LIKE :keyword OR paymentOrder.outTradeNo LIKE :keyword OR paymentOrder.subject LIKE :keyword)',
        { keyword },
      );
    }

    qb.orderBy('afterSales.createdAt', 'DESC').skip(skip).take(limit);
    const [items, total] = await qb.getManyAndCount();
    return createPaginatedResult(
      items.map((item) => this.toAdminDto(item)),
      total,
      page,
      limit,
    );
  }

  async reviewByAdmin(
    adminUserId: number,
    afterSalesId: number,
    body: { action: 'approve' | 'reject'; rejectReason?: string },
  ) {
    await this.roleAuthzService.assertRole(adminUserId, UserRole.ADMIN);
    const afterSales = await this.afterSalesRepository.findOne({
      where: { id: afterSalesId },
      relations: ['paymentOrder', 'user'],
    });
    if (!afterSales) {
      throw new NotFoundException('售后申请不存在');
    }
    if (afterSales.status !== PaymentAfterSalesStatus.PENDING) {
      throw new BadRequestException('当前售后申请不可审核');
    }

    if (body.action === 'reject') {
      const rejectReason = body.rejectReason?.trim();
      if (!rejectReason) {
        throw new BadRequestException('拒绝时需填写审核意见');
      }
      afterSales.status = PaymentAfterSalesStatus.REJECTED;
      afterSales.rejectReason = rejectReason;
      afterSales.processedAt = new Date();
      await this.afterSalesRepository.save(afterSales);
      await this.notificationService.sendAfterSalesRejected(
        afterSales.userId,
        afterSales.paymentOrder?.subject ?? '订单',
        afterSales.paymentOrderId,
        rejectReason,
      );
      return this.toAdminDto(afterSales);
    }

    const order = afterSales.paymentOrder;
    if (!order) {
      throw new NotFoundException('关联订单不存在');
    }
    await this.processRefund(order, afterSales);
    const refreshed = await this.afterSalesRepository.findOne({
      where: { id: afterSalesId },
      relations: ['paymentOrder', 'user', 'paymentOrder.payee'],
    });
    if (refreshed?.status === PaymentAfterSalesStatus.REFUNDED) {
      await this.notificationService.sendAfterSalesApproved(
        refreshed.userId,
        order.subject,
        order.id,
      );
    }
    return this.toAdminDto(refreshed!);
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

  private async notifyAdminsPendingAfterSales(order: PaymentOrderEntity) {
    const admins = await this.userRepository.find({
      where: { role: UserRole.ADMIN },
      select: ['id'],
    });
    await Promise.all(
      admins.map((admin) =>
        this.notificationService.sendAfterSalesPendingToAdmin(
          admin.id,
          order.subject,
          order.outTradeNo,
          order.id,
        ),
      ),
    );
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

  private toAdminDto(afterSales: PaymentAfterSalesEntity) {
    const order = afterSales.paymentOrder;
    return {
      id: afterSales.id,
      paymentOrderId: afterSales.paymentOrderId,
      userId: afterSales.userId,
      reason: afterSales.reason,
      status: afterSales.status,
      refundAmount: afterSales.refundAmount,
      processedAt: afterSales.processedAt,
      rejectReason: afterSales.rejectReason,
      createdAt: afterSales.createdAt,
      user: afterSales.user
        ? {
            id: afterSales.user.id,
            email: afterSales.user.email,
            realName: afterSales.user.realName,
            nickname: afterSales.user.nickname,
          }
        : null,
      order: order
        ? {
            id: order.id,
            outTradeNo: order.outTradeNo,
            subject: order.subject,
            amount: order.amount,
            status: order.status,
            bizType: order.bizType,
            payee: order.payee
              ? {
                  id: order.payee.id,
                  email: order.payee.email,
                  realName: order.payee.realName,
                  nickname: order.payee.nickname,
                }
              : null,
          }
        : null,
    };
  }
}
