export enum PaymentOrderStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  /** 买家已确认收货，待分账 */
  CONFIRMED = 'CONFIRMED',
  /** 分账完成 */
  SETTLED = 'SETTLED',
  /** 已退款 */
  REFUNDED = 'REFUNDED',
  CLOSED = 'CLOSED',
  FAILED = 'FAILED',
}

export enum PaymentOrderTab {
  ALL = 'all',
  PENDING_PAYMENT = 'pending_payment',
  AWAITING_RECEIPT = 'awaiting_receipt',
  REVIEW = 'review',
  AFTER_SALES = 'after_sales',
}

export enum PaymentAfterSalesStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  REFUNDING = 'REFUNDING',
  REFUNDED = 'REFUNDED',
  CANCELLED = 'CANCELLED',
}

export enum PaymentBizType {
  ACTIVITY_JOIN = 'ACTIVITY_JOIN',
  POST_PRODUCT_PURCHASE = 'POST_PRODUCT_PURCHASE',
  POST_BOOST = 'POST_BOOST',
  ACTIVITY_PROMOTE = 'ACTIVITY_PROMOTE',
}

export enum PaymentTransactionType {
  PAYMENT = 'PAYMENT',
  REFUND = 'REFUND',
}

export enum PaymentChannel {
  ALIPAY = 'ALIPAY',
  WECHAT = 'WECHAT',
}
