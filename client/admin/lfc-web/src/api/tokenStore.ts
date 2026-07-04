const TOKEN_KEY = 'lfc_admin_token'
const REFRESH_KEY = 'lfc_admin_refresh_token'
const REFRESH_BUFFER_SEC = 5 * 60

type TokenListener = (token: string | null) => void
const listeners = new Set<TokenListener>()

function decodeJwtExp(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = JSON.parse(atob(normalized)) as { exp?: number }
    return typeof decoded.exp === 'number' ? decoded.exp : null
  } catch {
    return null
  }
}

export function isAccessTokenExpiringSoon(
  token: string,
  bufferSec = REFRESH_BUFFER_SEC,
): boolean {
  const exp = decodeJwtExp(token)
  if (!exp) return true
  return Date.now() / 1000 >= exp - bufferSec
}

export function getStoredAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function setStoredTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
  listeners.forEach((listener) => listener(accessToken))
}

export function clearStoredTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  listeners.forEach((listener) => listener(null))
}

export function subscribeAccessToken(listener: TokenListener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

let refreshHandler: (() => Promise<string | null>) | null = null

export function setTokenRefreshHandler(handler: () => Promise<string | null>) {
  refreshHandler = handler
}

export async function ensureValidAccessToken(
  explicitToken?: string | null,
): Promise<string | null | undefined> {
  const candidate =
    explicitToken !== undefined ? explicitToken : getStoredAccessToken()

  if (candidate && !isAccessTokenExpiringSoon(candidate)) {
    return candidate
  }

  if (!getStoredRefreshToken() || !refreshHandler) {
    return candidate ?? null
  }

  return refreshHandler()
}
