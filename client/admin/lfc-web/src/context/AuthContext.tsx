import {
  useCallback,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import * as authApi from '../api/auth'
import type { AuthUser } from '../types'
import { AuthContext } from '../hooks/useAuth'

const TOKEN_KEY = 'lfc_admin_token'
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
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_KEY),
  )
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser())

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password)
    localStorage.setItem(TOKEN_KEY, result.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    setToken(result.accessToken)
    setUser(result.user)
  }, [])

  const logout = useCallback(async () => {
    if (token) {
      try {
        await authApi.logout(token)
      } catch {
        // 忽略登出接口失败，本地仍清除会话
      }
    }
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
  }, [token])

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
