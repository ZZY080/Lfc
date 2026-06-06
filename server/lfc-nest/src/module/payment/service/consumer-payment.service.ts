import {
  BadRequestException,
  Inject,
  Injectable,
  ServiceUnavailableException,
  forwardRef,
} from '@nestjs/common';
import {
  PaymentChannel,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';
import { AlipayService } from '@integration/alipay/service/alipay.service';
import { ConsumerActivityService } from '@module/activity/service/consumer-activity.service';
import { ConsumerPostService } from '@module/post/service/consumer-post.service';
import { ConsumerPostProductService } from '@module/post/service/consumer-post-product.service';
import { PaymentOrderService } from '@module/payment/service/payment-order.service';
import { UserAlipayService } from '@module/user/service/user-alipay.service';
import { CreatePaymentResultDto } from '@module/payment/dto/payment.dto';
import { PaymentFeeService } from '@module/payment/service/payment-fee.service';
import { PaymentOrderQueryService } from '@module/payment/service/payment-order-query.service';
import { PaymentTransactionQueryService } from '@module/payment/service/payment-transaction-query.service';
import { PaymentReviewService } from '@module/payment/service/payment-review.service';
import { PaymentAfterSalesService } from '@module/payment/service/payment-after-sales.service';
import { PaymentOrderEntity } from '@module/payment/entity/payment-order.entity';
import { PaymentCheckoutService } from '@module/payment/service/payment-checkout.service';
import { PaymentOrderTab } from '@shared/enum/payment.enum';
import {
  ApplyAfterSalesBodySchema,
  CreateOrderReviewBodySchema,
} from '@module/payment/schema/payment-order.schema';
import { ConsumerPromotionService } from '@module/promotion/service/consumer-promotion.service';

@Injectable()
export class ConsumerPaymentService {
  constructor(
    private readonly paymentOrderService: PaymentOrderService,
    private readonly paymentOrderQueryService: PaymentOrderQueryService,
    private readonly paymentTransactionQueryService: PaymentTransactionQueryService,
    private readonly paymentReviewService: PaymentReviewService,
    private readonly paymentAfterSalesService: PaymentAfterSalesService,
    private readonly paymentCheckoutService: PaymentCheckoutService,
    private readonly alipayService: AlipayService,
    private readonly userAlipayService: UserAlipayService,
    private readonly paymentFeeService: PaymentFeeService,
    private readonly consumerActivityService: ConsumerActivityService,
    private readonly consumerPostService: ConsumerPostService,
    private readonly consumerPostProductService: ConsumerPostProductService,
    @Inject(forwardRef(() => ConsumerPromotionService))
    private readonly consumerPromotionService: ConsumerPromotionService,
  ) {}

  async createActivityJoinOrder(
    userId: number,
    activityId: number,
  ): Promise<CreatePaymentResultDto> {
    this.assertAlipayConfigured();
    const channel = PaymentChannel.ALIPAY;

    const activity = await this.consumerActivityService.findOne(activityId);
    const fee = this.consumerActivityService.getActivityFee(activity);
    if (fee <= 0) {
      throw new BadRequestException('该活动无需支付，可直接报名');
    }

    await this.consumerActivityService.assertCanJoin(userId, activityId);
    await this.userAlipayService.assertCanReceive(activity.authorId, '活动发起人');

    const amount = fee.toFixed(2);
    const subject = `活动报名-${activity.title}`.slice(0, 120);

    const order = await this.paymentOrderService.acquireActivityJoinOrder({
      userId,
      activityId,
      payeeId: activity.authorId,
      amount,
      subject,
      channel,
    });

    return this.buildPaymentResult(order);
  }

  async createPostProductOrder(
    userId: number,
    postId: number,
  ): Promise<CreatePaymentResultDto> {
    this.assertAlipayConfigured();
    const channel = PaymentChannel.ALIPAY;

    const post = await this.consumerPostService.findOne(postId, userId);
    const product = await this.consumerPostProductService.assertCanPurchase(
      userId,
      postId,
      post.authorId,
    );
    const amount = this.consumerPostProductService.getProductPrice(product).toFixed(2);
    await this.userAlipayService.assertCanReceive(post.authorId, '卖家');

    const subject = `闲置转卖-${post.title}`.slice(0, 120);

    const order = await this.paymentOrderService.acquirePostProductOrder({
      userId,
      postId,
      payeeId: post.authorId,
      amount,
      subject,
      channel,
    });

    return this.buildPaymentResult(order);
  }

  /** 笔记擦亮增值服支付（直收入商户，不分账） */
  async createPostBoostOrder(
    userId: number,
    postId: number,
    bidAmount?: string,
  ): Promise<CreatePaymentResultDto> {
    const result = await this.consumerPromotionService.createPostBoostOrder(
      userId,
      postId,
      bidAmount,
    );
    return result.payment;
  }

  /** 活动推广增值服支付（直收入商户，不分账） */
  async createActivityPromoteOrder(
    userId: number,
    activityId: number,
    bidAmount?: string,
  ): Promise<CreatePaymentResultDto> {
    const result = await this.consumerPromotionService.createActivityPromoteOrder(
      userId,
      activityId,
      bidAmount,
    );
    return result.payment;
  }

  findOrderForUser(userId: number, outTradeNo: string) {
    return this.paymentOrderService.findOrderForUser(userId, outTradeNo);
  }

  /** 主动向支付宝查单并同步本地订单（notify 延迟/不可达时补偿） */
  async syncOrderFromAlipay(userId: number, outTradeNo: string) {
    const order = await this.paymentOrderService.findOrderForUser(
      userId,
      outTradeNo,
    );
    if (order.status !== PaymentOrderStatus.PENDING) {
      return order;
    }
    if (!this.alipayService.isConfigured()) {
      return order;
    }

    const trade = await this.alipayService.queryAppPayTrade(outTradeNo);
    const paidStatus =
      trade.tradeStatus === 'TRADE_SUCCESS' ||
      trade.tradeStatus === 'TRADE_FINISHED';
    if (!paidStatus || !trade.totalAmount) {
      return order;
    }

    await this.paymentOrderService.completePaidOrder({
      outTradeNo,
      channel: PaymentChannel.ALIPAY,
      channelTradeNo: trade.tradeNo,
      amount: trade.totalAmount,
      idempotencyKey: `alipay:sync:${outTradeNo}`,
    });

    return this.paymentOrderService.findOrderForUser(userId, outTradeNo);
  }

  findPostProductOrder(userId: number, postId: number) {
    return this.paymentOrderService.findProductOrderForBuyer(userId, postId);
  }

  confirmReceipt(userId: number, outTradeNo: string) {
    return this.paymentOrderService.confirmReceipt(userId, outTradeNo);
  }

  listOrders(
    userId: number,
    tab: PaymentOrderTab,
    page?: number,
    limit?: number,
  ) {
    return this.paymentOrderQueryService.listForBuyer(
      userId,
      tab,
      page,
      limit,
    );
  }

  getOrderTabCounts(userId: number) {
    return this.paymentOrderQueryService.getTabCounts(userId);
  }

  cancelOrder(userId: number, outTradeNo: string) {
    return this.paymentOrderService.cancelPendingOrder(userId, outTradeNo);
  }

  async repayOrder(userId: number, outTradeNo: string) {
    const order = await this.paymentOrderService.repayPendingOrder(
      userId,
      outTradeNo,
    );
    return this.buildPaymentResult(order);
  }

  createReview(
    userId: number,
    outTradeNo: string,
    body: CreateOrderReviewBodySchema,
  ) {
    return this.paymentReviewService.createReview(userId, outTradeNo, body);
  }

  applyAfterSales(
    userId: number,
    outTradeNo: string,
    body: ApplyAfterSalesBodySchema,
  ) {
    return this.paymentAfterSalesService.apply(userId, outTradeNo, body);
  }

  cancelAfterSales(userId: number, outTradeNo: string) {
    return this.paymentAfterSalesService.cancel(userId, outTradeNo);
  }

  getPaymentConfig() {
    return this.paymentFeeService.getPublicConfig();
  }

  listTransactions(userId: number, page?: number, limit?: number) {
    return this.paymentTransactionQueryService.listForUser(
      userId,
      page,
      limit,
    );
  }

  private buildPaymentResult(order: PaymentOrderEntity): CreatePaymentResultDto {
    return this.paymentCheckoutService.buildPaymentResultForOrder(order);
  }

  private assertAlipayConfigured() {
    if (!this.alipayService.isConfigured()) {
      throw new ServiceUnavailableException('支付宝支付未配置');
    }
  }
}
