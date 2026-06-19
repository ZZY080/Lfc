import type { ActivityStatus, PostStatus } from '../types'
import './StatusBadge.css'

type ReviewStatus = ActivityStatus | PostStatus

const STATUS_LABELS: Record<ReviewStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  OFF_SHELF: '已下架',
}

const STATUS_CLASS: Record<ReviewStatus, string> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  OFF_SHELF: 'muted',
}

export function StatusBadge({ status }: { status: ReviewStatus }) {
  return (
    <span className={`status-badge status-pill ${STATUS_CLASS[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  )
}
