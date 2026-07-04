import { getApiBaseUrl } from './baseUrl'
import { refreshAdminSession } from './refreshSession'
import {
  clearStoredTokens,
  ensureValidAccessToken,
  getStoredRefreshToken,
  setStoredTokens,
} from './tokenStore'

let unauthorizedHandler: (() => void) | null = null
let refreshPromise: Promise<string | null> | null = null

export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler
}

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function performTokenRefresh(): Promise<string | null> {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) return null

  if (!refreshPromise) {
    refreshPromise = refreshAdminSession(refreshToken)
      .then((result) => {
        setStoredTokens(result.accessToken, result.refreshToken)
        return result.accessToken
      })
      .catch(() => {
        clearStoredTokens()
        unauthorizedHandler?.()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
  allowRetry = true,
): Promise<T> {
  const resolvedToken = await ensureValidAccessToken(token)
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }
  if (resolvedToken) {
    headers.set('Authorization', `Bearer ${resolvedToken}`)
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...options,
    headers,
  })

  if (
    response.status === 401 &&
    allowRetry &&
    getStoredRefreshToken()
  ) {
    const newToken = await performTokenRefresh()
    if (newToken) {
      return request<T>(path, options, newToken, false)
    }
  }

  if (response.status === 401 && unauthorizedHandler) {
    unauthorizedHandler()
  }

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
    throw new ApiError(message || '请求失败', response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}
