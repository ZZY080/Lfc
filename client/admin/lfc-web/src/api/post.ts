import type {
  PaginatedResult,
  Post,
  PostDetail,
  PostFavoriteRecord,
  PostLikeRecord,
  PostProduct,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
  DeliveryMethod,
  Comment,
  PostStatus,
} from '../types'
import { request } from './client'

export interface PostListParams {
  page?: number
  limit?: number
  keyword?: string
  visibility?: 'all' | 'visible' | 'hidden'
  status?: PostStatus
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

export function fetchPendingPosts(token: string, params: PostListParams = {}) {
  return request<PaginatedResult<Post>>(
    `/admin/post/pending${buildQuery(params)}`,
    {},
    token,
  )
}

export function reviewPost(
  token: string,
  postId: number,
  data: {
    status: Extract<PostStatus, 'APPROVED' | 'REJECTED' | 'OFF_SHELF'>
    reviewComment?: string
  },
) {
  return request<Post>(
    `/admin/post/${postId}/review`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function fetchPosts(token: string, params: PostListParams = {}) {
  return request<PaginatedResult<Post>>(
    `/admin/post${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchPost(token: string, postId: number) {
  return request<Post>(`/admin/post/${postId}`, {}, token)
}

export function fetchPostDetail(token: string, postId: number) {
  return request<PostDetail>(`/admin/post/${postId}/detail`, {}, token)
}

export function fetchPostComments(
  token: string,
  postId: number,
  params: { page?: number; limit?: number } = {},
) {
  return request<PaginatedResult<Comment>>(
    `/admin/post/${postId}/comments${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchPostLikes(
  token: string,
  postId: number,
  params: { page?: number; limit?: number } = {},
) {
  return request<PaginatedResult<PostLikeRecord>>(
    `/admin/post/${postId}/likes${buildQuery(params)}`,
    {},
    token,
  )
}

export function fetchPostFavorites(
  token: string,
  postId: number,
  params: { page?: number; limit?: number } = {},
) {
  return request<PaginatedResult<PostFavoriteRecord>>(
    `/admin/post/${postId}/favorites${buildQuery(params)}`,
    {},
    token,
  )
}

export function removePostLike(token: string, postId: number, likeId: number) {
  return request<{ message: string }>(
    `/admin/post/${postId}/likes/${likeId}`,
    { method: 'DELETE' },
    token,
  )
}

export function removePostFavorite(
  token: string,
  postId: number,
  favoriteId: number,
) {
  return request<{ message: string }>(
    `/admin/post/${postId}/favorites/${favoriteId}`,
    { method: 'DELETE' },
    token,
  )
}

export function updatePostProduct(
  token: string,
  postId: number,
  data: {
    price?: number
    originalPrice?: number | null
    category?: PostProductCategory
    condition?: ProductCondition
    deliveryMethod?: DeliveryMethod
    status?: PostProductStatus
  },
) {
  return request<PostProduct>(
    `/admin/post/${postId}/product`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function deletePostProduct(token: string, postId: number) {
  return request<{ message: string }>(
    `/admin/post/${postId}/product`,
    { method: 'DELETE' },
    token,
  )
}

export function updatePost(
  token: string,
  postId: number,
  data: {
    title?: string
    content?: string
    category?: string
    images?: string[]
    location?: string | null
    latitude?: number | null
    longitude?: number | null
    isVisible?: boolean
    status?: PostStatus
    reviewComment?: string
  },
) {
  return request<Post>(
    `/admin/post/${postId}`,
    { method: 'PATCH', body: JSON.stringify(data) },
    token,
  )
}

export function updatePostVisibility(
  token: string,
  postId: number,
  isVisible: boolean,
) {
  return updatePost(token, postId, { isVisible })
}

export function deletePost(token: string, postId: number) {
  return request<{ message: string }>(
    `/admin/post/${postId}`,
    { method: 'DELETE' },
    token,
  )
}
