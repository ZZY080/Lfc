import {
  PaymentBizType,
  PaymentOrderStatus,
} from '@shared/enum/payment.enum';

/** 非终态订单唯一键：同一用户对同一业务仅允许一笔有效订单 */
export function buildPaymentActiveKey(
  bizType: PaymentBizType,
  bizId: number,
  userId: number,
): string {
  return `${bizType}:${bizId}:${userId}`;
}

export function isTerminalPaymentStatus(status: PaymentOrderStatus): boolean {
  return (
    status === PaymentOrderStatus.CLOSED ||
    status === PaymentOrderStatus.REFUNDED ||
    status === PaymentOrderStatus.FAILED
  );
}

export const PAYMENT_PENDING_TTL_MS = 30 * 60 * 1000;

export const PAYMENT_PRODUCT_LOCK_TTL_SECONDS = 20 * 60;

export const PAYMENT_CREATE_LOCK_TTL_SECONDS = 15;

export const PAYMENT_NOTIFY_PROCESS_LOCK_TTL_SECONDS = 60;

/** 增值服（擦亮/推广）：用户付给平台商户号，全额留商户账户，不走 C2C 分账 */
export function isPlatformDirectRevenueBizType(bizType: PaymentBizType): boolean {
  return (
    bizType === PaymentBizType.POST_BOOST ||
    bizType === PaymentBizType.ACTIVITY_PROMOTE
  );
}

/** C2C 下单是否开启支付宝分账（闲置购买 / 活动报名） */
export function shouldEnableAlipayRoyalty(bizType: PaymentBizType): boolean {
  return (
    !isPlatformDirectRevenueBizType(bizType) &&
    (bizType === PaymentBizType.POST_PRODUCT_PURCHASE ||
      bizType === PaymentBizType.ACTIVITY_JOIN)
  );
}
