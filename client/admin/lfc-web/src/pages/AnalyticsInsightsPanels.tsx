import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { AnalyticsInsights, DimensionCount } from '../types'
import './AnalyticsInsightsPanels.css'

const CHART_COLORS = [
  '#5b8def',
  '#7c5cff',
  '#10b981',
  '#f59e0b',
  '#ec4899',
  '#14b8a6',
]

function DimensionList({
  title,
  hint,
  items,
  emptyText = '暂无数据',
}: {
  title: string
  hint?: string
  items: DimensionCount[]
  emptyText?: string
}) {
  if (items.length === 0) {
    return (
      <section className="analytics-panel insight-panel">
        <div className="panel-head">
          <h2>{title}</h2>
          {hint && <span className="panel-hint">{hint}</span>}
        </div>
        <div className="chart-empty">{emptyText}</div>
      </section>
    )
  }

  const max = items[0]?.count ?? 1

  return (
    <section className="analytics-panel insight-panel">
      <div className="panel-head">
        <h2>{title}</h2>
        {hint && <span className="panel-hint">{hint}</span>}
      </div>
      <div className="dimension-list">
        {items.map((item, index) => (
          <div key={`${item.key}-${index}`} className="dimension-row">
            <span className="dimension-label" title={item.key}>
              {item.label}
            </span>
            <div className="dimension-track">
              <div
                className="dimension-fill"
                style={{
                  width: `${Math.round((item.count / max) * 100)}%`,
                  background: CHART_COLORS[index % CHART_COLORS.length],
                }}
              />
            </div>
            <span className="dimension-value">{item.count}</span>
          </div>
        ))}
      </div>
    </section>
  )
}

function FunnelCard({
  title,
  steps,
}: {
  title: string
  steps: Array<{ label: string; value: number; rate?: number }>
}) {
  const max = steps[0]?.value ?? 1
  return (
    <section className="analytics-panel funnel-card">
      <h2>{title}</h2>
      <div className="funnel-steps">
        {steps.map((step, index) => (
          <div key={step.label} className="funnel-step">
            <div className="funnel-step-head">
              <span>{step.label}</span>
              <strong>
                {step.value.toLocaleString()}
                {step.rate !== undefined && (
                  <em> ({step.rate}%)</em>
                )}
              </strong>
            </div>
            <div className="funnel-track">
              <div
                className="funnel-fill"
                style={{
                  width: `${max > 0 ? Math.max(8, Math.round((step.value / max) * 100)) : 0}%`,
                  background: CHART_COLORS[index % CHART_COLORS.length],
                }}
              />
            </div>
            {index < steps.length - 1 && <div className="funnel-arrow">↓</div>}
          </div>
        ))}
      </div>
    </section>
  )
}

function ChartTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean
  payload?: Array<{ value?: number }>
  label?: string
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip-title">{label}</div>
      <strong>{payload[0]?.value}</strong>
    </div>
  )
}

