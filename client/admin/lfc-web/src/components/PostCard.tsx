import { Link } from 'react-router-dom'
import { formatDateTime } from '../utils/format'
import { ImagePreviewGallery } from './ImagePreviewGallery'
import { StatusBadge } from './StatusBadge'
import { RowActions } from './RowActions'
import type { Post, PostStatus } from '../types'

interface PostCardProps {
  post: Post
  showReviewActions?: boolean
  showManageActions?: boolean
  reviewing?: boolean
  onApprove?: () => void
  onReject?: () => void
  onStatusChange?: (status: PostStatus) => void
  onEdit?: () => void
  onDelete?: () => void
}

const STATUS_OPTIONS: { value: PostStatus; label: string }[] = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'OFF_SHELF', label: '已下架' },
]

export function PostCard({
  post,
  showReviewActions = false,
  showManageActions = false,
  reviewing = false,
  onApprove,
  onReject,
  onStatusChange,
  onEdit,
  onDelete,
}: PostCardProps) {
  const authorName =
    post.author?.realName ||
    post.author?.nickname ||
    post.author?.email ||
    '未知用户'

  return (
    <article className="manage-card">
      <div className="manage-card-header">
        <div>
          <h3 className="manage-card-title">{post.title}</h3>
          <p className="manage-meta">
            {authorName} · {post.category} · {formatDateTime(post.createdAt)}
          </p>
        </div>
        <StatusBadge status={post.status} />
      </div>

      {post.status === 'REJECTED' && post.reviewComment && (
        <div className="review-comment">
          <strong>审核意见</strong>
          <p>{post.reviewComment}</p>
        </div>
      )}

      {post.images && post.images.length > 0 && (
        <ImagePreviewGallery images={post.images} thumbSize="sm" />
      )}

      <p className="manage-content clamped">{post.content}</p>

      <div className="manage-stats">
        <span>👍 {post.likeCount}</span>
        <span>⭐ {post.favoriteCount}</span>
        <span>💬 {post.commentCount}</span>
        <span>👁 {post.viewCount}</span>
      </div>

      {showReviewActions && (
        <div className="manage-actions">
          <button
            type="button"
            className="btn btn-success"
            disabled={reviewing}
            onClick={onApprove}
          >
            通过
          </button>
          <button
            type="button"
            className="btn btn-danger"
            disabled={reviewing}
            onClick={onReject}
          >
            拒绝
          </button>
        </div>
      )}

      {showManageActions && (
        <div className="manage-actions spread">
          <Link to={`/posts/${post.id}`} className="btn btn-ghost btn-sm">
            详情
          </Link>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={reviewing}
            onClick={onEdit}
          >
            编辑
          </button>
          <select
            className="filter-select"
            value={post.status}
            disabled={reviewing}
            onChange={(e) => onStatusChange?.(e.target.value as PostStatus)}
          >
            {STATUS_OPTIONS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
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
