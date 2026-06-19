import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deletePost,
  deletePostProduct,
  fetchPostComments,
  fetchPostDetail,
  fetchPostFavorites,
  fetchPostLikes,
  removePostFavorite,
  removePostLike,
  updatePostProduct,
} from '../api/post'
import { deleteComment, updateComment } from '../api/comment'
import { ApiError } from '../api/client'
import { EditPostModal } from '../components/EditPostModal'
import { ImagePreviewGallery } from '../components/ImagePreviewGallery'
import { Pagination } from '../components/Pagination'
import { RowActions } from '../components/RowActions'
import {
  DELIVERY_METHOD_LABELS,
  POST_PRODUCT_CATEGORY_LABELS,
  POST_PRODUCT_STATUS_LABELS,
  PRODUCT_CONDITION_LABELS,
} from '../constants/postProduct'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import type {
  Comment,
  PostDetail,
  PostFavoriteRecord,
  PostLikeRecord,
  PostProduct,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
  DeliveryMethod,
} from '../types'
import { formatDateTime, formatFee } from '../utils/format'
import './PostDetailPage.css'

type TabKey = 'comments' | 'likes' | 'favorites' | 'product'

function displayUserName(user?: {
  realName?: string
  nickname?: string | null
  email?: string
}) {
  return user?.realName || user?.nickname || user?.email || '未知用户'
}

