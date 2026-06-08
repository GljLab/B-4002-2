import { http } from './http'
import type { PageResponse, PostSummary, PostDetail, ReviewActionPayload, BatchReviewPayload } from '../types'

export async function getPendingPosts(page = 1, size = 20): Promise<PageResponse<PostSummary>> {
  const { data } = await http.get<PageResponse<PostSummary>>('/admin/reviews', { params: { page, size } })
  return data
}

export async function getReviewDetail(id: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/admin/reviews/${id}`)
  return data
}

export async function reviewAction(id: number, payload: ReviewActionPayload): Promise<void> {
  await http.put(`/admin/reviews/${id}`, payload)
}

export async function batchReview(payload: BatchReviewPayload): Promise<void> {
  await http.put('/admin/reviews/batch', payload)
}
