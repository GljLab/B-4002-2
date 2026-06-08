<script setup lang="ts">
import { ref, watch } from 'vue'
import { getAdminCategoriesFlat } from '../api/categories'
import { searchKeywords } from '../api/keywords'
import type { Category, Keyword } from '../types'

const props = withDefaults(
  defineProps<{
    title: string
    content: string
    categoryId: number | null
    keywords: string[]
    textareaRows?: number
    loading?: boolean
    submitText?: string
  }>(),
  {
    textareaRows: 10,
    loading: false,
    submitText: '提交',
  },
)

const emit = defineEmits<{
  'update:title': [value: string]
  'update:content': [value: string]
  'update:categoryId': [value: number | null]
  'update:keywords': [value: string[]]
  submit: []
}>()

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

loadCategories()

function isCategoryDisabled(category: Category): boolean {
  return !category.enabled
}

function buildCategoryLabel(category: Category): string {
  const indent = category.parentName ? `${category.parentName} / ` : ''
  const suffix = category.postCount > 0 ? ` (${category.postCount})` : ''
  return `${indent}${category.name}${suffix}`
}

const keywordOptions = ref<Keyword[]>([])
const keywordSearchLoading = ref(false)
let keywordSearchTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.keywords,
  () => {
    if (props.keywords.length > 0 && keywordOptions.value.length === 0) {
      searchKeywords('')
    }
  },
  { immediate: true },
)

async function handleKeywordSearch(query: string) {
  if (keywordSearchTimer) {
    clearTimeout(keywordSearchTimer)
  }
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
</script>

<template>
  <el-form class="editor-form" label-position="top" @submit.prevent="$emit('submit')">
    <el-form-item class="editor-form-item" label="标题">
      <el-input
        class="editor-input"
        :model-value="title"
        maxlength="200"
        show-word-limit
        placeholder="请输入文章标题"
        @update:model-value="$emit('update:title', $event)"
      />
    </el-form-item>

    <el-form-item class="editor-form-item" label="正文">
      <el-input
        class="editor-textarea"
        :model-value="content"
        type="textarea"
        :rows="textareaRows"
        placeholder="请输入正文内容"
        @update:model-value="$emit('update:content', $event)"
      />
    </el-form-item>

    <el-form-item class="editor-form-item" label="分类">
      <el-tree-select
        :model-value="categoryId"
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
        @update:model-value="$emit('update:categoryId', $event ?? null)"
      >
        <template #default="{ data: node }">
          <span :class="{ 'category-disabled': !node.enabled }">
            {{ buildCategoryLabel(node) }}
          </span>
        </template>
      </el-tree-select>
    </el-form-item>

    <el-form-item class="editor-form-item" label="关键词">
      <el-select
        :model-value="keywords"
        multiple
        filterable
        allow-create
        default-first-option
        :loading="keywordSearchLoading"
        placeholder="输入或选择关键词"
        style="width: 100%"
        @update:model-value="$emit('update:keywords', $event)"
        @filter-change="handleKeywordSearch"
      >
        <el-option
          v-for="kw in keywordOptions"
          :key="kw.id"
          :label="kw.name"
          :value="kw.name"
        >
          <span>{{ kw.name }}</span>
          <span style="float: right; color: var(--el-text-color-secondary); font-size: 12px">
            {{ kw.usageCount }} 次引用
          </span>
        </el-option>
      </el-select>
    </el-form-item>

    <el-button class="editor-submit-btn" type="primary" :loading="loading" @click="$emit('submit')">
      {{ submitText }}
    </el-button>
  </el-form>
</template>

<style scoped>
.category-disabled {
  color: var(--el-text-color-disabled);
}
</style>
