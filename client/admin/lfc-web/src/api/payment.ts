import type {
  PaginatedResult,
  PaymentAfterSales,
  PaymentAfterSalesStatus,
  PaymentBizType,
  PaymentOrder,
  PaymentOrderDetail,
  PaymentOrderStatus,
} from '../types'
import { request } from './client'

export interface PaymentListParams {
  page?: number
  limit?: number
  keyword?: string
  status?: PaymentOrderStatus
  bizType?: PaymentBizType
}

export interface AfterSalesListParams {
  page?: number
  limit?: number
  keyword?: string
  status?: PaymentAfterSalesStatus
}

function buildQuery(params: object) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

export function fetchPaymentOrders(
  token: string,
  params: PaymentListParams = {},
) {
  return request<PaginatedResult<PaymentOrder>>(
    `/api/admin/payment/order${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchPaymentOrder(token: string, orderId: number) {
  return request<PaymentOrderDetail>(
    `/api/admin/payment/order/${orderId}`,
    {},
    token,
  )
}

export function fetchAfterSales(
  token: string,
  params: AfterSalesListParams = {},
) {
  return request<PaginatedResult<PaymentAfterSales>>(
    `/api/admin/payment/after-sales${buildQuery(params)}`,
    {},
    token,
  )
}

export function reviewAfterSales(
  token: string,
  afterSalesId: number,
  data: { action: 'approve' | 'reject'; rejectReason?: string },
) {
  return request<PaymentAfterSales>(
    `/api/admin/payment/after-sales/${afterSalesId}/review`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}
