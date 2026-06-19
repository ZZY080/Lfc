import type { AuthTokenResponse } from '../types'
import { request } from './client'

export function login(email: string, password: string) {
  return request<AuthTokenResponse>('/api/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export function logout(token: string) {
  return request<{ message: string }>(
    '/api/admin/auth/logout',
    { method: 'POST' },
    token,
  )
}
