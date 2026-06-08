<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getPostDetail } from '../api/posts'
import type { BreadcrumbItem, KeywordItem, PostDetail } from '../types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const post = ref<PostDetail | null>(null)

const postId = computed(() => Number(route.params.id))

async function loadPost() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) {
    ElMessage.error('文章参数错误')
    return
  }

  loading.value = true
  try {
    post.value = await getPostDetail(postId.value)
  } catch {
    ElMessage.error('文章不存在或已删除')
  } finally {
    loading.value = false
  }
}

function goToCategory(item: BreadcrumbItem) {
  router.push({ path: '/', query: { categoryId: String(item.id) } })
}

function goToKeyword(keyword: KeywordItem) {
  router.push({ path: '/', query: { keywordId: String(keyword.id) } })
}

onMounted(loadPost)
</script>

<template>
  <div class="view-shell detail-shell">
    <el-card v-loading="loading" class="detail-card">
      <template v-if="post">
        <el-breadcrumb v-if="post.categoryBreadcrumb.length" separator="/">
          <el-breadcrumb-item
            v-for="item in post.categoryBreadcrumb"
            :key="item.id"
          >
            <a @click.prevent="goToCategory(item)">{{ item.name }}</a>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-divider v-if="post.categoryBreadcrumb.length" />
        <header class="detail-header">
          <h1 class="detail-title">{{ post.title }}</h1>
          <p class="meta">
            <el-avatar :size="36" :src="post.authorAvatar || undefined" class="author-avatar">
              {{ post.authorName?.charAt(0) }}
            </el-avatar>
            <router-link :to="`/authors/${post.authorId}`" class="author-link">作者：{{ post.authorName }}</router-link>
            <span>·</span>
            {{ post.viewCount }} 次阅读
            <span>·</span>
            {{ dayjs(post.createdAt).format('YYYY-MM-DD HH:mm:ss') }}
          </p>
        </header>
        <el-divider />
        <div class="detail-content" style="white-space: pre-wrap">{{ post.content }}</div>
        <el-divider v-if="post.keywords.length" />
        <div v-if="post.keywords.length" class="detail-keywords">
          <el-tag
            v-for="keyword in post.keywords"
            :key="keyword.id"
            class="keyword-tag"
            @click="goToKeyword(keyword)"
          >
            {{ keyword.name }}
          </el-tag>
        </div>
      </template>
      <template v-else>
        <el-empty class="detail-empty" description="未找到该文章" />
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.author-avatar {
  flex-shrink: 0;
}

.author-link {
  color: var(--color-primary);
  text-decoration: none;
  transition: opacity var(--motion-fast) var(--ease-standard);
}

.author-link:hover {
  opacity: 0.75;
}

.keyword-tag {
  cursor: pointer;
  margin: 0 8px 8px 0;
}
</style>
