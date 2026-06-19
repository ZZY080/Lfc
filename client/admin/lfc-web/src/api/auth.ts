import type { AuthTokenResponse } from '../types'
import { request } from './client'

export function login(email: string, password: string) {
  return request<AuthTokenResponse>('/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export function logout(token: string) {
  return request<{ message: string }>(
    '/admin/auth/logout',
    { method: 'POST' },
    token,
  )
}
