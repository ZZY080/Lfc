import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import './LoginPage.css'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      await login(email.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('登录失败，请稍后重试')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <span className="login-logo">LFC</span>
          <h1>管理后台</h1>
          <p>请使用管理员账号登录</p>
        </div>

        <form className="login-form" onSubmit={(e) => void handleSubmit(e)}>
          {error && <div className="alert alert-error">{error}</div>}

          <label className="field">
            <span>邮箱</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@lfc.edu"
              required
              autoComplete="email"
            />
          </label>

          <label className="field">
            <span>密码</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码"
              required
              autoComplete="current-password"
            />
          </label>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? '登录中…' : '登录'}
          </button>
        </form>
      </div>
    </div>
  )
}
