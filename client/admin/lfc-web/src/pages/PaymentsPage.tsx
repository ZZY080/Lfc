import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { PaymentAfterSalesTab } from './PaymentAfterSalesTab'
import { PaymentOrdersTab } from './PaymentOrdersTab'
import type { PaymentAfterSalesStatus } from '../types'

type PaymentTab = 'orders' | 'after-sales'
type AfterSalesStatusFilter = PaymentAfterSalesStatus | 'all'

const PAYMENT_TABS: { value: PaymentTab; label: string }[] = [
  { value: 'orders', label: '订单' },
  { value: 'after-sales', label: '售后' },
]

function parseTab(value: string | null): PaymentTab {
  return value === 'after-sales' ? 'after-sales' : 'orders'
}

function parseAfterSalesStatus(value: string | null): AfterSalesStatusFilter {
  const allowed: AfterSalesStatusFilter[] = [
    'all',
    'PENDING',
    'REFUNDING',
    'REFUNDED',
    'REJECTED',
    'CANCELLED',
  ]
  return allowed.includes(value as AfterSalesStatusFilter)
    ? (value as AfterSalesStatusFilter)
    : 'PENDING'
}

export function PaymentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = parseTab(searchParams.get('tab'))
  const afterSalesStatus = parseAfterSalesStatus(searchParams.get('status'))
  const [refreshKey, setRefreshKey] = useState(0)

  function switchTab(next: PaymentTab) {
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        params.set('tab', next)
        if (next === 'after-sales' && !params.get('status')) {
          params.set('status', 'PENDING')
        }
        if (next === 'orders') {
          params.delete('status')
        }
        return params
      },
      { replace: true },
    )
  }

  return (
    <div className="page">
      <PageHeader
        title="支付管理"
        description="查看支付订单，审核用户退款申请（拒绝需填写理由）"
        onRefresh={() => setRefreshKey((key) => key + 1)}
      />

      <div className="toolbar">
        <div className="filter-tabs">
          {PAYMENT_TABS.map((item) => (
            <button
              key={item.value}
              type="button"
              className={`filter-tab${tab === item.value ? ' active' : ''}`}
              onClick={() => switchTab(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {tab === 'orders' ? (
        <PaymentOrdersTab key={`orders-${refreshKey}`} />
      ) : (
        <PaymentAfterSalesTab
          key={`after-sales-${refreshKey}-${afterSalesStatus}`}
          initialStatus={afterSalesStatus}
        />
      )}
    </div>
  )
}
