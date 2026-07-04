import { useCallback, useEffect, useMemo, useState, type CSSProperties } from 'react'
import {
  Area,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { fetchDailyAnalytics } from '../api/analytics'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { DailyAnalytics, DailyAnalyticsRow } from '../types'
import { AnalyticsInsightsPanels } from './AnalyticsInsightsPanels'
import { AnalyticsEventDrilldownModal } from '../components/AnalyticsEventDrilldownModal'
import { downloadCsv } from '../utils/exportCsv'
import './AnalyticsPage.css'

const DAY_OPTIONS = [7, 14, 30, 90] as const
const VIEW_TABS = [
  { id: 'overview', label: '趋势概览' },
  { id: 'insights', label: '维度分析' },
] as const
type ViewTab = (typeof VIEW_TABS)[number]['id']

const CHART_COLORS = [
  '#5b8def',
  '#7c5cff',
  '#10b981',
  '#f59e0b',
  '#ec4899',
  '#14b8a6',
  '#8b5cf6',
  '#f97316',
]

const CATEGORY_COLORS: Record<string, string> = {
  访问: '#5b8def',
  浏览: '#7c5cff',
  互动: '#ec4899',
  内容: '#10b981',
  交易: '#f59e0b',
  账号: '#14b8a6',
  其他: '#9ca3af',
}

function formatPaidAmount(value: string | number) {
  return `¥${Number(value).toFixed(2)}`
}

function shortDate(date: string) {
  return date.slice(5)
}

function ChartTooltip({
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
    <div className="chart-tooltip">
      <div className="chart-tooltip-title">{label}</div>
      {payload.map((item) => (
        <div key={item.name} className="chart-tooltip-row">
          <span className="chart-tooltip-dot" style={{ background: item.color }} />
          <span>{item.name}</span>
          <strong>{item.value}</strong>
        </div>
      ))}
    </div>
  )
}

function SummaryCard({
  label,
  value,
  sub,
  accent,
}: {
  label: string
  value: string | number
  sub?: string
  accent: string
}) {
  return (
    <div className="analytics-kpi" style={{ '--kpi-accent': accent } as CSSProperties}>
      <span className="analytics-kpi-label">{label}</span>
      <span className="analytics-kpi-value">{value}</span>
      {sub && <span className="analytics-kpi-sub">{sub}</span>}
    </div>
  )
}

export function AnalyticsPage() {
  const { token } = useAuth()
  const [days, setDays] = useState<(typeof DAY_OPTIONS)[number]>(30)
  const [viewTab, setViewTab] = useState<ViewTab>('overview')
  const [data, setData] = useState<DailyAnalytics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [drilldownEvent, setDrilldownEvent] = useState<{
    key: string
    label: string
  } | null>(null)
  const [categoryFilter, setCategoryFilter] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchDailyAnalytics(token, days)
      setData(result)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载数据分析失败')
    } finally {
      setLoading(false)
    }
  }, [token, days])

  useEffect(() => {
    if (!token) {
      setLoading(false)
      setError('未登录，请重新登录')
      return
    }
    void loadData()
  }, [loadData, token])

  const chartRows = useMemo(() => {
    if (!data?.rows) return []
    return data.rows.map((row) => ({
      ...row,
      label: shortDate(row.date),
      paidAmountNum: Number(row.paidAmount ?? 0),
      engagements:
        (row.postLikes ?? 0) +
        (row.postComments ?? 0) +
        (row.postFavorites ?? 0) +
        (row.activityLikes ?? 0) +
        (row.activityJoins ?? 0) +
        (row.chatSends ?? 0) +
        (row.userFollows ?? 0),
      appOpens: row.appOpens ?? 0,
      screenViews: row.screenViews ?? 0,
      dau: row.dau ?? 0,
      postViews: row.postViews ?? 0,
      activityViews: row.activityViews ?? 0,
      feedRefreshes: row.feedRefreshes ?? 0,
      activityFeedRefreshes: row.activityFeedRefreshes ?? 0,
      contentClicks: row.contentClicks ?? 0,
      searches: row.searches ?? 0,
      postLikes: row.postLikes ?? 0,
      postComments: row.postComments ?? 0,
      postFavorites: row.postFavorites ?? 0,
      activityLikes: row.activityLikes ?? 0,
      userFollows: row.userFollows ?? 0,
      chatSends: row.chatSends ?? 0,
      activityJoins: row.activityJoins ?? 0,
      newUsers: row.newUsers ?? 0,
      postsCreated: row.postsCreated ?? 0,
      activitiesCreated: row.activitiesCreated ?? 0,
      ordersPaid: row.ordersPaid ?? 0,
    }))
  }, [data])

  function handleExportCsv() {
    if (!data?.rows) return
    downloadCsv(
      `lfc-analytics-${days}d.csv`,
      [
        '日期',
        'DAU',
        '应用打开',
        '页面浏览',
        '笔记浏览',
        '活动浏览',
        '内容点击',
        '笔记流刷新',
        '活动流刷新',
        '点赞',
        '评论',
        '收藏',
        '活动点赞',
        '关注',
        '私信',
        '搜索',
        '新增用户',
        '新增笔记',
        '新增活动',
        '支付笔数',
        '支付成功',
        '金额',
        '埋点事件',
      ],
      [...data.rows].reverse().map((row) => [
        row.date,
        row.dau,
        row.appOpens,
        row.screenViews,
        row.postViews,
        row.activityViews,
        row.contentClicks,
        row.feedRefreshes,
        row.activityFeedRefreshes,
        row.postLikes,
        row.postComments,
        row.postFavorites,
        row.activityLikes,
        row.userFollows,
        row.chatSends,
        row.searches,
        row.newUsers,
        row.postsCreated,
        row.activitiesCreated,
        row.ordersPaid,
        row.paymentSuccess,
        row.paidAmount,
        row.eventCount,
      ]),
    )
  }

  const topEvents = useMemo(() => {
    if (!data) return []
    const totals = new Map<string, number>()
    for (const item of data.eventBreakdown) {
      totals.set(item.event, (totals.get(item.event) ?? 0) + item.count)
    }
    return [...totals.entries()]
      .filter(([event]) => {
        if (!categoryFilter) return true
        return (data.eventCategories[event] ?? '其他') === categoryFilter
      })
      .sort((a, b) => b[1] - a[1])
      .slice(0, 12)
      .map(([event, count]) => ({
        event,
        name: data.eventLabels[event] ?? event,
        count,
      }))
  }, [data, categoryFilter])

  const categoryChartData = useMemo(() => {
    if (!data?.categoryTotals) return []
    return data.categoryTotals.map((item) => ({
      name: item.category,
      value: item.count,
      fill: CATEGORY_COLORS[item.category] ?? CATEGORY_COLORS['其他'],
    }))
  }, [data])

  return (
    <div className="page analytics-root">
      <header className="page-header analytics-header">
        <div>
          <h1>数据分析</h1>
          <p>用户行为埋点与业务指标可视化</p>
        </div>
        <div className="analytics-toolbar">
          <div className="day-switch">
            {DAY_OPTIONS.map((option) => (
              <button
                key={option}
                type="button"
                className={option === days ? 'active' : ''}
                onClick={() => setDays(option)}
              >
                {option} 天
              </button>
            ))}
          </div>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => void loadData()}
            disabled={loading}
          >
            刷新
          </button>
          {data && viewTab === 'overview' && (
            <button
              type="button"
              className="btn btn-ghost"
              onClick={handleExportCsv}
              disabled={!data.rows.length}
            >
              导出 CSV
            </button>
          )}
        </div>
      </header>

      {!loading && !error && data && (
        <div className="analytics-view-tabs">
          {VIEW_TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={viewTab === tab.id ? 'active' : ''}
              onClick={() => setViewTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      {loading && <div className="page-state">加载中…</div>}
      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && data && viewTab === 'overview' && (
        <div className="analytics-page">
          <section className="analytics-kpi-grid">
            <SummaryCard
              label="日均 DAU"
              value={data.summary.avgDau ?? 0}
              sub={`${days} 日累计 ${data.summary.totalDau ?? 0}`}
              accent="#5b8def"
            />
            <SummaryCard
              label="埋点事件"
              value={(data.summary.totalEvents ?? 0).toLocaleString()}
              sub="客户端上报总量"
              accent="#7c5cff"
            />
            <SummaryCard
              label="内容浏览"
              value={(
                (data.summary.totalPostViews ?? 0) +
                (data.summary.totalActivityViews ?? 0)
              ).toLocaleString()}
              sub={`笔记 ${data.summary.totalPostViews ?? 0} · 活动 ${data.summary.totalActivityViews ?? 0}`}
              accent="#10b981"
            />
            <SummaryCard
              label="互动行为"
              value={(data.summary.totalEngagements ?? 0).toLocaleString()}
              sub="点赞/评论/关注/私信等"
              accent="#ec4899"
            />
            <SummaryCard
              label="新增用户"
              value={data.summary.totalNewUsers ?? 0}
              accent="#14b8a6"
            />
            <SummaryCard
              label="支付成功"
              value={data.summary.totalPaymentSuccess ?? 0}
              sub={formatPaidAmount(data.summary.totalPaidAmount ?? '0')}
              accent="#f59e0b"
            />
          </section>

          <div className="analytics-charts-grid">
            <section className="analytics-panel analytics-panel-wide">
              <div className="panel-head">
                <h2>活跃与访问趋势</h2>
                <span className="panel-hint">DAU · 应用打开 · 页面浏览</span>
              </div>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={300}>
                  <ComposedChart data={chartRows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="dauFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#5b8def" stopOpacity={0.35} />
                        <stop offset="100%" stopColor="#5b8def" stopOpacity={0.02} />
                      </linearGradient>
                      <linearGradient id="openFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#7c5cff" stopOpacity={0.25} />
                        <stop offset="100%" stopColor="#7c5cff" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                    <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={36} />
                    <Tooltip content={<ChartTooltip />} />
                    <Legend />
                    <Area
                      type="monotone"
                      dataKey="dau"
                      name="DAU"
                      stroke="#5b8def"
                      fill="url(#dauFill)"
                      strokeWidth={2}
                    />
                    <Area
                      type="monotone"
                      dataKey="appOpens"
                      name="应用打开"
                      stroke="#7c5cff"
                      fill="url(#openFill)"
                      strokeWidth={2}
                    />
                    <Line
                      type="monotone"
                      dataKey="screenViews"
                      name="页面浏览"
                      stroke="#14b8a6"
                      strokeWidth={2}
                      dot={false}
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="analytics-panel">
              <div className="panel-head">
                <h2>行为分类占比</h2>
                <span className="panel-hint">
                  {categoryFilter ? `已筛选：${categoryFilter}` : '点击扇区筛选 TOP 事件'}
                </span>
              </div>
              <div className="chart-box chart-box-pie analytics-category-pie">
                {categoryChartData.length === 0 ? (
                  <div className="chart-empty">暂无埋点数据</div>
                ) : (
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={categoryChartData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={62}
                        outerRadius={98}
                        paddingAngle={2}
                        onClick={(entry) => {
                          const name = String(entry?.name ?? '')
                          setCategoryFilter((prev) => (prev === name ? null : name))
                        }}
                      >
                        {categoryChartData.map((entry) => (
                          <Cell key={entry.name} fill={entry.fill} />
                        ))}
                      </Pie>
                      <Tooltip content={<ChartTooltip />} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>
            </section>

            <section className="analytics-panel analytics-panel-wide">
              <div className="panel-head">
                <h2>内容浏览与搜索</h2>
                <span className="panel-hint">笔记/活动详情 · Feed 刷新 · 搜索</span>
              </div>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={280}>
                  <LineChart data={chartRows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                    <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={36} />
                    <Tooltip content={<ChartTooltip />} />
                    <Legend />
                    <Line type="monotone" dataKey="postViews" name="笔记详情" stroke="#5b8def" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="activityViews" name="活动详情" stroke="#7c5cff" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="contentClicks" name="卡片点击" stroke="#ec4899" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="feedRefreshes" name="笔记流刷新" stroke="#10b981" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="activityFeedRefreshes" name="活动流刷新" stroke="#14b8a6" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="searches" name="搜索" stroke="#f59e0b" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="analytics-panel analytics-panel-wide">
              <div className="panel-head">
                <h2>互动行为趋势</h2>
                <span className="panel-hint">点赞 · 评论 · 收藏 · 关注 · 私信 · 报名</span>
              </div>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={chartRows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                    <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={36} />
                    <Tooltip content={<ChartTooltip />} />
                    <Legend />
                    <Bar dataKey="postLikes" name="笔记点赞" stackId="a" fill="#5b8def" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="postComments" name="评论" stackId="a" fill="#ec4899" />
                    <Bar dataKey="postFavorites" name="收藏" stackId="a" fill="#f97316" />
                    <Bar dataKey="activityLikes" name="活动点赞" stackId="a" fill="#8b5cf6" />
                    <Bar dataKey="userFollows" name="关注" stackId="a" fill="#7c5cff" />
                    <Bar dataKey="chatSends" name="私信" stackId="a" fill="#14b8a6" />
                    <Bar dataKey="activityJoins" name="活动报名" stackId="a" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="analytics-panel">
              <div className="panel-head">
                <h2>业务增长</h2>
                <span className="panel-hint">新增用户/内容/支付</span>
              </div>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={chartRows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                    <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={36} />
                    <Tooltip content={<ChartTooltip />} />
                    <Legend />
                    <Bar dataKey="newUsers" name="新增用户" fill="#5b8def" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="postsCreated" name="新增笔记" fill="#10b981" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="activitiesCreated" name="新增活动" fill="#7c5cff" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="ordersPaid" name="支付笔数" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="analytics-panel analytics-panel-wide">
              <div className="panel-head">
                <h2>支付金额趋势</h2>
                <span className="panel-hint">每日实付金额与笔数</span>
              </div>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={260}>
                  <ComposedChart data={chartRows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                    <YAxis yAxisId="amount" tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={48} />
                    <YAxis yAxisId="count" orientation="right" tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={32} />
                    <Tooltip
                      content={({ active, payload, label }) => {
                        if (!active || !payload?.length) return null
                        const row = payload[0]?.payload as { ordersPaid?: number; paidAmountNum?: number }
                        return (
                          <div className="chart-tooltip">
                            <div className="chart-tooltip-title">{label}</div>
                            <div className="chart-tooltip-row">
                              <span>支付笔数</span>
                              <strong>{row.ordersPaid ?? 0}</strong>
                            </div>
                            <div className="chart-tooltip-row">
                              <span>支付金额</span>
                              <strong>{formatPaidAmount(row.paidAmountNum ?? 0)}</strong>
                            </div>
                          </div>
                        )
                      }}
                    />
                    <Legend />
                    <Bar yAxisId="count" dataKey="ordersPaid" name="支付笔数" fill="rgba(91, 141, 239, 0.35)" radius={[4, 4, 0, 0]} />
                    <Line
                      yAxisId="amount"
                      type="monotone"
                      dataKey="paidAmountNum"
                      name="支付金额"
                      stroke="#f59e0b"
                      strokeWidth={2}
                      dot={false}
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="analytics-panel">
              <div className="panel-head">
                <h2>热门埋点 TOP 12</h2>
                <span className="panel-hint chart-clickable-hint">点击条目查看下钻</span>
              </div>
              {topEvents.length === 0 ? (
                <div className="chart-empty">暂无埋点数据</div>
              ) : (
                <div className="chart-box">
                  <ResponsiveContainer width="100%" height={Math.max(280, topEvents.length * 28)}>
                    <BarChart
                      data={topEvents}
                      layout="vertical"
                      margin={{ top: 4, right: 16, left: 8, bottom: 4 }}
                      onClick={(state) => {
                        const payload = state?.activePayload?.[0]?.payload as
                          | { event?: string; name?: string }
                          | undefined
                        if (!payload?.event) return
                        setDrilldownEvent({
                          key: payload.event,
                          label: payload.name ?? payload.event,
                        })
                      }}
                    >
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" horizontal={false} />
                      <XAxis type="number" tick={{ fontSize: 12 }} stroke="var(--text-muted)" />
                      <YAxis
                        type="category"
                        dataKey="name"
                        width={108}
                        tick={{ fontSize: 12 }}
                        stroke="var(--text-muted)"
                      />
                      <Tooltip content={<ChartTooltip />} />
                      <Bar dataKey="count" name="次数" radius={[0, 6, 6, 0]} className="analytics-top-event-bar">
                        {topEvents.map((_, index) => (
                          <Cell key={_.event} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </section>
          </div>

          <section className="analytics-panel">
            <div className="panel-head">
              <h2>每日明细</h2>
              <span className="panel-hint">完整指标表</span>
            </div>
            <div className="analytics-table-wrap">
              <table className="analytics-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>DAU</th>
                    <th>打开</th>
                    <th>页面</th>
                    <th>笔记浏览</th>
                    <th>活动浏览</th>
                    <th>点击</th>
                    <th>点赞</th>
                    <th>评论</th>
                    <th>收藏</th>
                    <th>关注</th>
                    <th>私信</th>
                    <th>搜索</th>
                    <th>新增用户</th>
                    <th>新增笔记</th>
                    <th>支付</th>
                    <th>金额</th>
                    <th>埋点</th>
                  </tr>
                </thead>
                <tbody>
                  {[...data.rows].reverse().map((row: DailyAnalyticsRow) => (
                    <tr key={row.date}>
                      <td>{row.date}</td>
                      <td>{row.dau}</td>
                      <td>{row.appOpens}</td>
                      <td>{row.screenViews}</td>
                      <td>{row.postViews}</td>
                      <td>{row.activityViews}</td>
                      <td>{row.contentClicks}</td>
                      <td>{row.postLikes}</td>
                      <td>{row.postComments}</td>
                      <td>{row.postFavorites}</td>
                      <td>{row.userFollows}</td>
                      <td>{row.chatSends}</td>
                      <td>{row.searches}</td>
                      <td>{row.newUsers}</td>
                      <td>{row.postsCreated}</td>
                      <td>{row.ordersPaid}</td>
                      <td>{formatPaidAmount(row.paidAmount)}</td>
                      <td>{row.eventCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}

      {!loading && !error && data && viewTab === 'insights' && (
        data.insights ? (
          <AnalyticsInsightsPanels insights={data.insights} />
        ) : (
          <div className="page-state">
            维度分析数据暂不可用，请确认后端已更新并重启服务。
          </div>
        )
      )}

      {!loading && !error && !data && (
        <div className="page-state">暂无数据，请确认后端已启动并完成埋点上报</div>
      )}

      <AnalyticsEventDrilldownModal
        open={drilldownEvent != null}
        days={days}
        eventKey={drilldownEvent?.key ?? null}
        eventLabel={drilldownEvent?.label ?? ''}
        eventBreakdown={data?.eventBreakdown ?? []}
        onClose={() => setDrilldownEvent(null)}
      />
    </div>
  )
}
