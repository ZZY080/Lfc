import type { PaginatedResult, User, UserRole, UserStatus } from '../types'
import { request } from './client'

export interface UserListParams {
  page?: number
  limit?: number
  keyword?: string
  role?: UserRole
  status?: UserStatus
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

export function fetchUsers(token: string, params: UserListParams = {}) {
  return request<PaginatedResult<User>>(
    `/admin/user${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchUser(token: string, userId: number) {
  return request<User>(`/admin/user/${userId}`, {}, token)
}

export function createUser(
  token: string,
  data: {
    email: string
    password: string
    studentId: string
    realName: string
    role?: UserRole
  },
) {
  return request<User>(
    '/admin/user',
    { method: 'POST', body: JSON.stringify(data) },
    token,
  )
}

export function updateUser(
  token: string,
  userId: number,
  data: {
    email?: string
    studentId?: string
    realName?: string
    nickname?: string | null
    role?: UserRole
  },
) {
  return request<User>(
    `/admin/user/${userId}`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function updateUserStatus(
  token: string,
  userId: number,
  status: UserStatus,
  banReason?: string,
) {
  return request<User>(
    `/admin/user/${userId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify({ status, banReason }),
    },
    token,
  )
}

export function updateUserRole(token: string, userId: number, role: UserRole) {
  return request<User>(
    `/admin/user/${userId}/role`,
    { method: 'PATCH', body: JSON.stringify({ role }) },
    token,
  )
}

export function deleteUser(token: string, userId: number) {
  return request<{ message: string }>(
    `/admin/user/${userId}`,
    { method: 'DELETE' },
    token,
  )
}
