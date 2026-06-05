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
