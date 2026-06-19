import { useState, type FormEvent } from 'react'
import { updateUser } from '../api/user'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { User, UserRole } from '../types'
import './CreateUserModal.css'

interface EditUserModalProps {
  user: User | null
  onClose: () => void
  onSaved: () => void
}

export function EditUserModal({ user, onClose, onSaved }: EditUserModalProps) {
  const { token } = useAuth()
  const toast = useToast()
  const [email, setEmail] = useState(user?.email ?? '')
  const [studentId, setStudentId] = useState(user?.studentId ?? '')
  const [realName, setRealName] = useState(user?.realName ?? '')
  const [nickname, setNickname] = useState(user?.nickname ?? '')
  const [role, setRole] = useState<UserRole>(user?.role ?? 'CONSUMER')
  const [loading, setLoading] = useState(false)

  if (!user) return null
  const editingUser = user

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setLoading(true)
    try {
      await updateUser(token, editingUser.id, {
        email: email.trim(),
        studentId: studentId.trim(),
        realName: realName.trim(),
        nickname: nickname.trim() || null,
        role,
      })
      toast.success('用户信息已更新')
      onSaved()
      onClose()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <h2>编辑用户</h2>
          <button type="button" className="modal-close" onClick={onClose}>
            ×
          </button>
        </header>
        <form className="modal-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="field">
            <span>邮箱</span>
            <input value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="field">
            <span>学号</span>
            <input value={studentId} onChange={(e) => setStudentId(e.target.value)} required />
          </label>
          <label className="field">
            <span>姓名</span>
            <input value={realName} onChange={(e) => setRealName(e.target.value)} required />
          </label>
          <label className="field">
            <span>昵称</span>
            <input value={nickname} onChange={(e) => setNickname(e.target.value)} />
          </label>
          <label className="field">
            <span>角色</span>
            <select value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
              <option value="CONSUMER">普通用户</option>
              <option value="ADMIN">管理员</option>
            </select>
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '保存中…' : '保存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
