import { http } from './http'
import type { PageResponse, PostSummary, PostDetail, CreatePostPayload, UpdatePostPayload } from '../types'

export async function getPublicPosts(
  page = 1,
  size = 10,
  categoryId?: number | null,
  keywordId?: number | null,
): Promise<PageResponse<PostSummary>> {
  const params: Record<string, unknown> = { page, size }
  if (categoryId != null) params.categoryId = categoryId
  if (keywordId != null) params.keywordId = keywordId
  const { data } = await http.get<PageResponse<PostSummary>>('/posts', { params })
  return data
}

export async function getPostDetail(id: number): Promise<PostDetail> {
  const { data } = await http.get<PostDetail>(`/posts/${id}`)
  return data
}

export async function getMyPosts(): Promise<PostSummary[]> {
  const { data } = await http.get<PostSummary[]>('/admin/posts/mine')
  return data
}

export async function createPost(payload: CreatePostPayload): Promise<PostDetail> {
  const { data } = await http.post<PostDetail>('/admin/posts', payload)
  return data
}

export async function updatePost(id: number, payload: UpdatePostPayload): Promise<PostDetail> {
  const { data } = await http.put<PostDetail>(`/admin/posts/${id}`, payload)
  return data
}

export async function deletePost(id: number): Promise<void> {
  await http.delete(`/admin/posts/${id}`)
}

export async function batchUpdateCategory(postIds: number[], categoryId: number): Promise<void> {
  await http.put('/admin/posts/batch/category', { postIds, categoryId })
}

export async function batchAddKeywords(postIds: number[], keywords: string[]): Promise<void> {
  await http.post('/admin/posts/batch/keywords', { postIds, keywords })
}
