import { useEffect, useState, type FormEvent } from 'react'
import type { User } from '../types'
import './CreateUserModal.css'

interface BanUserModalProps {
  user: User | null
  onClose: () => void
  onConfirm: (reason: string) => Promise<void>
}

export function BanUserModal({ user, onClose, onConfirm }: BanUserModalProps) {
  const [reason, setReason] = useState('违反平台规定')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (user) {
      setReason(user.banReason ?? '违反平台规定')
    }
  }, [user])

  if (!user) return null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = reason.trim()
    if (!trimmed) return
    setLoading(true)
    try {
      await onConfirm(trimmed)
      onClose()
    } finally {
      setLoading(false)
    }
  }

  function handleClose() {
    if (loading) return
    onClose()
  }

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ban-user-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <h2 id="ban-user-title">封禁用户</h2>
          <button type="button" className="modal-close" onClick={handleClose}>
            ×
          </button>
        </header>

        <form className="modal-form" onSubmit={(event) => void handleSubmit(event)}>
          <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted)' }}>
            将封禁用户「{user.realName}」（{user.email}），封禁后该用户将无法登录。
          </p>

          <label className="field">
            <span>封禁原因</span>
            <textarea
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              placeholder="请填写封禁原因"
              rows={4}
              maxLength={500}
              required
              autoFocus
            />
          </label>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={handleClose}
              disabled={loading}
            >
              取消
            </button>
            <button
              type="submit"
              className="btn btn-danger"
              disabled={loading || !reason.trim()}
            >
              {loading ? '提交中…' : '确认封禁'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
