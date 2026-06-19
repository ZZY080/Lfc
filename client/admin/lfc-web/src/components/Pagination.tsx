import './Pagination.css'

interface PaginationProps {
  page: number
  total: number
  limit: number
  hasMore: boolean
  onPageChange: (page: number) => void
}

export function Pagination({
  page,
  total,
  limit,
  hasMore,
  onPageChange,
}: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / limit))

  return (
    <div className="pagination">
      <span className="pagination-info">
        共 {total} 条，第 {page} / {totalPages} 页
      </span>
      <div className="pagination-actions">
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          上一页
        </button>
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          disabled={!hasMore && page >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          下一页
        </button>
      </div>
    </div>
  )
}
