import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import './ImagePreviewGallery.css'

interface ImagePreviewModalProps {
  images: string[]
  index: number
  onClose: () => void
  onIndexChange: (index: number) => void
}

export function ImagePreviewModal({
  images,
  index,
  onClose,
  onIndexChange,
}: ImagePreviewModalProps) {
  const hasPrev = index > 0
  const hasNext = index < images.length - 1

  const goPrev = useCallback(() => {
    if (hasPrev) onIndexChange(index - 1)
  }, [hasPrev, index, onIndexChange])

  const goNext = useCallback(() => {
    if (hasNext) onIndexChange(index + 1)
  }, [hasNext, index, onIndexChange])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
      if (event.key === 'ArrowLeft') goPrev()
      if (event.key === 'ArrowRight') goNext()
    }

    document.addEventListener('keydown', handleKeyDown)
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [onClose, goPrev, goNext])

  return createPortal(
    <div
      className="image-preview-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="图片预览"
      onClick={onClose}
    >
      <button
        type="button"
        className="image-preview-close"
        aria-label="关闭预览"
        onClick={onClose}
      >
        ×
      </button>

      {images.length > 1 && (
        <div className="image-preview-counter">
          {index + 1} / {images.length}
        </div>
      )}

      {hasPrev && (
        <button
          type="button"
          className="image-preview-nav image-preview-nav-prev"
          aria-label="上一张"
          onClick={(event) => {
            event.stopPropagation()
            goPrev()
          }}
        >
          ‹
        </button>
      )}

      <img
        className="image-preview-full"
        src={images[index]}
        alt=""
        onClick={(event) => event.stopPropagation()}
      />

      {hasNext && (
        <button
          type="button"
          className="image-preview-nav image-preview-nav-next"
          aria-label="下一张"
          onClick={(event) => {
            event.stopPropagation()
            goNext()
          }}
        >
          ›
        </button>
      )}
    </div>,
    document.body,
  )
}

interface ImagePreviewGalleryProps {
  images: string[]
  thumbSize?: 'sm' | 'md' | 'lg'
  className?: string
}

export function ImagePreviewGallery({
  images,
  thumbSize = 'sm',
  className = '',
}: ImagePreviewGalleryProps) {
  const [previewIndex, setPreviewIndex] = useState<number | null>(null)

  if (!images.length) return null

  return (
    <>
      <div className={`image-preview-gallery size-${thumbSize} ${className}`.trim()}>
        {images.map((url, index) => (
          <button
            key={`${url}-${index}`}
            type="button"
            className="image-preview-thumb"
            aria-label={`预览图片 ${index + 1}`}
            onClick={() => setPreviewIndex(index)}
          >
            <img src={url} alt="" loading="lazy" />
          </button>
        ))}
      </div>

      {previewIndex !== null && (
        <ImagePreviewModal
          images={images}
          index={previewIndex}
          onClose={() => setPreviewIndex(null)}
          onIndexChange={setPreviewIndex}
        />
      )}
    </>
  )
}
