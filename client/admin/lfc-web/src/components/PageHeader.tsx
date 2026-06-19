import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  loading?: boolean
  onRefresh?: () => void
  actions?: ReactNode
}

export function PageHeader({
  title,
  description,
  loading = false,
  onRefresh,
  actions,
}: PageHeaderProps) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      <div className="page-header-actions">
        {actions}
        {onRefresh && (
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onRefresh}
            disabled={loading}
          >
            刷新
          </button>
        )}
      </div>
    </header>
  )
}
