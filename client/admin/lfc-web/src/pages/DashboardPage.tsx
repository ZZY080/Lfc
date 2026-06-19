import { Link } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import { fetchStatsOverview } from '../api/stats'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { StatsOverview } from '../types'
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

export function DashboardPage() {
  const { token } = useAuth()
  const [stats, setStats] = useState<StatsOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadStats = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const data = await fetchStatsOverview(token)
      setStats(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载统计数据失败')
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadStats()
  }, [loadStats])

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
          onClick={() => void loadStats()}
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
