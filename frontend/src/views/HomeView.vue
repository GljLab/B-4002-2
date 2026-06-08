<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getPublicPosts } from '../api/posts'
import { getPublicCategories } from '../api/categories'
import { getKeywordCloud } from '../api/keywords'
import type { PostSummary, Category, KeywordCloud, PageResponse } from '../types'

const route = useRoute()
const router = useRouter()

const posts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const categories = ref<Category[]>([])
const keywords = ref<KeywordCloud[]>([])
const selectedCategoryId = ref<number | null>(null)
const selectedKeywordId = ref<number | null>(null)

function readQueryParams() {
  const qCat = route.query.categoryId
  const qKw = route.query.keywordId
  selectedCategoryId.value = qCat ? Number(qCat) : null
  selectedKeywordId.value = qKw ? Number(qKw) : null
}

function pushQueryParams() {
  const query: Record<string, string> = {}
  if (selectedCategoryId.value != null) query.categoryId = String(selectedCategoryId.value)
  if (selectedKeywordId.value != null) query.keywordId = String(selectedKeywordId.value)
  router.replace({ path: '/', query })
}

async function loadPosts() {
  loading.value = true
  try {
    const data: PageResponse<PostSummary> = await getPublicPosts(
      page.value,
      size.value,
      selectedCategoryId.value,
      selectedKeywordId.value,
    )
    posts.value = data.items
    total.value = data.total
  } catch {
    ElMessage.error('文章列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getPublicCategories()
  } catch {
    ElMessage.error('分类加载失败')
  }
}

async function loadKeywords() {
  try {
    keywords.value = await getKeywordCloud()
  } catch {
    ElMessage.error('关键词加载失败')
  }
}

async function applyFilter() {
  page.value = 1
  pushQueryParams()
  await loadPosts()
}

function onCategoryChange() {
  applyFilter()
}

function onKeywordChange() {
  applyFilter()
}

function clearFilters() {
  selectedCategoryId.value = null
  selectedKeywordId.value = null
  applyFilter()
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage
  await loadPosts()
}

onMounted(() => {
  readQueryParams()
  loadCategories()
  loadKeywords()
  loadPosts()
})

watch(() => route.query, () => {
  readQueryParams()
  loadPosts()
})
</script>

<template>
  <div class="view-shell home-shell">
    <section class="home-hero">
      <p class="hero-kicker">BLOG</p>
      <h1>最新文章</h1>
      <p class="hero-desc">浏览最新发布内容，快速进入文章详情。</p>
    </section>

    <div class="filter-bar">
      <el-tree-select
        v-model="selectedCategoryId"
        :data="categories"
        :props="{ label: 'name', children: 'children', value: 'id' }"
        value-key="id"
        placeholder="选择分类"
        clearable
        check-strictly
        style="min-width: 200px"
        @change="onCategoryChange"
      />
      <el-select
        v-model="selectedKeywordId"
        placeholder="选择关键词"
        clearable
        style="min-width: 180px"
        @change="onKeywordChange"
      >
        <el-option
          v-for="kw in keywords"
          :key="kw.id"
          :label="kw.name"
          :value="kw.id"
        />
      </el-select>
      <el-button
        v-if="selectedCategoryId != null || selectedKeywordId != null"
        @click="clearFilters"
      >
        清除筛选
      </el-button>
    </div>

    <el-skeleton v-if="loading" class="skeleton-panel" :rows="6" animated />

    <template v-else>
      <el-empty v-if="posts.length === 0" class="home-empty" description="暂无文章" />
      <div v-else class="card-list">
        <el-card
          v-for="(post, index) in posts"
          :key="post.id"
          class="post-card"
          shadow="hover"
          :style="{ '--stagger-index': index }"
        >
          <template #header>
            <router-link :to="`/posts/${post.id}`" class="title-link">
              {{ post.title }}
            </router-link>
          </template>
          <p class="meta">{{ post.authorName }} · {{ dayjs(post.createdAt).format('YYYY-MM-DD HH:mm:ss') }}</p>
          <div class="post-tags">
            <el-tag
              v-if="post.categoryPath"
              class="category-tag"
              size="small"
              @click="selectedCategoryId = post.categoryId; applyFilter()"
            >
              {{ post.categoryPath }}
            </el-tag>
            <el-tag
              v-for="kw in post.keywords"
              :key="kw.id"
              size="small"
              type="info"
              class="keyword-tag"
              @click="selectedKeywordId = kw.id; applyFilter()"
            >
              {{ kw.name }}
            </el-tag>
          </div>
          <p class="excerpt">{{ post.excerpt }}</p>
        </el-card>
      </div>
      <div class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="handlePageChange"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding: var(--space-3) var(--space-4);
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.post-tags {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-wrap: wrap;
  margin-top: var(--space-2);
}

.category-tag {
  cursor: pointer;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.category-tag:hover {
  opacity: 0.75;
}

.keyword-tag {
  cursor: pointer;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.keyword-tag:hover {
  opacity: 0.75;
}
</style>
