import './CreateUserModal.css'

interface ConfirmDialogProps {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
  loading?: boolean
  onClose: () => void
  onConfirm: () => void | Promise<void>
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = '确认',
  cancelLabel = '取消',
  danger = false,
  loading = false,
  onClose,
  onConfirm,
}: ConfirmDialogProps) {
  if (!open) return null

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
        aria-labelledby="confirm-dialog-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <h2 id="confirm-dialog-title">{title}</h2>
          <button type="button" className="modal-close" onClick={handleClose}>
            ×
          </button>
        </header>

        <div className="modal-form">
          <p style={{ margin: 0, fontSize: 14, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            {message}
          </p>
          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={handleClose}
              disabled={loading}
            >
              {cancelLabel}
            </button>
            <button
              type="button"
              className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
              disabled={loading}
              onClick={() => void onConfirm()}
            >
              {loading ? '处理中…' : confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
