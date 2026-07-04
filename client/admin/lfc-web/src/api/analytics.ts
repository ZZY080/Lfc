import type { DailyAnalytics, PaginatedResult, AnalyticsEventRecord } from '../types'
import { request } from './client'

export function fetchDailyAnalytics(token: string, days = 30) {
  return request<DailyAnalytics>(
    `/admin/analytics/daily?days=${days}`,
    {},
    token,
  )
}

export interface AnalyticsEventsParams {
  days?: number
  event?: string
  page?: number
  limit?: number
}

export function fetchAnalyticsEvents(
  token: string,
  params: AnalyticsEventsParams = {},
) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  const query = search.toString()
  return request<PaginatedResult<AnalyticsEventRecord>>(
    `/admin/analytics/events${query ? `?${query}` : ''}`,
    {},
    token,
  )
}
