import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { fetchAnalyticsEvents } from '../api/analytics'
import { ApiError } from '../api/client'
import { Pagination } from '../components/Pagination'
import { useAuth } from '../hooks/useAuth'
import type { EventDailyCount } from '../types'
import { formatDateTime } from '../utils/format'
import './CreateUserModal.css'
import './AnalyticsEventDrilldownModal.css'

interface AnalyticsEventDrilldownModalProps {
  open: boolean
  days: number
  eventKey: string | null
  eventLabel: string
  eventBreakdown: EventDailyCount[]
  onClose: () => void
}

function shortDate(date: string) {
  return date.slice(5)
}

export function AnalyticsEventDrilldownModal({
  open,
  days,
  eventKey,
  eventLabel,
  eventBreakdown,
  onClose,
}: AnalyticsEventDrilldownModalProps) {
  const { token } = useAuth()
  const [records, setRecords] = useState<Awaited<
    ReturnType<typeof fetchAnalyticsEvents>
  >['items']>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const limit = 10

  const chartData = useMemo(() => {
    if (!eventKey) return []
    const map = new Map<string, number>()
    for (const item of eventBreakdown) {
      if (item.event !== eventKey) continue
      map.set(item.date, item.count)
    }
    return [...map.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([date, count]) => ({
        date,
        label: shortDate(date),
        count,
      }))
  }, [eventBreakdown, eventKey])

  const loadRecords = useCallback(async () => {
    if (!token || !eventKey) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchAnalyticsEvents(token, {
        days,
        event: eventKey,
        page,
        limit,
      })
      setRecords(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载事件明细失败')
    } finally {
      setLoading(false)
    }
  }, [token, eventKey, days, page])

  useEffect(() => {
    if (!open || !eventKey) return
    setPage(1)
  }, [open, eventKey])

  useEffect(() => {
    if (!open || !eventKey) return
    void loadRecords()
  }, [loadRecords, open, eventKey])

  if (!open || !eventKey) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card analytics-drilldown-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="analytics-drilldown-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <div>
            <h2 id="analytics-drilldown-title">埋点下钻</h2>
            <p className="analytics-drilldown-subtitle">{eventLabel}</p>
          </div>
          <button type="button" className="modal-close" onClick={onClose}>
            ×
          </button>
        </header>

        <div className="analytics-drilldown-body">
          <section className="analytics-drilldown-chart">
            <h3>每日趋势</h3>
            {chartData.length === 0 ? (
              <div className="chart-empty">该事件暂无趋势数据</div>
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                  <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                  <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={36} />
                  <Tooltip />
                  <Bar dataKey="count" name="次数" fill="#5b8def" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </section>

          <section className="analytics-drilldown-records">
            <h3>最近上报记录</h3>
            {loading && <div className="page-state">加载中…</div>}
            {!loading && error && <div className="alert alert-error">{error}</div>}
            {!loading && !error && records.length === 0 && (
              <div className="chart-empty">暂无原始事件记录</div>
            )}
            {!loading && !error && records.length > 0 && (
              <>
                <div className="table-wrap">
                  <table className="data-table analytics-event-table">
                    <thead>
                      <tr>
                        <th>时间</th>
                        <th>用户</th>
                        <th>平台</th>
                        <th>属性</th>
                      </tr>
                    </thead>
                    <tbody>
                      {records.map((item) => (
                        <tr key={item.id}>
                          <td>{formatDateTime(item.createdAt)}</td>
                          <td>{item.userId ?? '—'}</td>
                          <td>{item.platform ?? '—'}</td>
                          <td className="analytics-event-props">
                            {item.properties
                              ? JSON.stringify(item.properties)
                              : '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <Pagination
                  page={page}
                  total={total}
                  limit={limit}
                  hasMore={hasMore}
                  onPageChange={setPage}
                />
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}
