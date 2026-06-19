import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { deleteActivity, fetchAllActivities, reviewActivity, updateActivity } from '../api/activity'
import { ApiError } from '../api/client'
import { ActivityCard } from '../components/ActivityCard'
import { PageHeader } from '../components/PageHeader'
import { Pagination } from '../components/Pagination'
import { RejectReviewModal } from '../components/RejectReviewModal'
import { useToast } from '../hooks/useToast'
import { useAuth } from '../hooks/useAuth'
import type { Activity, ActivityStatus } from '../types'

type StatusFilter = ActivityStatus | 'all'

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'OFF_SHELF', label: '已下架' },
]

export function ActivitiesPage() {
  const { token } = useAuth()
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const initialStatus =
    (searchParams.get('status') as StatusFilter | null) ?? 'all'
  const [activities, setActivities] = useState<Activity[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [limit] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>(
    STATUS_TABS.some((item) => item.value === initialStatus)
      ? initialStatus
      : 'all',
  )
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reviewingId, setReviewingId] = useState<number | null>(null)
  const [rejectTarget, setRejectTarget] = useState<{
    id: number
    title: string
    mode: 'review' | 'status'
  } | null>(null)

  const loadActivities = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchAllActivities(token, {
        page,
        limit,
        keyword: keyword || undefined,
        status: statusFilter === 'all' ? undefined : statusFilter,
      })
      setActivities(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载活动列表失败')
    } finally {
      setLoading(false)
    }
  }, [token, page, limit, keyword, statusFilter])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadActivities()
  }, [loadActivities])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(1)
    setKeyword(searchInput.trim())
  }

  async function handleReview(
    id: number,
    status: 'APPROVED' | 'REJECTED',
    reviewComment?: string,
  ) {
    if (!token) return
    setReviewingId(id)
    try {
      const updated = await reviewActivity(token, id, { status, reviewComment })
      setActivities((prev) =>
        statusFilter === 'PENDING'
          ? prev.filter((item) => item.id !== id)
          : prev.map((item) =>
              item.id === id
                ? {
                    ...item,
                    status: updated.status,
                    reviewComment: updated.reviewComment,
                  }
                : item,
            ),
      )
      if (statusFilter === 'PENDING') {
        setTotal((t) => Math.max(0, t - 1))
      }
      toast.success(status === 'APPROVED' ? '已通过审核' : '已拒绝')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '审核操作失败')
    } finally {
      setReviewingId(null)
    }
  }

  async function handleRejectConfirm(reviewComment: string) {
    if (!token || !rejectTarget) return
    const { id, mode } = rejectTarget
    if (mode === 'review') {
      await handleReview(id, 'REJECTED', reviewComment)
      setRejectTarget(null)
      return
    }
    setReviewingId(id)
    try {
      const updated = await updateActivity(token, id, {
        status: 'REJECTED',
        reviewComment,
      })
      setActivities((prev) =>
        prev.map((item) =>
          item.id === id
            ? {
                ...item,
                status: updated.status,
                reviewComment: updated.reviewComment,
              }
            : item,
        ),
      )
      toast.success('活动已拒绝')
      setRejectTarget(null)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
      void loadActivities()
    } finally {
      setReviewingId(null)
    }
  }

  async function handleStatusChange(id: number, status: ActivityStatus) {
    if (status === 'REJECTED') {
      const activity = activities.find((item) => item.id === id)
      setRejectTarget({
        id,
        title: activity?.title ?? '',
        mode: 'status',
      })
      return
    }
    if (!token) return
    setReviewingId(id)
    try {
      const updated = await updateActivity(token, id, { status })
      setActivities((prev) =>
        prev.map((item) =>
          item.id === id
            ? {
                ...item,
                status: updated.status,
                reviewComment: updated.reviewComment,
              }
            : item,
        ),
      )
      toast.success('活动状态已更新')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
      void loadActivities()
    } finally {
      setReviewingId(null)
    }
  }

  async function handleDelete(id: number, title: string) {
    if (!token) return
    if (!window.confirm(`确定删除活动「${title}」吗？`)) return
    setReviewingId(id)
    try {
      await deleteActivity(token, id)
      setActivities((prev) => prev.filter((item) => item.id !== id))
      setTotal((t) => Math.max(0, t - 1))
      toast.success('活动已删除')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    } finally {
      setReviewingId(null)
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="活动管理"
        description="审核活动并通过、拒绝或下架，管理全部活动"
        loading={loading}
        onRefresh={() => void loadActivities()}
      />

      <div className="toolbar">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="搜索标题、描述、地点…"
          />
          <button type="submit" className="btn btn-primary btn-sm">
            搜索
          </button>
        </form>

        <div className="filter-tabs">
          {STATUS_TABS.map((item) => (
            <button
              key={item.value}
              type="button"
              className={`filter-tab${statusFilter === item.value ? ' active' : ''}`}
              onClick={() => {
                setPage(1)
                setStatusFilter(item.value)
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {loading && <div className="page-state">加载中…</div>}

      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && (
        <div className="manage-list">
          {activities.length === 0 ? (
            <div className="page-state empty-state">
              <p>暂无活动</p>
            </div>
          ) : (
            activities.map((activity) => (
              <ActivityCard
                key={activity.id}
                activity={activity}
                showReviewActions={activity.status === 'PENDING'}
                showManageActions={activity.status !== 'PENDING'}
                reviewing={reviewingId === activity.id}
                onApprove={() => void handleReview(activity.id, 'APPROVED')}
                onReject={() =>
                  setRejectTarget({
                    id: activity.id,
                    title: activity.title,
                    mode: 'review',
                  })
                }
                onStatusChange={(status) => void handleStatusChange(activity.id, status)}
                onDelete={() => void handleDelete(activity.id, activity.title)}
              />
            ))
          )}

          {activities.length > 0 && (
            <Pagination
              page={page}
              total={total}
              limit={limit}
              hasMore={hasMore}
              onPageChange={setPage}
            />
          )}
        </div>
      )}

      <RejectReviewModal
        open={rejectTarget !== null}
        title={rejectTarget?.title ?? ''}
        onClose={() => setRejectTarget(null)}
        onConfirm={handleRejectConfirm}
      />
    </div>
  )
}
