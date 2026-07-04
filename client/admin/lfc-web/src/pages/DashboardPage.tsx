import { Link } from 'react-router-dom'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { fetchDailyAnalytics } from '../api/analytics'
import { fetchStatsOverview } from '../api/stats'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { DailyAnalytics, StatsOverview } from '../types'
import './DashboardPage.css'

function StatCard({
  label,
  value,
  sub,
  highlight,
  to,
}: {
  label: string
  value: string | number
  sub?: string
  highlight?: boolean
  to?: string
}) {
  const content = (
    <>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      {sub && <span className="stat-sub">{sub}</span>}
    </>
  )

  if (to) {
    return (
      <Link
        to={to}
        className={`stat-card stat-card-link${highlight ? ' stat-card-highlight' : ''}`}
      >
        {content}
      </Link>
    )
  }

  return (
    <div className={`stat-card${highlight ? ' stat-card-highlight' : ''}`}>
      {content}
    </div>
  )
}

function MiniChartTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean
  payload?: Array<{ name?: string; value?: number; color?: string }>
  label?: string
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="dashboard-chart-tooltip">
      <div className="dashboard-chart-tooltip-title">{label}</div>
      {payload.map((item) => (
        <div key={item.name} className="dashboard-chart-tooltip-row">
          <span style={{ color: item.color }}>{item.name}</span>
          <strong>{item.value}</strong>
        </div>
      ))}
    </div>
  )
}

