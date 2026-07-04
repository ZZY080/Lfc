import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import * as authApi from '../api/auth'
import { performTokenRefresh } from '../api/client'
import {
  clearStoredTokens,
  getStoredAccessToken,
  getStoredRefreshToken,
  isAccessTokenExpiringSoon,
  setStoredTokens,
  setTokenRefreshHandler,
  subscribeAccessToken,
} from '../api/tokenStore'
import type { AuthUser } from '../types'
import { AuthContext } from '../hooks/useAuth'

const USER_KEY = 'lfc_admin_user'

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredAccessToken())
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser())

  useEffect(() => {
    setTokenRefreshHandler(() => performTokenRefresh())
    return subscribeAccessToken(setToken)
  }, [])

  useEffect(() => {
    if (!token || !getStoredRefreshToken()) return

    const timer = window.setInterval(() => {
      if (token && isAccessTokenExpiringSoon(token)) {
        void performTokenRefresh()
      }
    }, 60_000)

    if (isAccessTokenExpiringSoon(token)) {
      void performTokenRefresh()
    }

    return () => window.clearInterval(timer)
  }, [token])

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password)
    setStoredTokens(result.accessToken, result.refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    setToken(result.accessToken)
    setUser(result.user)
  }, [])

  const logout = useCallback(async () => {
    const currentToken = getStoredAccessToken()
    if (currentToken) {
      try {
        await authApi.logout(currentToken)
      } catch {
        // 忽略登出接口失败，本地仍清除会话
      }
    }
    clearStoredTokens()
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      login,
      logout,
    }),
    [user, token, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
