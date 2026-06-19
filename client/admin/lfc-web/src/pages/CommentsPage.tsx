import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { deleteComment, fetchComments, updateComment } from '../api/comment'
import { ApiError } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { Pagination } from '../components/Pagination'
import { RowActions } from '../components/RowActions'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type { Comment } from '../types'
import { formatDateTime } from '../utils/format'

type VisibilityFilter = 'all' | 'visible' | 'hidden'

export function CommentsPage() {
  const { token } = useAuth()
  const toast = useToast()
  const [comments, setComments] = useState<Comment[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [limit] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [visibility, setVisibility] = useState<VisibilityFilter>('all')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  const loadComments = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const result = await fetchComments(token, {
        page,
        limit,
        keyword: keyword || undefined,
        visibility,
      })
      setComments(result.items)
      setTotal(result.total)
      setHasMore(result.hasMore)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载评论失败')
    } finally {
      setLoading(false)
    }
  }, [token, page, limit, keyword, visibility])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadComments()
  }, [loadComments])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    setPage(1)
    setKeyword(searchInput.trim())
  }

  async function handleToggleVisibility(comment: Comment) {
    if (!token) return
    setUpdatingId(comment.id)
    try {
      await updateComment(token, comment.id, { isVisible: !comment.isVisible })
      toast.success(comment.isVisible ? '评论已隐藏' : '评论已恢复展示')
      void loadComments()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '操作失败')
    } finally {
      setUpdatingId(null)
    }
  }

  async function handleDelete(comment: Comment) {
    if (!token) return
    if (!window.confirm('确定删除这条评论吗？')) return
    setUpdatingId(comment.id)
    try {
      await deleteComment(token, comment.id)
      toast.success('评论已删除')
      void loadComments()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="评论管理"
        description="审核、隐藏或删除违规评论"
        loading={loading}
        onRefresh={() => void loadComments()}
      />

      <div className="toolbar">
        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="搜索评论内容…"
          />
          <button type="submit" className="btn btn-primary btn-sm">搜索</button>
        </form>
        <div className="filter-tabs">
          {([['all', '全部'], ['visible', '展示中'], ['hidden', '已隐藏']] as const).map(
            ([value, label]) => (
              <button
                key={value}
                type="button"
                className={`filter-tab${visibility === value ? ' active' : ''}`}
                onClick={() => {
                  setPage(1)
                  setVisibility(value)
                }}
              >
                {label}
              </button>
            ),
          )}
        </div>
      </div>

      {loading && <div className="page-state">加载中…</div>}
      {!loading && error && <div className="alert alert-error">{error}</div>}

      {!loading && !error && (
        <div className="manage-list">
          {comments.length === 0 ? (
            <div className="page-state">暂无评论</div>
          ) : (
            comments.map((comment) => {
              const author =
                comment.author?.realName ||
                comment.author?.nickname ||
                comment.author?.email ||
                '未知用户'
              return (
                <article key={comment.id} className="manage-card">
                  <div className="manage-card-header">
                    <div>
                      <p className="manage-meta">
                        {author} · 帖子：
                        <Link to={`/posts/${comment.postId}`}>
                          {comment.post?.title ?? comment.postId}
                        </Link>
                        {' · '}
                        {formatDateTime(comment.createdAt)}
                      </p>
                    </div>
                    <span
                      className={`status-pill ${comment.isVisible ? 'success' : 'danger'}`}
                    >
                      {comment.isVisible ? '展示中' : '已隐藏'}
                    </span>
                  </div>
                  <p className="manage-content">{comment.content}</p>
                  <div className="manage-actions">
                    <RowActions
                      actions={[
                        {
                          label: comment.isVisible ? '隐藏' : '恢复展示',
                          variant: comment.isVisible ? 'danger' : 'primary',
                          disabled: updatingId === comment.id,
                          onClick: () => void handleToggleVisibility(comment),
                        },
                        {
                          label: '删除',
                          variant: 'danger',
                          disabled: updatingId === comment.id,
                          onClick: () => void handleDelete(comment),
                        },
                      ]}
                    />
                  </div>
                </article>
              )
            })
          )}
          {comments.length > 0 && (
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
    </div>
  )
}
