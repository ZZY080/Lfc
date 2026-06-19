import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { setUnauthorizedHandler } from '../api/client'
import { useAuth } from '../hooks/useAuth'

export function AuthSessionGuard() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    setUnauthorizedHandler(() => {
      void logout().finally(() => {
        navigate('/login', { replace: true })
      })
    })
    return () => setUnauthorizedHandler(() => {})
  }, [logout, navigate])

  return null
}