export function DashboardPage() {
  const { token } = useAuth()
  const [stats, setStats] = useState<StatsOverview | null>(null)
  const [analytics, setAnalytics] = useState<DailyAnalytics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadData = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const [statsData, analyticsData] = await Promise.all([
        fetchStatsOverview(token),
        fetchDailyAnalytics(token, 7),
      ])
      setStats(statsData)
      setAnalytics(analyticsData)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载统计数据失败')
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadData()
  }, [loadData])

  const chartRows = useMemo(() => {
    if (!analytics?.rows) return []
    return analytics.rows.map((row) => ({
      label: row.date.slice(5),
      dau: row.dau ?? 0,
      events: row.eventCount ?? 0,
      paymentSuccess: row.paymentSuccess ?? row.ordersPaid ?? 0,
    }))
  }, [analytics])

  const paidAmount = stats
    ? Number(stats.payments.totalPaidAmount).toFixed(2)
    : '0.00'

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>数据概览</h1>
          <p>平台核心指标一览</p>
        </div>
        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => void loadData()}
          disabled={loading}
        >
          刷新
        </button>
      </header>

      {loading && <div className="page-state">加载中…</div>}
      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && stats && (
        <div className="dashboard">
          <section className="quick-links">
            <Link to="/analytics" className="quick-link quick-link-analytics">
              数据分析
            </Link>
            <Link to="/activities?status=PENDING" className="quick-link">
              活动管理
              {stats.activities.pending > 0 && (
                <span className="quick-link-badge">{stats.activities.pending}</span>
              )}
            </Link>
            <Link to="/posts?status=PENDING" className="quick-link">
              笔记管理
              {stats.posts.pending > 0 && (
                <span className="quick-link-badge">{stats.posts.pending}</span>
              )}
            </Link>
            <Link to="/payments?tab=after-sales&status=PENDING" className="quick-link">
              支付管理
              {stats.payments.pendingAfterSales > 0 && (
                <span className="quick-link-badge">
                  {stats.payments.pendingAfterSales}
                </span>
              )}
            </Link>
            <Link to="/users" className="quick-link">
              用户管理
            </Link>
          </section>

          {analytics && (
            <section className="dashboard-analytics">
              <div className="dashboard-analytics-head">
                <div>
                  <h2>近 7 日趋势</h2>
                  <p>DAU · 埋点事件 · 支付成功</p>
                </div>
                <Link to="/analytics" className="dashboard-analytics-link">
                  查看完整分析 →
                </Link>
              </div>

              <div className="dashboard-analytics-kpis">
                <div className="dashboard-mini-kpi">
                  <span>日均 DAU</span>
                  <strong>{analytics.summary.avgDau ?? 0}</strong>
                </div>
                <div className="dashboard-mini-kpi">
                  <span>埋点事件</span>
                  <strong>{(analytics.summary.totalEvents ?? 0).toLocaleString()}</strong>
                </div>
                <div className="dashboard-mini-kpi">
                  <span>支付成功</span>
                  <strong>{analytics.summary.totalPaymentSuccess ?? 0}</strong>
                </div>
                <div className="dashboard-mini-kpi">
                  <span>支付金额</span>
                  <strong>¥{Number(analytics.summary.totalPaidAmount ?? 0).toFixed(2)}</strong>
                </div>
              </div>

              <div className="dashboard-chart-box">
                {chartRows.length === 0 ? (
                  <div className="dashboard-chart-empty">暂无埋点数据</div>
                ) : (
                  <ResponsiveContainer width="100%" height={220}>
                    <ComposedChart data={chartRows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                      <defs>
                        <linearGradient id="dashboardDauFill" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#5b8def" stopOpacity={0.3} />
                          <stop offset="100%" stopColor="#5b8def" stopOpacity={0.02} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                      <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                      <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={32} />
                      <Tooltip content={<MiniChartTooltip />} />
                      <Area
                        type="monotone"
                        dataKey="dau"
                        name="DAU"
                        stroke="#5b8def"
                        fill="url(#dashboardDauFill)"
                        strokeWidth={2}
                      />
                      <Line
                        type="monotone"
                        dataKey="events"
                        name="埋点"
                        stroke="#7c5cff"
                        strokeWidth={2}
                        dot={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="paymentSuccess"
                        name="支付"
                        stroke="#f59e0b"
                        strokeWidth={2}
                        dot={false}
                      />
                    </ComposedChart>
                  </ResponsiveContainer>
                )}
              </div>
            </section>
          )}

          <section className="stat-section">
            <h2>用户</h2>
            <div className="stat-grid">
              <StatCard label="总用户" value={stats.users.total} to="/users" />
              <StatCard label="普通用户" value={stats.users.consumers} to="/users" />
              <StatCard label="管理员" value={stats.users.admins} to="/users" />
              <StatCard
                label="近 7 日新增"
                value={stats.users.recent7Days}
                highlight
                to="/users"
              />
            </div>
          </section>

          <section className="stat-section">
            <h2>笔记</h2>
            <div className="stat-grid">
              <StatCard label="总笔记" value={stats.posts.total} to="/posts" />
              <StatCard
                label="待审核"
                value={stats.posts.pending}
                highlight={stats.posts.pending > 0}
                to="/posts?status=PENDING"
              />
              <StatCard label="已通过" value={stats.posts.approved} to="/posts?status=APPROVED" />
              <StatCard label="已拒绝" value={stats.posts.rejected} to="/posts?status=REJECTED" />
              <StatCard label="已下架" value={stats.posts.offShelf} to="/posts?status=OFF_SHELF" />
            </div>
          </section>

          <section className="stat-section">
            <h2>活动</h2>
            <div className="stat-grid">
              <StatCard label="总活动" value={stats.activities.total} to="/activities" />
              <StatCard
                label="待审核"
                value={stats.activities.pending}
                highlight={stats.activities.pending > 0}
                to="/activities?status=PENDING"
              />
              <StatCard label="已通过" value={stats.activities.approved} to="/activities?status=APPROVED" />
              <StatCard label="已拒绝" value={stats.activities.rejected} to="/activities?status=REJECTED" />
              <StatCard label="已下架" value={stats.activities.offShelf} to="/activities?status=OFF_SHELF" />
            </div>
          </section>

          <section className="stat-section">
            <h2>支付</h2>
            <div className="stat-grid">
              <StatCard label="总订单" value={stats.payments.total} to="/payments?tab=orders" />
              <StatCard label="已支付" value={stats.payments.paid} to="/payments?tab=orders" />
              <StatCard label="支付总额" value={`¥${paidAmount}`} to="/payments?tab=orders" />
              <StatCard
                label="待审核退款"
                value={stats.payments.pendingAfterSales}
                highlight={stats.payments.pendingAfterSales > 0}
                to="/payments?tab=after-sales&status=PENDING"
              />
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
