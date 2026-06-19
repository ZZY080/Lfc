import './RowActions.css'

interface Action {
  label: string
  onClick: () => void
  variant?: 'default' | 'primary' | 'danger'
  disabled?: boolean
}

export function RowActions({ actions }: { actions: Action[] }) {
  return (
    <div className="row-actions">
      {actions.map((action) => (
        <button
          key={action.label}
          type="button"
          className={`action-btn${
            action.variant === 'danger'
              ? ' action-btn-danger'
              : action.variant === 'primary'
                ? ' action-btn-primary'
                : ''
          }`}
          disabled={action.disabled}
          onClick={action.onClick}
        >
          {action.label}
        </button>
      ))}
    </div>
  )
}
