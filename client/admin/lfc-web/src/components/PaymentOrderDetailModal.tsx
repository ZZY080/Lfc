import { useEffect, useState, type ReactNode } from 'react'
import { fetchPaymentOrder } from '../api/payment'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { PaymentOrder, PaymentOrderDetail } from '../types'
import { formatDateTime, formatFee } from '../utils/format'
import {
  formatPaymentBizType,
  formatPaymentStatus,
  formatUserBrief,
} from '../utils/payment'
import './CreateUserModal.css'
import './PaymentOrderDetailModal.css'

const AFTER_SALES_STATUS_LABELS: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  CANCELLED: '已取消',
}

interface PaymentOrderDetailModalProps {
  order: PaymentOrder | null
  onClose: () => void
}

export function PaymentOrderDetailModal({
  order,
  onClose,
}: PaymentOrderDetailModalProps) {
  const { token } = useAuth()
  const [detail, setDetail] = useState<PaymentOrderDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!order || !token) {
      setDetail(null)
      setError('')
      return
    }

    let cancelled = false
    setLoading(true)
    setError('')
    void fetchPaymentOrder(token, order.id)
      .then((data) => {
        if (!cancelled) setDetail(data)
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : '加载订单详情失败')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [order, token])

  if (!order) return null

  const data = detail ?? order

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card modal-card-wide order-detail-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="order-detail-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <div>
            <h2 id="order-detail-title">订单详情</h2>
            <p className="order-detail-subtitle">{order.outTradeNo}</p>
          </div>
          <button type="button" className="modal-close" onClick={onClose}>
            ×
          </button>
        </header>

        <div className="order-detail-body">
          {loading && <div className="page-state">加载详情…</div>}
          {!loading && error && <div className="alert alert-error">{error}</div>}

          {!loading && !error && (
            <>
              <div className="order-detail-grid">
                <DetailItem label="订单标题" value={data.subject} wide />
                <DetailItem label="业务类型" value={formatPaymentBizType(data.bizType)} />
                <DetailItem label="业务 ID" value={String(data.bizId)} />
                <DetailItem
                  label="订单状态"
                  value={
                    <span className={`status-pill order-status status-${data.status.toLowerCase()}`}>
                      {formatPaymentStatus(data.status)}
                    </span>
                  }
                />
                <DetailItem label="支付渠道" value={data.channel === 'ALIPAY' ? '支付宝' : '微信'} />
                <DetailItem label="订单金额" value={formatFee(data.amount)} highlight />
                <DetailItem label="平台手续费" value={formatFee(data.platformFee)} />
                <DetailItem label="收款方实收" value={formatFee(data.payeeAmount)} />
                <DetailItem label="买家" value={formatUserBrief(data.user)} />
                <DetailItem label="收款方" value={formatUserBrief(data.payee)} />
                <DetailItem label="第三方流水号" value={data.tradeNo ?? '—'} mono />
                <DetailItem label="创建时间" value={formatDateTime(data.createdAt)} />
                <DetailItem
                  label="支付时间"
                  value={data.paidAt ? formatDateTime(data.paidAt) : '—'}
                />
                <DetailItem
                  label="确认收货"
                  value={data.confirmedAt ? formatDateTime(data.confirmedAt) : '—'}
                />
                <DetailItem
                  label="分账时间"
                  value={data.settledAt ? formatDateTime(data.settledAt) : '—'}
                />
              </div>

              {detail?.afterSales && detail.afterSales.length > 0 && (
                <section className="order-after-sales">
                  <h3>售后记录</h3>
                  <div className="order-after-sales-list">
                    {detail.afterSales.map((item) => (
                      <div key={item.id} className="order-after-sales-item">
                        <div className="order-after-sales-head">
                          <span className={`status-pill status-${item.status.toLowerCase()}`}>
                            {AFTER_SALES_STATUS_LABELS[item.status] ?? item.status}
                          </span>
                          <span>{formatFee(item.refundAmount)}</span>
                          <span className="order-after-sales-time">
                            {formatDateTime(item.createdAt)}
                          </span>
                        </div>
                        <p>{item.reason}</p>
                        {item.rejectReason && (
                          <p className="order-after-sales-reject">拒绝原因：{item.rejectReason}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </section>
              )}
            </>
          )}
        </div>

        <div className="order-detail-footer">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            关闭
          </button>
        </div>
      </div>
    </div>
  )
}

function DetailItem({
  label,
  value,
  wide,
  mono,
  highlight,
}: {
  label: string
  value: ReactNode
  wide?: boolean
  mono?: boolean
  highlight?: boolean
}) {
  return (
    <div className={`order-detail-item${wide ? ' order-detail-item-wide' : ''}`}>
      <span className="order-detail-label">{label}</span>
      <span
        className={`order-detail-value${mono ? ' cell-mono' : ''}${highlight ? ' order-detail-highlight' : ''}`}
      >
        {value}
      </span>
    </div>
  )
}
