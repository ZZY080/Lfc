import type { Comment, PaginatedResult } from '../types'
import { request } from './client'

export interface CommentListParams {
  page?: number
  limit?: number
  keyword?: string
  postId?: number
  visibility?: 'all' | 'visible' | 'hidden'
}

function buildQuery(params: object) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

export function fetchComments(token: string, params: CommentListParams = {}) {
  return request<PaginatedResult<Comment>>(
    `/api/admin/comment${buildQuery(params)}`,
    {},
    token,
  )
}

export function updateComment(
  token: string,
  commentId: number,
  data: { content?: string; isVisible?: boolean },
) {
  return request<Comment>(
    `/api/admin/comment/${commentId}`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function deleteComment(token: string, commentId: number) {
  return request<{ message: string }>(
    `/api/admin/comment/${commentId}`,
    { method: 'DELETE' },
    token,
  )
}
