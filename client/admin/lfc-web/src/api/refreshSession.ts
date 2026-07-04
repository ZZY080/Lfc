import { getApiBaseUrl } from './baseUrl'
import type { AuthTokenResponse } from '../types'

export async function refreshAdminSession(
  refreshToken: string,
): Promise<AuthTokenResponse> {
  const response = await fetch(`${getApiBaseUrl()}/admin/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = (await response.json()) as { message?: string | string[] }
      if (Array.isArray(data.message)) {
        message = data.message.join('；')
      } else if (data.message) {
        message = data.message
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message || '刷新登录状态失败')
  }

  return response.json() as Promise<AuthTokenResponse>
}