function PaymentScenarioTable({
  items,
}: {
  items: Array<{ key: string; label: string; start: number; success: number; rate: number }>
}) {
  if (items.length === 0) {
    return <div className="chart-empty">暂无支付埋点</div>
  }

  return (
    <div className="scenario-table-wrap">
      <table className="scenario-table">
        <thead>
          <tr>
            <th>支付场景</th>
            <th>发起</th>
            <th>成功</th>
            <th>转化率</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.key}>
              <td>{item.label}</td>
              <td>{item.start}</td>
              <td>{item.success}</td>
              <td>
                <span className="scenario-rate">{item.rate}%</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function AnalyticsInsightsPanels({
  insights,
}: {
  insights: AnalyticsInsights
}) {
  const hourlyPeak = insights.hourlyActivity.reduce(
    (best, item) => (item.count > best.count ? item : best),
    insights.hourlyActivity[0] ?? { hour: 0, label: '00:00', count: 0 },
  )

  return (
    <div className="insights-page">
      <section className="analytics-panel analytics-panel-wide">
        <div className="panel-head">
          <h2>活跃时段分布</h2>
          <span className="panel-hint">
            高峰 {hourlyPeak.label}（{hourlyPeak.count} 次事件）
          </span>
        </div>
        <div className="chart-box">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={insights.hourlyActivity} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} stroke="var(--text-muted)" interval={2} />
              <YAxis tick={{ fontSize: 12 }} stroke="var(--text-muted)" width={36} />
              <Tooltip content={<ChartTooltip />} />
              <Bar dataKey="count" name="事件数" radius={[4, 4, 0, 0]}>
                {insights.hourlyActivity.map((item, index) => (
                  <Cell
                    key={item.hour}
                    fill={
                      item.hour === hourlyPeak.hour
                        ? '#5b8def'
                        : CHART_COLORS[(index + 2) % CHART_COLORS.length]
                    }
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>

      <div className="funnel-grid">
        <FunnelCard
          title="支付转化漏斗"
          steps={[
            { label: '发起支付', value: insights.funnels.payment.start },
            {
              label: '支付成功',
              value: insights.funnels.payment.success,
              rate: insights.funnels.payment.rate,
            },
          ]}
        />
        <FunnelCard
          title="内容浏览漏斗"
          steps={[
            { label: '卡片点击', value: insights.funnels.content.clicks },
            {
              label: '详情浏览',
              value: insights.funnels.content.views,
              rate: insights.funnels.content.viewRate,
            },
            {
              label: '互动行为',
              value: insights.funnels.content.engagements,
              rate: insights.funnels.content.engageRate,
            },
          ]}
        />
        <FunnelCard
          title="笔记互动漏斗"
          steps={[
            { label: '笔记详情', value: insights.funnels.post.views },
            {
              label: '点赞',
              value: insights.funnels.post.likes,
              rate: insights.funnels.post.likeRate,
            },
            {
              label: '评论',
              value: insights.funnels.post.comments,
              rate: insights.funnels.post.commentRate,
            },
            {
              label: '收藏',
              value: insights.funnels.post.favorites,
              rate:
                insights.funnels.post.views > 0
                  ? Math.round(
                      (insights.funnels.post.favorites / insights.funnels.post.views) *
                        1000,
                    ) / 10
                  : 0,
            },
          ]}
        />
      </div>

      <section className="analytics-panel analytics-panel-wide">
        <div className="panel-head">
          <h2>支付场景转化</h2>
          <span className="panel-hint">按业务类型拆分发起/成功/转化率</span>
        </div>
        <PaymentScenarioTable items={insights.paymentScenarioStats ?? []} />
      </section>

      <div className="insight-grid">
        <DimensionList
          title="笔记分类 · 发布"
          hint="按频道统计"
          items={insights.postCategoryCreateBreakdown ?? []}
          emptyText="暂无发布埋点"
        />
        <DimensionList
          title="笔记分类 · 浏览"
          hint="详情页浏览"
          items={insights.postCategoryViewBreakdown ?? []}
          emptyText="暂无浏览埋点"
        />
        <DimensionList
          title="热门搜索词"
          hint="TOP 20"
          items={insights.topSearchKeywords}
          emptyText="暂无搜索埋点"
        />
        <DimensionList
          title="页面访问分布"
          hint="子页面 + Tab"
          items={insights.screenBreakdown}
        />
        <DimensionList
          title="Feed 频道偏好"
          items={insights.feedChannelBreakdown}
        />
        <DimensionList
          title="Feed 顶栏切换"
          items={insights.feedTabBreakdown}
        />
        <DimensionList
          title="内容卡片点击"
          items={insights.contentClickBreakdown}
        />
        <DimensionList
          title="搜索 Tab 分布"
          items={insights.searchTabBreakdown}
        />
        <DimensionList
          title="私信类型"
          items={insights.chatMessageTypes}
        />
      </div>
    </div>
  )
}
