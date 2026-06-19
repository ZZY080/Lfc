import { useState, type FormEvent } from 'react'
import { createUser } from '../api/user'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { UserRole } from '../types'
import './CreateUserModal.css'

interface CreateUserModalProps {
  open: boolean
  onClose: () => void
  onCreated: () => void
}

export function CreateUserModal({
  open,
  onClose,
  onCreated,
}: CreateUserModalProps) {
  const { token } = useAuth()
  const toast = useToast()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [studentId, setStudentId] = useState('')
  const [realName, setRealName] = useState('')
  const [role, setRole] = useState<UserRole>('CONSUMER')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setLoading(true)
    try {
      await createUser(token, {
        email: email.trim(),
        password,
        studentId: studentId.trim(),
        realName: realName.trim(),
        role,
      })
      toast.success(role === 'ADMIN' ? '管理员创建成功' : '用户创建成功')
      setEmail('')
      setPassword('')
      setStudentId('')
      setRealName('')
      setRole('CONSUMER')
      onCreated()
      onClose()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '创建失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card"
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-user-title"
      >
        <header className="modal-header">
          <h2 id="create-user-title">创建账号</h2>
          <button type="button" className="modal-close" onClick={onClose}>
            ×
          </button>
        </header>

        <form className="modal-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="field">
            <span>邮箱</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>

          <label className="field">
            <span>密码</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={6}
              required
            />
          </label>

          <label className="field">
            <span>学号</span>
            <input
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              required
            />
          </label>

          <label className="field">
            <span>姓名</span>
            <input
              value={realName}
              onChange={(e) => setRealName(e.target.value)}
              required
            />
          </label>

          <label className="field">
            <span>角色</span>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as UserRole)}
            >
              <option value="CONSUMER">普通用户</option>
              <option value="ADMIN">管理员</option>
            </select>
          </label>

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '创建中…' : '创建'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
