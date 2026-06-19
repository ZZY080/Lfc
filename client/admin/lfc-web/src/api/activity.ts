import type { Activity, ActivityStatus, PaginatedResult } from '../types'
import { request } from './client'

export interface ActivityListParams {
  page?: number
  limit?: number
  keyword?: string
  status?: ActivityStatus
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

export function fetchPendingActivities(
  token: string,
  params: ActivityListParams = {},
) {
  return request<PaginatedResult<Activity>>(
    `/admin/activity/pending${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchAllActivities(
  token: string,
  params: ActivityListParams = {},
) {
  return request<PaginatedResult<Activity>>(
    `/admin/activity${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchActivity(token: string, id: number) {
  return request<Activity>(`/admin/activity/${id}`, {}, token)
}

export function updateActivity(
  token: string,
  id: number,
  data: {
    title?: string
    description?: string
    location?: string
    status?: ActivityStatus
    reviewComment?: string
    maxParticipants?: number
    fee?: number
  },
) {
  return request<Activity>(
    `/admin/activity/${id}`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function reviewActivity(
  token: string,
  id: number,
  data: {
    status: Extract<ActivityStatus, 'APPROVED' | 'REJECTED'>
    reviewComment?: string
  },
) {
  return request<Activity>(
    `/admin/activity/${id}/review`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function deleteActivity(token: string, id: number) {
  return request<{ message: string }>(
    `/admin/activity/${id}`,
    { method: 'DELETE' },
    token,
  )
}
