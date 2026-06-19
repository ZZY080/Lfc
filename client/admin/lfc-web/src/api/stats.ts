import type { PaginatedResult, StatsOverview } from '../types'
import { request } from './client'

export function fetchStatsOverview(token: string) {
  return request<StatsOverview>('/admin/stats/overview', {}, token)
}

export type { PaginatedResult }
