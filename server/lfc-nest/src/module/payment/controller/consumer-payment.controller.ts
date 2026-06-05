import {
  Body,
  Controller,
  Delete,
  Get,
  NotFoundException,
  Param,
  ParseIntPipe,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ConsumerPaymentService } from '@module/payment/service/consumer-payment.service';
import { JwtAuthGuard } from '@shared/guard/jwt-auth.guard';
import { CurrentUser } from '@shared/decorator/user.decorator';
import {
  ApplyAfterSalesBodySchema,
  CreateOrderReviewBodySchema,
  PaymentOrderListQuerySchema,
  PaymentTransactionListQuerySchema,
} from '@module/payment/schema/payment-order.schema';
import { PaymentOrderTab } from '@shared/enum/payment.enum';

@Controller('consumer/payment')
export class ConsumerPaymentController {
  constructor(
    private readonly consumerPaymentService: ConsumerPaymentService,
  ) {}

  /** 获取 C2C 支付/平台抽成配置 */
  @Get('config')
  getPaymentConfig() {
    return this.consumerPaymentService.getPaymentConfig();
  }

  /** 买家订单列表（全部/待付款/待收货/评价/售后） */
  @Get('orders')
  @UseGuards(JwtAuthGuard)
  listOrders(
    @CurrentUser('userId') userId: number,
    @Query() query: PaymentOrderListQuerySchema,
  ) {
    return this.consumerPaymentService.listOrders(
      userId,
      query.tab ?? PaymentOrderTab.ALL,
      query.page,
      query.limit,
    );
  }

  /** 各订单 Tab 数量 */
  @Get('orders/counts')
  @UseGuards(JwtAuthGuard)
  getOrderTabCounts(@CurrentUser('userId') userId: number) {
    return this.consumerPaymentService.getOrderTabCounts(userId);
  }

  /** 支付/退款流水（买家视角） */
  @Get('transactions')
  @UseGuards(JwtAuthGuard)
  listTransactions(
    @CurrentUser('userId') userId: number,
    @Query() query: PaymentTransactionListQuerySchema,
  ) {
    return this.consumerPaymentService.listTransactions(
      userId,
      query.page,
      query.limit,
    );
  }

  /** 创建活动报名 C2C 支付订单（支付给活动发起人） */
  @Post('activity/:activityId/order')
  @UseGuards(JwtAuthGuard)
  createActivityJoinOrder(
    @CurrentUser('userId') userId: number,
    @Param('activityId', ParseIntPipe) activityId: number,
  ) {
    return this.consumerPaymentService.createActivityJoinOrder(
      userId,
      activityId,
    );
  }

  /** 创建闲置商品 C2C 支付订单（支付给卖家） */
  @Post('post/:postId/order')
  @UseGuards(JwtAuthGuard)
  createPostProductOrder(
    @CurrentUser('userId') userId: number,
    @Param('postId', ParseIntPipe) postId: number,
  ) {
    return this.consumerPaymentService.createPostProductOrder(
      userId,
      postId,
    );
  }

  /** 查询当前用户对某商品的支付订单（确认收货用） */
  @Get('post/:postId/order')
  @UseGuards(JwtAuthGuard)
  async findPostProductOrder(
    @CurrentUser('userId') userId: number,
    @Param('postId', ParseIntPipe) postId: number,
  ) {
    const order = await this.consumerPaymentService.findPostProductOrder(
      userId,
      postId,
    );
    if (!order) {
      throw new NotFoundException('暂无支付订单');
    }
    return order;
  }

  @Get('orders/:outTradeNo')
  @UseGuards(JwtAuthGuard)
  findOrder(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
  ) {
    return this.consumerPaymentService.findOrderForUser(userId, outTradeNo);
  }

  /** 取消待付款订单 */
  @Post('orders/:outTradeNo/cancel')
  @UseGuards(JwtAuthGuard)
  cancelOrder(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
  ) {
    return this.consumerPaymentService.cancelOrder(userId, outTradeNo);
  }

  /** 继续支付待付款订单 */
  @Post('orders/:outTradeNo/repay')
  @UseGuards(JwtAuthGuard)
  repayOrder(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
  ) {
    return this.consumerPaymentService.repayOrder(userId, outTradeNo);
  }

  /** 买家确认收货，触发商家分账 */
  @Post('orders/:outTradeNo/confirm-receipt')
  @UseGuards(JwtAuthGuard)
  confirmReceipt(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
  ) {
    return this.consumerPaymentService.confirmReceipt(userId, outTradeNo);
  }

  /** 提交订单评价 */
  @Post('orders/:outTradeNo/reviews')
  @UseGuards(JwtAuthGuard)
  createReview(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
    @Body() body: CreateOrderReviewBodySchema,
  ) {
    return this.consumerPaymentService.createReview(userId, outTradeNo, body);
  }

  /** 申请售后/退款 */
  @Post('orders/:outTradeNo/after-sales')
  @UseGuards(JwtAuthGuard)
  applyAfterSales(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
    @Body() body: ApplyAfterSalesBodySchema,
  ) {
    return this.consumerPaymentService.applyAfterSales(
      userId,
      outTradeNo,
      body,
    );
  }

  /** 取消售后申请 */
  @Delete('orders/:outTradeNo/after-sales')
  @UseGuards(JwtAuthGuard)
  cancelAfterSales(
    @CurrentUser('userId') userId: number,
    @Param('outTradeNo') outTradeNo: string,
  ) {
    return this.consumerPaymentService.cancelAfterSales(userId, outTradeNo);
  }
}
