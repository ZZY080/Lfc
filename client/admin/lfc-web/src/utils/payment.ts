import type {
  PaymentBizType,
  PaymentOrderStatus,
} from '../types'

const PAYMENT_STATUS_LABELS: Record<PaymentOrderStatus, string> = {
  PENDING: '待支付',
  PAID: '已支付',
  CONFIRMED: '待分账',
  SETTLED: '已分账',
  REFUNDED: '已退款',
  CLOSED: '已关闭',
  FAILED: '支付失败',
}

const PAYMENT_BIZ_LABELS: Record<PaymentBizType, string> = {
  ACTIVITY_JOIN: '活动报名',
  POST_PRODUCT_PURCHASE: '闲置购买',
  POST_BOOST: '帖子擦亮',
  ACTIVITY_PROMOTE: '活动推广',
}

export function formatPaymentStatus(status: PaymentOrderStatus) {
  return PAYMENT_STATUS_LABELS[status] ?? status
}

export function formatPaymentBizType(bizType: PaymentBizType) {
  return PAYMENT_BIZ_LABELS[bizType] ?? bizType
}

export function formatUserBrief(
  user: { realName: string; nickname: string | null; email: string } | null,
) {
  if (!user) return '—'
  return user.realName || user.nickname || user.email
}
