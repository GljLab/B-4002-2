export interface AuthUser {
  id: number
  username: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface CreatePostPayload {
  title: string
  content: string
  categoryId?: number | null
  keywords?: string[]
}

export interface UpdatePostPayload {
  title: string
  content: string
  categoryId?: number | null
  keywords?: string[]
}

export interface PostSummary {
  id: number
  title: string
  excerpt: string
  authorName: string
  categoryId: number | null
  categoryPath: string
  keywords: KeywordItem[]
  createdAt: string
}

export interface PostDetail {
  id: number
  title: string
  content: string
  authorName: string
  categoryId: number | null
  categoryPath: string
  categoryBreadcrumb: BreadcrumbItem[]
  keywords: KeywordItem[]
  createdAt: string
}

export interface BreadcrumbItem {
  id: number
  name: string
  slug: string
}

export interface KeywordItem {
  id: number
  name: string
  usageCount: number
  lastUsedAt: string | null
  archived: boolean
  createdAt: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface ApiError {
  code: string
  message: string
  traceId: string
}

export interface TokenResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
}

export interface Category {
  id: number
  name: string
  slug: string
  parentId: number | null
  parentName: string | null
  enabled: boolean
  sortOrder: number
  postCount: number
  createdAt: string
  updatedAt: string
  children: Category[]
}

export interface Keyword {
  id: number
  name: string
  usageCount: number
  lastUsedAt: string | null
  archived: boolean
  createdAt: string
}

export interface KeywordCloud {
  id: number
  name: string
  usageCount: number
  heatScore: number
  lastUsedAt: string | null
}

export interface CreateCategoryPayload {
  name: string
  slug?: string
  parentId?: number | null
  sortOrder?: number
}

export interface UpdateCategoryPayload {
  name: string
  slug?: string
  parentId?: number | null
  enabled?: boolean
  sortOrder?: number
}

export interface BatchUpdateCategoryPayload {
  postIds: number[]
  categoryId: number
}

export interface BatchAddKeywordsPayload {
  postIds: number[]
  keywords: string[]
}
