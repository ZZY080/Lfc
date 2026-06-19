import { useRef, useState } from 'react'
import { uploadPostImage } from '../api/upload'
import { ApiError } from '../api/client'
import { ImagePreviewModal } from './ImagePreviewGallery'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../hooks/useToast'
import { MAX_POST_IMAGES } from '../constants/postCategories'
import './PostImageEditor.css'

interface PostImageEditorProps {
  images: string[]
  onChange: (images: string[]) => void
  disabled?: boolean
}

export function PostImageEditor({
  images,
  onChange,
  disabled = false,
}: PostImageEditorProps) {
  const { token } = useAuth()
  const toast = useToast()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [urlInput, setUrlInput] = useState('')
  const [previewIndex, setPreviewIndex] = useState<number | null>(null)

  const canAddMore = images.length < MAX_POST_IMAGES

  function removeImage(index: number) {
    onChange(images.filter((_, i) => i !== index))
  }

  async function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const files = event.target.files
    if (!files?.length || !token) return

    const remaining = MAX_POST_IMAGES - images.length
    const selected = Array.from(files).slice(0, remaining)
    if (selected.length < files.length) {
      toast.error(`最多 ${MAX_POST_IMAGES} 张图片`)
    }

    setUploading(true)
    const uploaded: string[] = []
    try {
      for (const file of selected) {
        const url = await uploadPostImage(token, file)
        uploaded.push(url)
      }
      onChange([...images, ...uploaded])
    } catch (err) {
      if (uploaded.length > 0) {
        onChange([...images, ...uploaded])
      }
      toast.error(err instanceof ApiError ? err.message : '图片上传失败')
    } finally {
      setUploading(false)
      event.target.value = ''
    }
  }

  function handleAddUrl() {
    const url = urlInput.trim()
    if (!url) return
    if (!canAddMore) {
      toast.error(`最多 ${MAX_POST_IMAGES} 张图片`)
      return
    }
    onChange([...images, url])
    setUrlInput('')
  }

  return (
    <div className="post-image-editor">
      <div className="post-image-editor-header">
        <span>图片</span>
        <small>
          {images.length}/{MAX_POST_IMAGES}
        </small>
      </div>

      <div className="post-image-grid">
        {images.map((url, index) => (
          <div key={`${url}-${index}`} className="post-image-item">
            <button
              type="button"
              className="post-image-preview-trigger"
              onClick={() => setPreviewIndex(index)}
              disabled={disabled || uploading}
              aria-label={`预览图片 ${index + 1}`}
            >
              <img src={url} alt="" />
            </button>
            <button
              type="button"
              className="post-image-remove"
              onClick={() => removeImage(index)}
              disabled={disabled || uploading}
              aria-label="移除图片"
            >
              ×
            </button>
          </div>
        ))}

        {canAddMore && (
          <button
            type="button"
            className="post-image-add"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || uploading}
          >
            <span className="post-image-add-icon">+</span>
            {uploading ? '上传中…' : '上传图片'}
          </button>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(event) => void handleFileChange(event)}
      />

      {canAddMore && (
        <div className="post-image-url-input">
          <input
            type="url"
            value={urlInput}
            onChange={(event) => setUrlInput(event.target.value)}
            placeholder="或粘贴图片 URL"
            disabled={disabled || uploading}
          />
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={handleAddUrl}
            disabled={disabled || uploading || !urlInput.trim()}
          >
            添加
          </button>
        </div>
      )}

      {previewIndex !== null && (
        <ImagePreviewModal
          images={images}
          index={previewIndex}
          onClose={() => setPreviewIndex(null)}
          onIndexChange={setPreviewIndex}
        />
      )}
    </div>
  )
}
