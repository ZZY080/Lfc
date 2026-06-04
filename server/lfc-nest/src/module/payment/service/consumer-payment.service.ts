import {
  BadRequestException,
  Injectable,
  ServiceUnavailableException,
} from '@nestjs/common';
import {
  PaymentBizType,
  PaymentChannel,
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
import { PaymentReviewService } from '@module/payment/service/payment-review.service';
import { PaymentAfterSalesService } from '@module/payment/service/payment-after-sales.service';
import { PaymentOrderTab } from '@shared/enum/payment.enum';
import {
  ApplyAfterSalesBodySchema,
  CreateOrderReviewBodySchema,
} from '@module/payment/schema/payment-order.schema';

@Injectable()
export class ConsumerPaymentService {
  constructor(
    private readonly paymentOrderService: PaymentOrderService,
    private readonly paymentOrderQueryService: PaymentOrderQueryService,
    private readonly paymentReviewService: PaymentReviewService,
    private readonly paymentAfterSalesService: PaymentAfterSalesService,
    private readonly alipayService: AlipayService,
    private readonly userAlipayService: UserAlipayService,
    private readonly paymentFeeService: PaymentFeeService,
    private readonly consumerActivityService: ConsumerActivityService,
    private readonly consumerPostService: ConsumerPostService,
    private readonly consumerPostProductService: ConsumerPostProductService,
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

    const pendingOrder = await this.paymentOrderService.findPendingOrder({
      userId,
      bizType: PaymentBizType.ACTIVITY_JOIN,
      bizId: activityId,
      channel,
    });
    if (pendingOrder) {
      return this.buildPaymentResult(pendingOrder);
    }

    const amount = fee.toFixed(2);
    const outTradeNo = this.paymentOrderService.generateOutTradeNo(userId);
    const subject = `活动报名-${activity.title}`.slice(0, 120);

    const order = await this.paymentOrderService.createPendingOrder({
      outTradeNo,
      userId,
      payeeId: activity.authorId,
      bizType: PaymentBizType.ACTIVITY_JOIN,
      bizId: activityId,
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

    const pendingOrder = await this.paymentOrderService.findPendingOrder({
      userId,
      bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
      bizId: postId,
      channel,
    });
    if (pendingOrder) {
      return this.buildPaymentResult(pendingOrder);
    }

    const outTradeNo = this.paymentOrderService.generateOutTradeNo(userId);
    const subject = `闲置转卖-${post.title}`.slice(0, 120);

    const order = await this.paymentOrderService.createPendingOrder({
      outTradeNo,
      userId,
      payeeId: post.authorId,
      bizType: PaymentBizType.POST_PRODUCT_PURCHASE,
      bizId: postId,
      amount,
      subject,
      channel,
    });

    return this.buildPaymentResult(order);
  }

  findOrderForUser(userId: number, outTradeNo: string) {
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

  private buildPaymentResult(
    order: {
      outTradeNo: string;
      amount: string;
      platformFee?: string;
      payeeAmount?: string;
      subject: string;
      status: string;
      payeeId: number;
      bizType?: PaymentBizType;
    },
  ): CreatePaymentResultDto {
    const settlement = this.paymentFeeService.calculateSettlement(order.amount);
    const platformFee = order.platformFee || settlement.platformFee;
    const payeeAmount = order.payeeAmount || settlement.payeeAmount;
    const config = this.paymentFeeService.getPublicConfig();
    const base = {
      outTradeNo: order.outTradeNo,
      channel: PaymentChannel.ALIPAY,
      amount: order.amount,
      platformFee,
      payeeAmount,
      platformFeeRateLabel: config.platformFeeRateLabel,
      subject: order.subject,
      status: order.status,
      payeeId: order.payeeId,
    };

    const { orderStr } = this.alipayService.createAppPayOrder({
      outTradeNo: order.outTradeNo,
      totalAmount: order.amount,
      subject: order.subject,
      enableRoyalty:
        order.bizType === PaymentBizType.POST_PRODUCT_PURCHASE ||
        order.bizType === PaymentBizType.ACTIVITY_JOIN,
    });
    return { ...base, alipay: { orderStr } };
  }

  private assertAlipayConfigured() {
    if (!this.alipayService.isConfigured()) {
      throw new ServiceUnavailableException('支付宝支付未配置');
    }
  }
}
