import { useState, type FormEvent } from 'react'
import './CreateUserModal.css'

interface RejectReviewModalProps {
  open: boolean
  title: string
  onClose: () => void
  onConfirm: (reviewComment: string) => Promise<void>
}

export function RejectReviewModal({
  open,
  title,
  onClose,
  onConfirm,
}: RejectReviewModalProps) {
  const [reviewComment, setReviewComment] = useState('')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const comment = reviewComment.trim()
    if (!comment) return
    setLoading(true)
    try {
      await onConfirm(comment)
      setReviewComment('')
      onClose()
    } finally {
      setLoading(false)
    }
  }

  function handleClose() {
    if (loading) return
    setReviewComment('')
    onClose()
  }

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reject-review-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <h2 id="reject-review-title">填写审核意见</h2>
          <button type="button" className="modal-close" onClick={handleClose}>
            ×
          </button>
        </header>

        <form className="modal-form" onSubmit={(event) => void handleSubmit(event)}>
          <p style={{ margin: 0, fontSize: 13, color: 'var(--text-muted)' }}>
            拒绝活动「{title}」，意见将通知发起人
          </p>

          <label className="field">
            <span>审核意见</span>
            <textarea
              value={reviewComment}
              onChange={(event) => setReviewComment(event.target.value)}
              placeholder="请说明拒绝原因"
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
              disabled={loading || !reviewComment.trim()}
            >
              {loading ? '提交中…' : '确认拒绝'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
