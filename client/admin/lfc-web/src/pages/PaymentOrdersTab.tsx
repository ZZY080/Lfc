import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { fetchPaymentOrders } from '../api/payment'
import { ApiError } from '../api/client'
import { Pagination } from '../components/Pagination'
import { useAuth } from '../hooks/useAuth'
import type { PaymentBizType, PaymentOrderStatus } from '../types'
import { formatDateTime, formatFee } from '../utils/format'
import {
  formatPaymentBizType,
  formatPaymentStatus,
  formatUserBrief,
} from '../utils/payment'

const STATUS_OPTIONS: { value: PaymentOrderStatus | ''; label: string }[] = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'CONFIRMED', label: '待分账' },
  { value: 'SETTLED', label: '已分账' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'FAILED', label: '支付失败' },
]

const BIZ_OPTIONS: { value: PaymentBizType | ''; label: string }[] = [
  { value: '', label: '全部类型' },
  { value: 'ACTIVITY_JOIN', label: '活动报名' },
  { value: 'POST_PRODUCT_PURCHASE', label: '闲置购买' },
  { value: 'POST_BOOST', label: '帖子擦亮' },
  { value: 'ACTIVITY_PROMOTE', label: '活动推广' },
]

export function PaymentOrdersTab() {
  const { token } = useAuth()
  const [orders, setOrders] = useState<Awaited<
    ReturnType<typeof fetchPaymentOrders>
  >['items']>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [limit] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [status, setStatus] = useState<PaymentOrderStatus | ''>('')
  const [bizType, setBizType] = useState<PaymentBizType | ''>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadOrders = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchPaymentOrders(token, {
        page,
        limit,
        keyword: keyword || undefined,
        status: status || undefined,
        bizType: bizType || undefined,
      })
      setOrders(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载订单失败')
    } finally {
      setLoading(false)
    }
  }, [token, page, limit, keyword, status, bizType])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadOrders()
  }, [loadOrders])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(1)
    setKeyword(searchInput.trim())
  }

  return (
    <>
      <div className="toolbar">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="搜索订单号、标题、流水号…"
          />
          <button type="submit" className="btn btn-primary btn-sm">
            搜索
          </button>
        </form>

        <select
          className="filter-select"
          value={status}
          onChange={(e) => {
            setPage(1)
            setStatus(e.target.value as PaymentOrderStatus | '')
          }}
        >
          {STATUS_OPTIONS.map((item) => (
            <option key={item.label} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>

        <select
          className="filter-select"
          value={bizType}
          onChange={(e) => {
            setPage(1)
            setBizType(e.target.value as PaymentBizType | '')
          }}
        >
          {BIZ_OPTIONS.map((item) => (
            <option key={item.label} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </div>

      {loading && <div className="page-state">加载中…</div>}
      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>标题</th>
                <th>类型</th>
                <th>金额</th>
                <th>状态</th>
                <th>买家</th>
                <th>收款方</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 ? (
                <tr>
                  <td colSpan={8} className="empty-cell">
                    暂无订单
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
                  <tr key={order.id}>
                    <td className="cell-mono">{order.outTradeNo}</td>
                    <td className="cell-subject">{order.subject}</td>
                    <td>{formatPaymentBizType(order.bizType)}</td>
                    <td>{formatFee(order.amount)}</td>
                    <td>
                      <span
                        className={`status-pill order-status status-${order.status.toLowerCase()}`}
                      >
                        {formatPaymentStatus(order.status)}
                      </span>
                    </td>
                    <td>{formatUserBrief(order.user)}</td>
                    <td>{formatUserBrief(order.payee)}</td>
                    <td>{formatDateTime(order.createdAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          <Pagination
            page={page}
            total={total}
            limit={limit}
            hasMore={hasMore}
            onPageChange={setPage}
          />
        </div>
      )}
    </>
  )
}
