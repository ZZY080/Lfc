import { useState, type FormEvent } from 'react'
import { updatePost } from '../api/post'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { POST_CATEGORIES } from '../constants/postCategories'
import { PostImageEditor } from './PostImageEditor'
import type { Post } from '../types'
import './CreateUserModal.css'

interface EditPostModalProps {
  post: Post | null
  onClose: () => void
  onSaved: () => void
}

export function EditPostModal({ post, onClose, onSaved }: EditPostModalProps) {
  const { token } = useAuth()
  const toast = useToast()
  const [title, setTitle] = useState(post?.title ?? '')
  const [category, setCategory] = useState(post?.category ?? '校园生活')
  const [content, setContent] = useState(post?.content ?? '')
  const [images, setImages] = useState<string[]>(post?.images ?? [])
  const [location, setLocation] = useState(post?.location ?? '')
  const [isVisible, setIsVisible] = useState(post?.isVisible ?? true)
  const [loading, setLoading] = useState(false)

  if (!post) return null
  const editingPost = post

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setLoading(true)
    try {
      await updatePost(token, editingPost.id, {
        title: title.trim(),
        category,
        content,
        images,
        location: location.trim() || null,
        isVisible,
      })
      toast.success('帖子已更新')
      onSaved()
      onClose()
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : '更新失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <h2>编辑帖子</h2>
          <button type="button" className="modal-close" onClick={onClose}>
            ×
          </button>
        </header>
        <form className="modal-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="field">
            <span>标题</span>
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </label>
          <label className="field">
            <span>分类</span>
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              {POST_CATEGORIES.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>内容</span>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={6}
              required
            />
          </label>

          <PostImageEditor
            images={images}
            onChange={setImages}
            disabled={loading}
          />

          <label className="field">
            <span>定位地址（可选）</span>
            <input
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="如：图书馆门口"
            />
          </label>

          <label className="field field-row">
            <input
              type="checkbox"
              checked={isVisible}
              onChange={(e) => setIsVisible(e.target.checked)}
            />
            <span>公域展示</span>
          </label>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '保存中…' : '保存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
