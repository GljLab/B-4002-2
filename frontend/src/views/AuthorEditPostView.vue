<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getPostDetail, updateAuthorPost, submitForReview } from '../api/posts'
import { getAdminCategoriesFlat } from '../api/categories'
import { searchKeywords } from '../api/keywords'
import { uploadImage } from '../api/upload'
import type { Category, Keyword, PostDetail } from '../types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const fullscreen = ref(false)
const preview = ref(false)
const lastSavedAt = ref<string | null>(null)
const postData = ref<PostDetail | null>(null)
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const postId = computed(() => Number(route.params.id))

const form = reactive({
  title: '',
  content: '',
  categoryId: null as number | null,
  keywords: [] as string[],
})

const categories = ref<Category[]>([])
const categoriesLoading = ref(false)

async function loadCategories() {
  categoriesLoading.value = true
  try {
    categories.value = await getAdminCategoriesFlat()
  } catch {
    categories.value = []
  } finally {
    categoriesLoading.value = false
  }
}

function isCategoryDisabled(category: Category): boolean {
  return !category.enabled
}

const keywordOptions = ref<Keyword[]>([])
const keywordSearchLoading = ref(false)
let keywordSearchTimer: ReturnType<typeof setTimeout> | null = null

async function handleKeywordSearch(query: string) {
  if (keywordSearchTimer) clearTimeout(keywordSearchTimer)
  keywordSearchTimer = setTimeout(async () => {
    keywordSearchLoading.value = true
    try {
      keywordOptions.value = await searchKeywords(query)
    } catch {
      keywordOptions.value = []
    } finally {
      keywordSearchLoading.value = false
    }
  }, 300)
}

async function loadPost() {
  if (!Number.isFinite(postId.value) || postId.value <= 0) {
    ElMessage.error('文章参数错误')
    router.replace('/author/posts')
    return
  }

  loading.value = true
  try {
    const detail = await getPostDetail(postId.value)
    postData.value = detail
    form.title = detail.title
    form.content = detail.content
    form.categoryId = detail.categoryId
    form.keywords = detail.keywords.map(k => k.name)
  } catch {
    ElMessage.error('文章加载失败')
    router.replace('/author/posts')
  } finally {
    loading.value = false
  }
}

async function handleImageUpload(file: File) {
  try {
    const url = await uploadImage(file)
    form.content += `\n![image](${url})\n`
    ElMessage.success('图片上传成功')
  } catch {
    ElMessage.error('图片上传失败')
  }
  return false
}

function handleImageSelect() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const target = e.target as HTMLInputElement
    const file = target.files?.[0]
    if (file) await handleImageUpload(file)
  }
  input.click()
}

async function saveDraft() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  try {
    await updateAuthorPost(postId.value, {
      title: form.title.trim(),
      content: form.content.trim(),
      categoryId: form.categoryId,
      keywords: form.keywords,
    })
    lastSavedAt.value = dayjs().format('HH:mm:ss')
    ElMessage.success('草稿已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function submitReview() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  submitting.value = true
  try {
    await updateAuthorPost(postId.value, {
      title: form.title.trim(),
      content: form.content.trim(),
      categoryId: form.categoryId,
      keywords: form.keywords,
    })
    await submitForReview(postId.value)
    ElMessage.success('已提交审核')
    router.push('/author/posts')
  } catch {
    ElMessage.error('提交审核失败')
  } finally {
    submitting.value = false
  }
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
}

function togglePreview() {
  preview.value = !preview.value
}

onMounted(() => {
  loadPost()
  loadCategories()
  autoSaveTimer = setInterval(() => {
    if (form.title.trim() || form.content.trim()) {
      saveDraft()
    }
  }, 30000)
})

onUnmounted(() => {
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})
</script>

<template>
  <div class="view-shell author-shell" :class="{ 'editor-fullscreen': fullscreen }">
    <header class="author-header" v-if="!fullscreen">
      <h1>编辑文章</h1>
      <p class="author-subtitle">修改文章内容后保存或提交审核。</p>
    </header>

    <el-alert
      v-if="postData?.status === 'REJECTED' && postData?.rejectionReason"
      type="error"
      :title="'拒绝原因: ' + postData.rejectionReason"
      show-icon
      :closable="false"
      style="margin-bottom: var(--space-3)"
    />

    <el-card
      class="panel-card panel-editor"
      :class="{ 'panel-editor-full': fullscreen }"
      v-loading="loading"
    >
      <template #header>
        <div class="panel-title-wrap">
          <strong class="panel-title">编辑器</strong>
          <div class="editor-toolbar">
            <span v-if="lastSavedAt" class="auto-save-hint">上次自动保存: {{ lastSavedAt }}</span>
            <el-button size="small" @click="handleImageSelect">插入图片</el-button>
            <el-button size="small" @click="togglePreview">
              {{ preview ? '编辑' : '预览' }}
            </el-button>
            <el-button size="small" @click="toggleFullscreen">
              {{ fullscreen ? '退出全屏' : '全屏' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-form class="editor-form" label-position="top" @submit.prevent>
        <el-form-item label="标题">
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
            placeholder="请输入文章标题"
          />
        </el-form-item>

        <el-form-item label="正文">
          <el-input
            v-if="!preview"
            v-model="form.content"
            type="textarea"
            :rows="fullscreen ? 28 : 16"
            placeholder="请输入正文内容"
          />
          <div v-else class="content-preview">
            <div class="preview-content" v-text="form.content" />
          </div>
        </el-form-item>

        <el-form-item label="分类">
          <el-tree-select
            v-model="form.categoryId"
            :data="categories"
            :loading="categoriesLoading"
            :props="{ label: 'name', children: 'children', disabled: isCategoryDisabled }"
            value-key="id"
            node-key="id"
            check-strictly
            filterable
            clearable
            placeholder="请选择分类"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="关键词">
          <el-select
            v-model="form.keywords"
            multiple
            filterable
            allow-create
            default-first-option
            :loading="keywordSearchLoading"
            placeholder="输入或选择关键词"
            style="width: 100%"
            @filter-change="handleKeywordSearch"
          >
            <el-option
              v-for="kw in keywordOptions"
              :key="kw.id"
              :label="kw.name"
              :value="kw.name"
            />
          </el-select>
        </el-form-item>

        <div class="editor-actions">
          <el-button @click="router.push('/author/posts')">返回列表</el-button>
          <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
          <el-button type="primary" @click="submitReview" :loading="submitting">提交审核</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.author-header h1 {
  margin: 0;
  font-size: clamp(28px, 3.4vw, 34px);
  line-height: 1.2;
}

.author-subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-3);
  font-size: 15px;
  line-height: 1.65;
}

.panel-card {
  width: 100%;
  border: 1px solid rgba(180, 201, 228, 0.72);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.panel-editor-full {
  position: fixed;
  inset: 0;
  z-index: 100;
  border-radius: 0;
  margin: 0;
  overflow-y: auto;
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.panel-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--color-text-1);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.auto-save-hint {
  font-size: 12px;
  color: var(--color-text-3);
  margin-right: var(--space-1);
}

.editor-form .el-form-item {
  margin-bottom: var(--space-4);
}

.content-preview {
  min-height: 200px;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fafbfc;
}

.preview-content {
  color: var(--color-text-2);
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.editor-actions {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
  padding-top: var(--space-2);
}

.editor-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: var(--color-surface);
  padding: var(--space-4);
  overflow-y: auto;
}
</style>
