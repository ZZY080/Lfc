import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteActivity,
  fetchActivity,
  reviewActivity,
  updateActivity,
} from '../api/activity'
import { ApiError } from '../api/client'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ImagePreviewGallery } from '../components/ImagePreviewGallery'
import { RejectReviewModal } from '../components/RejectReviewModal'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { Activity, ActivityStatus } from '../types'
import { formatDateTime, formatFee } from '../utils/format'
import './ActivityDetailPage.css'

const STATUS_OPTIONS: { value: ActivityStatus; label: string }[] = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'OFF_SHELF', label: '已下架' },
]

function displayUserName(user?: {
  realName?: string
  nickname?: string | null
  email?: string
}) {
  return user?.realName || user?.nickname || user?.email || '未知用户'
}

export function ActivityDetailPage() {
  const { activityId: activityIdParam } = useParams()
  const activityId = Number(activityIdParam)
  const navigate = useNavigate()
  const { token } = useAuth()
  const toast = useToast()

  const [detail, setDetail] = useState<Activity | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [acting, setActing] = useState(false)
  const [rejectOpen, setRejectOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const loadDetail = useCallback(async () => {
    if (!token || !activityId) return
    setLoading(true)
    setError('')
    try {
      const data = await fetchActivity(token, activityId)
      setDetail(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载活动详情失败')
    } finally {
      setLoading(false)
    }
  }, [token, activityId])

  useEffect(() => {
    void loadDetail()
  }, [loadDetail])

  async function handleApprove() {
    if (!token || !detail) return
    setActing(true)
    try {
      const updated = await reviewActivity(token, detail.id, { status: 'APPROVED' })
      setDetail(updated)
      toast.success('活动已通过审核')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '审核失败')
    } finally {
      setActing(false)
    }
  }

  async function handleRejectConfirm(reviewComment: string) {
    if (!token || !detail) return
    setActing(true)
    try {
      const updated = await reviewActivity(token, detail.id, {
        status: 'REJECTED',
        reviewComment,
      })
      setDetail(updated)
      toast.success('活动已拒绝')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '审核失败')
      throw err
    } finally {
      setActing(false)
    }
  }

  async function handleStatusChange(status: ActivityStatus) {
    if (!token || !detail || status === detail.status) return
    if (status === 'REJECTED') {
      setRejectOpen(true)
      return
    }
    setActing(true)
    try {
      const updated = await updateActivity(token, detail.id, { status })
      setDetail(updated)
      toast.success('活动状态已更新')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
    } finally {
      setActing(false)
    }
  }

  async function handleDeleteConfirm() {
    if (!token || !detail) return
    setActing(true)
    try {
      await deleteActivity(token, detail.id)
      toast.success('活动已删除')
      navigate('/activities')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    } finally {
      setActing(false)
      setDeleteOpen(false)
    }
  }

  if (!activityId) {
    return <div className="page-state">无效的活动 ID</div>
  }

  if (loading) {
    return <div className="page-state">加载中…</div>
  }

  if (error || !detail) {
    return (
      <div className="page">
        <div className="alert alert-error">{error || '活动不存在'}</div>
        <Link to="/activities" className="btn btn-ghost">
          返回列表
        </Link>
      </div>
    )
  }

  const authorName = displayUserName(detail.author)
  const participants = detail.participants ?? []
  const participantCount = participants.length

  return (
    <div className="page activity-detail-page">
      <header className="page-header">
        <div>
          <Link to="/activities" className="back-link">
            ← 返回活动列表
          </Link>
          <h1>{detail.title}</h1>
          <p className="activity-detail-meta">
            发起人：{authorName}
            {detail.author?.studentId && `（${detail.author.studentId}）`}
            · 提交于 {formatDateTime(detail.createdAt)}
          </p>
        </div>
        <div className="activity-detail-header-actions">
          <StatusBadge status={detail.status} />
          {detail.status === 'PENDING' && (
            <>
              <button
                type="button"
                className="btn btn-success"
                disabled={acting}
                onClick={() => void handleApprove()}
              >
                通过
              </button>
              <button
                type="button"
                className="btn btn-danger"
                disabled={acting}
                onClick={() => setRejectOpen(true)}
              >
                拒绝
              </button>
            </>
          )}
          {detail.status !== 'PENDING' && (
            <select
              className="filter-select"
              value={detail.status}
              disabled={acting}
              onChange={(e) => void handleStatusChange(e.target.value as ActivityStatus)}
            >
              {STATUS_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          )}
          <button
            type="button"
            className="btn btn-danger"
            disabled={acting}
            onClick={() => setDeleteOpen(true)}
          >
            删除
          </button>
        </div>
      </header>

      <div className="activity-detail-summary">
        <div className="summary-card static">
          <strong>{participantCount}</strong>
          <span>已报名</span>
        </div>
        <div className="summary-card static">
          <strong>
            {detail.maxParticipants > 0 ? detail.maxParticipants : '不限'}
          </strong>
          <span>人数上限</span>
        </div>
        <div className="summary-card static">
          <strong>{formatFee(detail.fee)}</strong>
          <span>报名费用</span>
        </div>
      </div>

      {detail.images && detail.images.length > 0 && (
        <ImagePreviewGallery images={detail.images} thumbSize="md" />
      )}

      <dl className="activity-detail-info">
        <div>
          <dt>地点</dt>
          <dd>{detail.location}</dd>
        </div>
        <div>
          <dt>开始时间</dt>
          <dd>{formatDateTime(detail.startTime)}</dd>
        </div>
        <div>
          <dt>结束时间</dt>
          <dd>{formatDateTime(detail.endTime)}</dd>
        </div>
        {detail.latitude != null && detail.longitude != null && (
          <div>
            <dt>坐标</dt>
            <dd>
              {detail.latitude}, {detail.longitude}
            </dd>
          </div>
        )}
      </dl>

      {detail.status === 'REJECTED' && detail.reviewComment && (
        <div className="review-comment">
          <strong>审核意见</strong>
          <p>{detail.reviewComment}</p>
        </div>
      )}

      <section className="activity-detail-section">
        <h2>活动描述</h2>
        <p className="activity-detail-content">{detail.description}</p>
      </section>

      <section className="activity-detail-section">
        <div className="activity-detail-section-head">
          <h2>报名用户</h2>
          <span>{participantCount} 人</span>
        </div>
        {participants.length === 0 ? (
          <div className="page-state">暂无报名用户</div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>学号</th>
                  <th>邮箱</th>
                  <th>报名时间</th>
                </tr>
              </thead>
              <tbody>
                {participants.map((item) => (
                  <tr key={item.id}>
                    <td>{displayUserName(item.user)}</td>
                    <td>{item.user?.studentId ?? '—'}</td>
                    <td className="cell-email">{item.user?.email ?? '—'}</td>
                    <td>{formatDateTime(item.joinedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <RejectReviewModal
        open={rejectOpen}
        title={detail.title}
        onClose={() => setRejectOpen(false)}
        onConfirm={handleRejectConfirm}
      />

      <ConfirmDialog
        open={deleteOpen}
        title="删除活动"
        message={`确定删除活动「${detail.title}」吗？此操作不可恢复。`}
        confirmLabel="确认删除"
        danger
        loading={acting}
        onClose={() => {
          if (!acting) setDeleteOpen(false)
        }}
        onConfirm={() => void handleDeleteConfirm()}
      />
    </div>
  )
}