export function PostDetailPage() {
  const { postId: postIdParam } = useParams()
  const postId = Number(postIdParam)
  const navigate = useNavigate()
  const { token } = useAuth()
  const toast = useToast()

  const [detail, setDetail] = useState<PostDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<TabKey>('comments')
  const [editing, setEditing] = useState(false)
  const [actingId, setActingId] = useState<number | null>(null)

  const [comments, setComments] = useState<Comment[]>([])
  const [commentPage, setCommentPage] = useState(1)
  const [commentTotal, setCommentTotal] = useState(0)
  const [commentHasMore, setCommentHasMore] = useState(false)

  const [likes, setLikes] = useState<PostLikeRecord[]>([])
  const [likePage, setLikePage] = useState(1)
  const [likeTotal, setLikeTotal] = useState(0)
  const [likeHasMore, setLikeHasMore] = useState(false)

  const [favorites, setFavorites] = useState<PostFavoriteRecord[]>([])
  const [favoritePage, setFavoritePage] = useState(1)
  const [favoriteTotal, setFavoriteTotal] = useState(0)
  const [favoriteHasMore, setFavoriteHasMore] = useState(false)

  const [productForm, setProductForm] = useState<PostProduct | null>(null)
  const [savingProduct, setSavingProduct] = useState(false)

  const limit = 10

  const loadDetail = useCallback(async () => {
    if (!token || !postId) return
    setLoading(true)
    setError('')
    try {
      const data = await fetchPostDetail(token, postId)
      setDetail(data)
      setProductForm(data.product)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '加载帖子详情失败')
    } finally {
      setLoading(false)
    }
  }, [token, postId])

  const loadComments = useCallback(async () => {
    if (!token || !postId) return
    try {
      const result = await fetchPostComments(token, postId, {
        page: commentPage,
        limit,
      })
      setComments(result.items)
      setCommentTotal(result.total)
      setCommentHasMore(result.hasMore)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '加载评论失败')
    }
  }, [token, postId, commentPage, toast])

  const loadLikes = useCallback(async () => {
    if (!token || !postId) return
    try {
      const result = await fetchPostLikes(token, postId, {
        page: likePage,
        limit,
      })
      setLikes(result.items)
      setLikeTotal(result.total)
      setLikeHasMore(result.hasMore)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '加载点赞失败')
    }
  }, [token, postId, likePage, toast])

  const loadFavorites = useCallback(async () => {
    if (!token || !postId) return
    try {
      const result = await fetchPostFavorites(token, postId, {
        page: favoritePage,
        limit,
      })
      setFavorites(result.items)
      setFavoriteTotal(result.total)
      setFavoriteHasMore(result.hasMore)
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '加载收藏失败')
    }
  }, [token, postId, favoritePage, toast])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- data fetch on mount
    void loadDetail()
  }, [loadDetail])

  useEffect(() => {
    if (activeTab === 'comments') void loadComments()
    if (activeTab === 'likes') void loadLikes()
    if (activeTab === 'favorites') void loadFavorites()
  }, [activeTab, loadComments, loadLikes, loadFavorites])

  async function handleDeletePost() {
    if (!token || !detail) return
    if (!window.confirm(`确定删除帖子「${detail.title}」吗？`)) return
    try {
      await deletePost(token, detail.id)
      toast.success('帖子已删除')
      navigate('/posts')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    }
  }

  async function handleCommentVisibility(comment: Comment) {
    if (!token) return
    setActingId(comment.id)
    try {
      await updateComment(token, comment.id, { isVisible: !comment.isVisible })
      toast.success(comment.isVisible ? '评论已隐藏' : '评论已恢复展示')
      void loadComments()
      void loadDetail()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '操作失败')
    } finally {
      setActingId(null)
    }
  }

  async function handleCommentDelete(comment: Comment) {
    if (!token) return
    if (!window.confirm('确定删除这条评论吗？')) return
    setActingId(comment.id)
    try {
      await deleteComment(token, comment.id)
      toast.success('评论已删除')
      void loadComments()
      void loadDetail()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    } finally {
      setActingId(null)
    }
  }

  async function handleRemoveLike(like: PostLikeRecord) {
    if (!token || !detail) return
    if (!window.confirm(`确定移除 ${displayUserName(like.user)} 的点赞吗？`)) return
    setActingId(like.id)
    try {
      await removePostLike(token, detail.id, like.id)
      toast.success('已移除点赞')
      void loadLikes()
      void loadDetail()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '操作失败')
    } finally {
      setActingId(null)
    }
  }

  async function handleRemoveFavorite(record: PostFavoriteRecord) {
    if (!token || !detail) return
    if (!window.confirm(`确定移除 ${displayUserName(record.user)} 的收藏吗？`)) return
    setActingId(record.id)
    try {
      await removePostFavorite(token, detail.id, record.id)
      toast.success('已移除收藏')
      void loadFavorites()
      void loadDetail()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '操作失败')
    } finally {
      setActingId(null)
    }
  }

  async function handleSaveProduct(event: FormEvent) {
    event.preventDefault()
    if (!token || !detail || !productForm) return
    setSavingProduct(true)
    try {
      const updated = await updatePostProduct(token, detail.id, {
        price: Number(productForm.price),
        originalPrice: productForm.originalPrice
          ? Number(productForm.originalPrice)
          : null,
        category: productForm.category,
        condition: productForm.condition,
        deliveryMethod: productForm.deliveryMethod,
        status: productForm.status,
      })
      setProductForm(updated)
      setDetail((prev) => (prev ? { ...prev, product: updated } : prev))
      toast.success('商品信息已更新')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '保存失败')
    } finally {
      setSavingProduct(false)
    }
  }

  async function handleDeleteProduct() {
    if (!token || !detail) return
    if (!window.confirm('确定删除该帖子的商品关联吗？')) return
    setSavingProduct(true)
    try {
      await deletePostProduct(token, detail.id)
      setProductForm(null)
      setDetail((prev) => (prev ? { ...prev, product: null } : prev))
      toast.success('商品关联已删除')
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '删除失败')
    } finally {
      setSavingProduct(false)
    }
  }

  if (!postId) {
    return <div className="page-state">无效的帖子 ID</div>
  }

  if (loading) {
    return <div className="page-state">加载中…</div>
  }

  if (error || !detail) {
    return (
      <div className="page">
        <div className="alert alert-error">{error || '帖子不存在'}</div>
        <Link to="/posts" className="btn btn-ghost">
          返回列表
        </Link>
      </div>
    )
  }

  const authorName = displayUserName(detail.author)

  return (
    <div className="page post-detail-page">
      <header className="page-header">
        <div>
          <Link to="/posts" className="back-link">
            ← 返回笔记列表
          </Link>
          <h1>{detail.title}</h1>
          <p className="post-detail-meta">
            {authorName} · {detail.category} · {formatDateTime(detail.createdAt)}
            {detail.location && ` · ${detail.location}`}
          </p>
        </div>
        <div className="post-detail-header-actions">
          <button type="button" className="btn btn-primary" onClick={() => setEditing(true)}>
            编辑帖子
          </button>
          <button type="button" className="btn btn-danger" onClick={() => void handleDeletePost()}>
            删除帖子
          </button>
        </div>
      </header>

      <div className="post-detail-summary">
        <button
          type="button"
          className={`summary-card${activeTab === 'comments' ? ' active' : ''}`}
          onClick={() => setActiveTab('comments')}
        >
          <strong>{detail.commentCount}</strong>
          <span>评论</span>
        </button>
        <button
          type="button"
          className={`summary-card${activeTab === 'likes' ? ' active' : ''}`}
          onClick={() => setActiveTab('likes')}
        >
          <strong>{detail.likeCount}</strong>
          <span>点赞</span>
        </button>
        <button
          type="button"
          className={`summary-card${activeTab === 'favorites' ? ' active' : ''}`}
          onClick={() => setActiveTab('favorites')}
        >
          <strong>{detail.favoriteCount}</strong>
          <span>收藏</span>
        </button>
        <button
          type="button"
          className={`summary-card${activeTab === 'product' ? ' active' : ''}`}
          onClick={() => setActiveTab('product')}
        >
          <strong>{detail.product ? '有' : '无'}</strong>
          <span>商品</span>
        </button>
        <div className="summary-card static">
          <strong>{detail.viewCount}</strong>
          <span>浏览</span>
        </div>
        <div className="summary-card static">
          <strong>{detail.isVisible ? '展示' : '隐藏'}</strong>
          <span>状态</span>
        </div>
      </div>

      {detail.images && detail.images.length > 0 && (
        <ImagePreviewGallery images={detail.images} thumbSize="lg" />
      )}

      <p className="post-detail-content">{detail.content}</p>

      <section className="post-detail-panel">
        {activeTab === 'comments' && (
          <>
            <h2>评论管理</h2>
            {comments.length === 0 ? (
              <div className="page-state">暂无评论</div>
            ) : (
              <div className="relation-list">
                {comments.map((comment) => (
                  <article key={comment.id} className="relation-card">
                    <div className="relation-card-header">
                      <p>
                        {displayUserName(comment.author)}
                        {comment.parentId && ` · 回复 #${comment.parentId}`}
                        {' · '}
                        {formatDateTime(comment.createdAt)}
                      </p>
                      <span
                        className={`visibility-badge${comment.isVisible ? ' visible' : ' hidden'}`}
                      >
                        {comment.isVisible ? '展示中' : '已隐藏'}
                      </span>
                    </div>
                    <p>{comment.content}</p>
                    <RowActions
                      actions={[
                        {
                          label: comment.isVisible ? '隐藏' : '恢复展示',
                          variant: comment.isVisible ? 'danger' : 'primary',
                          disabled: actingId === comment.id,
                          onClick: () => void handleCommentVisibility(comment),
                        },
                        {
                          label: '删除',
                          variant: 'danger',
                          disabled: actingId === comment.id,
                          onClick: () => void handleCommentDelete(comment),
                        },
                      ]}
                    />
                  </article>
                ))}
              </div>
            )}
            <Pagination
              page={commentPage}
              total={commentTotal}
              limit={limit}
              hasMore={commentHasMore}
              onPageChange={setCommentPage}
            />
          </>
        )}

        {activeTab === 'likes' && (
          <>
            <h2>点赞用户</h2>
            {likes.length === 0 ? (
              <div className="page-state">暂无点赞</div>
            ) : (
              <div className="relation-list">
                {likes.map((like) => (
                  <article key={like.id} className="relation-card relation-row">
                    <div>
                      <strong>{displayUserName(like.user)}</strong>
                      <p className="relation-sub">
                        {like.user.email} · {formatDateTime(like.createdAt)}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      disabled={actingId === like.id}
                      onClick={() => void handleRemoveLike(like)}
                    >
                      移除
                    </button>
                  </article>
                ))}
              </div>
            )}
            <Pagination
              page={likePage}
              total={likeTotal}
              limit={limit}
              hasMore={likeHasMore}
              onPageChange={setLikePage}
            />
          </>
        )}

        {activeTab === 'favorites' && (
          <>
            <h2>收藏用户</h2>
            {favorites.length === 0 ? (
              <div className="page-state">暂无收藏</div>
            ) : (
              <div className="relation-list">
                {favorites.map((record) => (
                  <article key={record.id} className="relation-card relation-row">
                    <div>
                      <strong>{displayUserName(record.user)}</strong>
                      <p className="relation-sub">
                        {record.user.email} · {formatDateTime(record.createdAt)}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      disabled={actingId === record.id}
                      onClick={() => void handleRemoveFavorite(record)}
                    >
                      移除
                    </button>
                  </article>
                ))}
              </div>
            )}
            <Pagination
              page={favoritePage}
              total={favoriteTotal}
              limit={limit}
              hasMore={favoriteHasMore}
              onPageChange={setFavoritePage}
            />
          </>
        )}

        {activeTab === 'product' && (
          <>
            <h2>关联商品</h2>
            {!productForm ? (
              <div className="page-state">该帖子未关联商品</div>
            ) : (
              <form className="product-form" onSubmit={(e) => void handleSaveProduct(e)}>
                <label className="field">
                  <span>售价（元）</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={productForm.price}
                    onChange={(e) =>
                      setProductForm({ ...productForm, price: e.target.value })
                    }
                    required
                  />
                </label>
                <label className="field">
                  <span>原价（元，可选）</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={productForm.originalPrice ?? ''}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        originalPrice: e.target.value || null,
                      })
                    }
                  />
                </label>
                <label className="field">
                  <span>分类</span>
                  <select
                    value={productForm.category}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        category: e.target.value as PostProductCategory,
                      })
                    }
                  >
                    {Object.entries(POST_PRODUCT_CATEGORY_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>成色</span>
                  <select
                    value={productForm.condition}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        condition: e.target.value as ProductCondition,
                      })
                    }
                  >
                    {Object.entries(PRODUCT_CONDITION_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>交易方式</span>
                  <select
                    value={productForm.deliveryMethod}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        deliveryMethod: e.target.value as DeliveryMethod,
                      })
                    }
                  >
                    {Object.entries(DELIVERY_METHOD_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>状态</span>
                  <select
                    value={productForm.status}
                    onChange={(e) =>
                      setProductForm({
                        ...productForm,
                        status: e.target.value as PostProductStatus,
                      })
                    }
                  >
                    {Object.entries(POST_PRODUCT_STATUS_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <p className="product-meta">
                  当前售价 {formatFee(productForm.price)}
                  {productForm.originalPrice &&
                    ` · 原价 ${formatFee(productForm.originalPrice)}`}
                </p>
                <div className="product-form-actions">
                  <button
                    type="button"
                    className="btn btn-danger"
                    disabled={savingProduct}
                    onClick={() => void handleDeleteProduct()}
                  >
                    删除商品关联
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={savingProduct}>
                    {savingProduct ? '保存中…' : '保存商品'}
                  </button>
                </div>
              </form>
            )}
          </>
        )}
      </section>

      <EditPostModal
        key={editing ? detail.id : 'closed'}
        post={editing ? detail : null}
        onClose={() => setEditing(false)}
        onSaved={() => void loadDetail()}
      />
    </div>
  )
}
