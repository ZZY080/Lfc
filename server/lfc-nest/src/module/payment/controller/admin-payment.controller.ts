import { Controller, Get, Param, ParseIntPipe, Patch, Body, Query, UseGuards } from '@nestjs/common';
import { AdminPaymentService } from '@module/payment/service/admin-payment.service';
import { PaymentAfterSalesService } from '@module/payment/service/payment-after-sales.service';
import { AdminPaymentOrderListQuerySchema } from '@module/payment/schema/admin-payment.schema';
import {
  AdminAfterSalesListQuerySchema,
  AdminReviewAfterSalesBodySchema,
} from '@module/payment/schema/admin-after-sales.schema';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';

@Controller('admin/payment')
@UseGuards(JwtAuthGuard)
export class AdminPaymentController {
  constructor(
    private readonly adminPaymentService: AdminPaymentService,
    private readonly paymentAfterSalesService: PaymentAfterSalesService,
  ) {}

  @Get('order')
  findOrders(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminPaymentOrderListQuerySchema,
  ) {
    return this.adminPaymentService.findOrders(userId, query);
  }

  @Get('order/:id')
  findOrder(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
  ) {
    return this.adminPaymentService.findOrder(userId, id);
  }

  @Get('after-sales')
  findAfterSales(
    @CurrentUser('userId') userId: number,
    @Query() query: AdminAfterSalesListQuerySchema,
  ) {
    return this.paymentAfterSalesService.findAllForAdmin(userId, query);
  }

  @Patch('after-sales/:id/review')
  reviewAfterSales(
    @CurrentUser('userId') userId: number,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: AdminReviewAfterSalesBodySchema,
  ) {
    return this.paymentAfterSalesService.reviewByAdmin(userId, id, body);
  }
}
