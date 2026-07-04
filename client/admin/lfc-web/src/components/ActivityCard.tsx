import { Link } from 'react-router-dom'
import { formatDateTime, formatFee } from '../utils/format'
import { ImagePreviewGallery } from './ImagePreviewGallery'
import { StatusBadge } from './StatusBadge'
import { RowActions } from './RowActions'
import type { Activity, ActivityStatus } from '../types'

interface ActivityCardProps {
  activity: Activity
  showReviewActions?: boolean
  showManageActions?: boolean
  reviewing?: boolean
  onApprove?: () => void
  onReject?: () => void
  onStatusChange?: (status: ActivityStatus) => void
  onDelete?: () => void
}

const STATUS_OPTIONS: { value: ActivityStatus; label: string }[] = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'OFF_SHELF', label: '已下架' },
]

export function ActivityCard({
  activity,
  showReviewActions = false,
  showManageActions = false,
  reviewing = false,
  onApprove,
  onReject,
  onStatusChange,
  onDelete,
}: ActivityCardProps) {
  const authorName =
    activity.author?.realName ||
    activity.author?.nickname ||
    activity.author?.email ||
    '未知用户'

  return (
    <article className="manage-card">
      <div className="manage-card-header">
        <div>
          <h3 className="manage-card-title">
            <Link to={`/activities/${activity.id}`} className="manage-card-title-link">
              {activity.title}
            </Link>
          </h3>
          <p className="manage-meta">
            发起人：{authorName}
            {activity.author?.studentId && `（${activity.author.studentId}）`}
          </p>
        </div>
        <StatusBadge status={activity.status} />
      </div>

      {activity.images && activity.images.length > 0 && (
        <ImagePreviewGallery images={activity.images} thumbSize="sm" />
      )}

      <p className="manage-content">{activity.description}</p>

      {activity.status === 'REJECTED' && activity.reviewComment && (
        <div className="review-comment">
          <strong>审核意见</strong>
          <p>{activity.reviewComment}</p>
        </div>
      )}

      <dl className="manage-details">
        <div>
          <dt>地点</dt>
          <dd>{activity.location}</dd>
        </div>
        <div>
          <dt>开始时间</dt>
          <dd>{formatDateTime(activity.startTime)}</dd>
        </div>
        <div>
          <dt>结束时间</dt>
          <dd>{formatDateTime(activity.endTime)}</dd>
        </div>
        <div>
          <dt>人数上限</dt>
          <dd>{activity.maxParticipants > 0 ? activity.maxParticipants : '不限'}</dd>
        </div>
        <div>
          <dt>费用</dt>
          <dd>{formatFee(activity.fee)}</dd>
        </div>
        <div>
          <dt>提交时间</dt>
          <dd>{formatDateTime(activity.createdAt)}</dd>
        </div>
      </dl>

      {showReviewActions && (
        <div className="manage-actions">
          <button type="button" className="btn btn-success" disabled={reviewing} onClick={onApprove}>
            通过
          </button>
          <button type="button" className="btn btn-danger" disabled={reviewing} onClick={onReject}>
            拒绝
          </button>
        </div>
      )}

      {showManageActions && (
        <div className="manage-actions spread">
          <select
            className="filter-select"
            value={activity.status}
            disabled={reviewing}
            onChange={(e) => onStatusChange?.(e.target.value as ActivityStatus)}
          >
            {STATUS_OPTIONS.map((item) => (
              <option key={item.value} value={item.value}>{item.label}</option>
            ))}
          </select>
          <RowActions
            actions={[
              {
                label: '删除',
                variant: 'danger',
                onClick: () => onDelete?.(),
                disabled: reviewing,
              },
            ]}
          />
        </div>
      )}
    </article>
  )
}
