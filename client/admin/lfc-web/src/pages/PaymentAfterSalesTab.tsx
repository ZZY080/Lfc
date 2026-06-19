import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { fetchAfterSales, reviewAfterSales } from '../api/payment'
import { ApiError } from '../api/client'
import { Pagination } from '../components/Pagination'
import { RejectReviewModal } from '../components/RejectReviewModal'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { PaymentAfterSales, PaymentAfterSalesStatus } from '../types'
import { formatDateTime, formatFee } from '../utils/format'
import { formatPaymentBizType, formatUserBrief } from '../utils/payment'

type StatusFilter = PaymentAfterSalesStatus | 'all'

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'PENDING', label: '待审核' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'CANCELLED', label: '已取消' },
]

const STATUS_LABELS: Record<PaymentAfterSalesStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  CANCELLED: '已取消',
}

const STATUS_PILL: Record<PaymentAfterSalesStatus, string> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  REFUNDING: 'info',
  REFUNDED: 'success',
  CANCELLED: 'muted',
}

interface PaymentAfterSalesTabProps {
  initialStatus?: StatusFilter
}

export function PaymentAfterSalesTab({
  initialStatus = 'PENDING',
}: PaymentAfterSalesTabProps) {
  const { token } = useAuth()
  const toast = useToast()
  const [items, setItems] = useState<PaymentAfterSales[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [limit] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>(
    STATUS_TABS.some((item) => item.value === initialStatus)
      ? initialStatus
      : 'PENDING',
  )
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actingId, setActingId] = useState<number | null>(null)
  const [rejectTarget, setRejectTarget] = useState<PaymentAfterSales | null>(null)

  const loadItems = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchAfterSales(token, {
        page,
        limit,
        keyword: keyword || undefined,
        status: statusFilter === 'all' ? undefined : statusFilter,
      })
      setItems(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载售后列表失败')
    } finally {
      setLoading(false)
    }
  }, [token, page, limit, keyword, statusFilter])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadItems()
  }, [loadItems])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(1)
    setKeyword(searchInput.trim())
  }

  async function handleApprove(item: PaymentAfterSales) {
    if (!token) return
    if (!window.confirm(`确定同意订单 ${item.order?.outTradeNo ?? ''} 的售后退款吗？`)) {
      return
    }
    setActingId(item.id)
    try {
      await reviewAfterSales(token, item.id, { action: 'approve' })
      setItems((prev) =>
        statusFilter === 'PENDING'
          ? prev.filter((row) => row.id !== item.id)
          : prev.map((row) =>
              row.id === item.id ? { ...row, status: 'REFUNDING' } : row,
            ),
      )
      if (statusFilter === 'PENDING') {
        setTotal((t) => Math.max(0, t - 1))
      }
      toast.success('已同意退款')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '操作失败')
    } finally {
      setActingId(null)
    }
  }

  async function handleRejectConfirm(rejectReason: string) {
    if (!token || !rejectTarget) return
    setActingId(rejectTarget.id)
    try {
      await reviewAfterSales(token, rejectTarget.id, {
        action: 'reject',
        rejectReason,
      })
      setItems((prev) =>
        statusFilter === 'PENDING'
          ? prev.filter((row) => row.id !== rejectTarget.id)
          : prev.map((row) =>
              row.id === rejectTarget.id
                ? { ...row, status: 'REJECTED', rejectReason }
                : row,
            ),
      )
      if (statusFilter === 'PENDING') {
        setTotal((t) => Math.max(0, t - 1))
      }
      toast.success('已拒绝售后申请')
      setRejectTarget(null)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '操作失败')
    } finally {
      setActingId(null)
    }
  }

  return (
    <>
      <div className="toolbar">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="搜索订单号、原因…"
          />
          <button type="submit" className="btn btn-primary btn-sm">
            搜索
          </button>
        </form>

        <div className="filter-tabs">
          {STATUS_TABS.map((item) => (
            <button
              key={item.value}
              type="button"
              className={`filter-tab${statusFilter === item.value ? ' active' : ''}`}
              onClick={() => {
                setPage(1)
                setStatusFilter(item.value)
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {loading && <div className="page-state">加载中…</div>}
      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && (
        <div className="manage-list">
          {items.length === 0 ? (
            <div className="page-state empty-state">
              <p>暂无售后申请</p>
            </div>
          ) : (
            items.map((item) => (
              <article key={item.id} className="manage-card">
                <div className="manage-card-header">
                  <div>
                    <h3 className="manage-card-title">
                      {item.order?.subject ?? '关联订单'}
                    </h3>
                    <p className="manage-meta">
                      订单号 {item.order?.outTradeNo ?? '-'} ·{' '}
                      {formatPaymentBizType(
                        item.order?.bizType ?? 'POST_PRODUCT_PURCHASE',
                      )}{' '}
                      · {formatDateTime(item.createdAt)}
                    </p>
                  </div>
                  <span className={`status-pill ${STATUS_PILL[item.status]}`}>
                    {STATUS_LABELS[item.status]}
                  </span>
                </div>

                <dl className="manage-details">
                  <div>
                    <dt>申请人</dt>
                    <dd>{formatUserBrief(item.user)}</dd>
                  </div>
                  <div>
                    <dt>退款金额</dt>
                    <dd>{formatFee(item.refundAmount)}</dd>
                  </div>
                  <div>
                    <dt>申请原因</dt>
                    <dd>{item.reason}</dd>
                  </div>
                </dl>

                {item.rejectReason && (
                  <div className="review-comment">
                    <strong>拒绝理由</strong>
                    <p>{item.rejectReason}</p>
                  </div>
                )}

                {item.status === 'PENDING' && (
                  <div className="manage-actions">
                    <button
                      type="button"
                      className="btn btn-success"
                      disabled={actingId === item.id}
                      onClick={() => void handleApprove(item)}
                    >
                      同意退款
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      disabled={actingId === item.id}
                      onClick={() => setRejectTarget(item)}
                    >
                      拒绝
                    </button>
                  </div>
                )}
              </article>
            ))
          )}

          {items.length > 0 && (
            <Pagination
              page={page}
              total={total}
              limit={limit}
              hasMore={hasMore}
              onPageChange={setPage}
            />
          )}
        </div>
      )}

      <RejectReviewModal
        open={rejectTarget !== null}
        title={rejectTarget?.order?.subject ?? '售后申请'}
        onClose={() => setRejectTarget(null)}
        onConfirm={handleRejectConfirm}
      />
    </>
  )
}
