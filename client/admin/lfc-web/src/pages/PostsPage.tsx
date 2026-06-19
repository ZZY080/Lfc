import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  deletePost,
  fetchPosts,
  reviewPost,
  updatePost,
} from '../api/post'
import { ApiError } from '../api/client'
import { Pagination } from '../components/Pagination'
import { EditPostModal } from '../components/EditPostModal'
import { PageHeader } from '../components/PageHeader'
import { PostCard } from '../components/PostCard'
import { RejectReviewModal } from '../components/RejectReviewModal'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { Post, PostStatus } from '../types'

type StatusFilter = PostStatus | 'all'

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'OFF_SHELF', label: '已下架' },
]

export function PostsPage() {
  const { token } = useAuth()
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const initialStatus =
    (searchParams.get('status') as StatusFilter | null) ?? 'all'
  const [posts, setPosts] = useState<Post[]>([])
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
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [editingPost, setEditingPost] = useState<Post | null>(null)
  const [rejectTarget, setRejectTarget] = useState<{
    id: number
    title: string
    mode: 'review' | 'status'
  } | null>(null)

  const loadPosts = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchPosts(token, {
        page,
        limit,
        keyword: keyword || undefined,
        status: statusFilter === 'all' ? undefined : statusFilter,
      })
      setPosts(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载笔记列表失败')
    } finally {
      setLoading(false)
    }
  }, [token, page, limit, keyword, statusFilter])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadPosts()
  }, [loadPosts])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(1)
    setKeyword(searchInput.trim())
  }

  async function handleReview(
    id: number,
    nextStatus: Extract<PostStatus, 'APPROVED' | 'REJECTED'>,
    reviewComment?: string,
  ) {
    if (!token) return
    setUpdatingId(id)
    try {
      const updated = await reviewPost(token, id, {
        status: nextStatus,
        reviewComment,
      })
      setPosts((prev) =>
        statusFilter === 'PENDING'
          ? prev.filter((item) => item.id !== id)
          : prev.map((item) => (item.id === id ? { ...item, ...updated } : item)),
      )
      toast.success(nextStatus === 'APPROVED' ? '已通过审核' : '已拒绝')
      if (statusFilter === 'PENDING') {
        setTotal((t) => Math.max(0, t - 1))
      }
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '审核操作失败')
    } finally {
      setUpdatingId(null)
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
    setUpdatingId(id)
    try {
      const updated = await updatePost(token, id, {
        status: 'REJECTED',
        reviewComment,
      })
      setPosts((prev) =>
        prev.map((item) => (item.id === id ? { ...item, ...updated } : item)),
      )
      toast.success('笔记已拒绝')
      setRejectTarget(null)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
      void loadPosts()
    } finally {
      setUpdatingId(null)
    }
  }

  async function handleStatusChange(id: number, status: PostStatus) {
    if (status === 'REJECTED') {
      const post = posts.find((item) => item.id === id)
      setRejectTarget({
        id,
        title: post?.title ?? '',
        mode: 'status',
      })
      return
    }
    if (!token) return
    setUpdatingId(id)
    try {
      const updated =
        status === 'APPROVED' || status === 'OFF_SHELF'
          ? await reviewPost(token, id, { status })
          : await updatePost(token, id, { status })
      setPosts((prev) =>
        prev.map((item) => (item.id === id ? { ...item, ...updated } : item)),
      )
      toast.success('笔记状态已更新')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
      void loadPosts()
    } finally {
      setUpdatingId(null)
    }
  }

  async function handleDelete(id: number, title: string) {
    if (!token) return
    if (!window.confirm(`确定删除笔记「${title}」吗？`)) return
    setUpdatingId(id)
    try {
      await deletePost(token, id)
      setPosts((prev) => prev.filter((item) => item.id !== id))
      setTotal((t) => Math.max(0, t - 1))
      toast.success('笔记已删除')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="笔记管理"
        description="审核笔记并通过、拒绝或下架，管理关联数据"
        loading={loading}
        onRefresh={() => void loadPosts()}
      />

      <div className="toolbar">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="搜索标题、内容…"
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
          {posts.length === 0 ? (
            <div className="page-state empty-state">
              <p>暂无笔记</p>
            </div>
          ) : (
            posts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                showReviewActions={post.status === 'PENDING'}
                showManageActions={post.status !== 'PENDING'}
                reviewing={updatingId === post.id}
                onApprove={() => void handleReview(post.id, 'APPROVED')}
                onReject={() =>
                  setRejectTarget({
                    id: post.id,
                    title: post.title,
                    mode: 'review',
                  })
                }
                onStatusChange={(status) => void handleStatusChange(post.id, status)}
                onEdit={() => setEditingPost(post)}
                onDelete={() => void handleDelete(post.id, post.title)}
              />
            ))
          )}

          {posts.length > 0 && (
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

      <EditPostModal
        key={editingPost?.id ?? 'closed'}
        post={editingPost}
        onClose={() => setEditingPost(null)}
        onSaved={() => void loadPosts()}
      />

      <RejectReviewModal
        open={rejectTarget !== null}
        title={rejectTarget?.title ?? ''}
        onClose={() => setRejectTarget(null)}
        onConfirm={handleRejectConfirm}
      />
    </div>
  )
}
