<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingPosts, getReviewDetail, reviewAction, batchReview } from '../api/reviews'
import type { PostSummary, PostDetail } from '../types'

const pendingPosts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const selectedIds = ref<number[]>([])
const selectedDetail = ref<PostDetail | null>(null)
const detailLoading = ref(false)
const batchRejectDialog = ref(false)
const batchRejectLoading = ref(false)
const batchRejectReason = ref('')

const hasSelection = computed(() => selectedIds.value.length > 0)

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingPosts(page.value, size.value)
    pendingPosts.value = res.items
    total.value = res.total
  } catch {
    ElMessage.error('获取待审核列表失败')
  } finally {
    loading.value = false
  }
}

async function loadDetail(id: number) {
  detailLoading.value = true
  try {
    selectedDetail.value = await getReviewDetail(id)
  } catch {
    ElMessage.error('获取文章详情失败')
    selectedDetail.value = null
  } finally {
    detailLoading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  loadPending()
}

function handleSelectionChange(rows: PostSummary[]) {
  selectedIds.value = rows.map((r) => r.id)
}

function handleRowClick(row: PostSummary) {
  loadDetail(row.id)
}

async function handleApprove(id: number) {
  try {
    await reviewAction(id, { action: 'APPROVE' })
    ElMessage.success('已通过')
    selectedDetail.value = null
    await loadPending()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleReject(id: number) {
  try {
    const result = await ElMessageBox.prompt('请输入拒绝原因', '拒绝文章', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '拒绝原因不能为空',
    })
    const reason = (result as any).value as string
    await reviewAction(id, { action: 'REJECT', reason })
    ElMessage.success('已拒绝')
    selectedDetail.value = null
    await loadPending()
  } catch {
    // cancelled or failed
  }
}

async function handleBatchApprove() {
  if (!hasSelection.value) {
    ElMessage.warning('请先选择文章')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定批量通过选中的 ${selectedIds.value.length} 篇文章吗？`,
      '批量通过',
      { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
    await batchReview({ postIds: selectedIds.value, action: 'APPROVE' })
    ElMessage.success('批量通过成功')
    selectedIds.value = []
    selectedDetail.value = null
    await loadPending()
  } catch {
    // cancelled or failed
  }
}

function openBatchReject() {
  if (!hasSelection.value) {
    ElMessage.warning('请先选择文章')
    return
  }
  batchRejectReason.value = ''
  batchRejectDialog.value = true
}

async function handleBatchReject() {
  if (!batchRejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  batchRejectLoading.value = true
  try {
    await batchReview({
      postIds: selectedIds.value,
      action: 'REJECT',
      reason: batchRejectReason.value.trim(),
    })
    ElMessage.success('批量拒绝成功')
    batchRejectDialog.value = false
    selectedIds.value = []
    selectedDetail.value = null
    await loadPending()
  } catch {
    ElMessage.error('批量拒绝失败')
  } finally {
    batchRejectLoading.value = false
  }
}

onMounted(loadPending)
</script>

<template>
  <div class="view-shell admin-shell">
    <header class="admin-header">
      <h1>审核队列</h1>
      <p class="admin-subtitle">审核待发布的文章，支持单篇与批量操作。</p>
    </header>

    <section class="review-layout">
      <div class="review-left">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-title-wrap">
              <strong class="panel-title">待审核文章</strong>
              <div v-if="hasSelection" class="batch-actions">
                <el-button type="success" size="small" @click="handleBatchApprove">
                  批量通过 ({{ selectedIds.length }})
                </el-button>
                <el-button type="danger" size="small" @click="openBatchReject">
                  批量拒绝 ({{ selectedIds.length }})
                </el-button>
              </div>
            </div>
          </template>

          <el-table
            class="admin-table"
            v-loading="loading"
            :data="pendingPosts"
            stripe
            highlight-current-row
            @selection-change="handleSelectionChange"
            @row-click="handleRowClick"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="authorName" label="作者" width="120" />
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                {{ dayjs(row.createdAt).format('YYYY-MM-DD HH:mm') }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button class="action-btn" type="success" link size="small" @click.stop="handleApprove(row.id)">通过</el-button>
                <el-button class="action-btn" type="danger" link size="small" @click.stop="handleReject(row.id)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="total > size" class="pagination-row" style="margin-top: 16px">
            <el-pagination
              v-model:current-page="page"
              :page-size="size"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </div>

      <div class="review-right">
        <el-card v-if="!selectedDetail" class="panel-card review-empty-card">
          <div class="review-empty">
            <p>点击左侧文章查看详情</p>
          </div>
        </el-card>

        <el-card v-else class="panel-card" v-loading="detailLoading">
          <template #header>
            <div class="panel-title-wrap">
              <strong class="panel-title">{{ selectedDetail.title }}</strong>
              <div class="detail-actions">
                <el-button type="success" size="small" @click="handleApprove(selectedDetail!.id)">通过</el-button>
                <el-button type="danger" size="small" @click="handleReject(selectedDetail!.id)">拒绝</el-button>
              </div>
            </div>
          </template>

          <div class="detail-meta">
            <span>作者：{{ selectedDetail.authorName }}</span>
            <span>分类：{{ selectedDetail.categoryPath || '未分类' }}</span>
            <span>提交时间：{{ dayjs(selectedDetail.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
          </div>

          <el-divider />

          <div class="review-content">
            {{ selectedDetail.content }}
          </div>
        </el-card>
      </div>
    </section>

    <el-dialog v-model="batchRejectDialog" title="批量拒绝" width="480px">
      <p>将拒绝选中的 {{ selectedIds.length }} 篇文章，请输入拒绝原因：</p>
      <el-input
        v-model="batchRejectReason"
        type="textarea"
        :rows="3"
        placeholder="请输入拒绝原因"
        style="margin-top: 12px"
      />
      <template #footer>
        <el-button @click="batchRejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="batchRejectLoading" @click="handleBatchReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  width: 100%;
}

.review-empty-card {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  color: var(--color-text-3);
  font-size: 14px;
}

.batch-actions {
  display: flex;
  gap: var(--space-2);
}

.detail-actions {
  display: flex;
  gap: var(--space-1);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  color: var(--color-text-3);
  font-size: 13px;
}

.review-content {
  color: var(--color-text-2);
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 60vh;
  overflow-y: auto;
}

@media (max-width: 1023px) {
  .review-layout {
    grid-template-columns: 1fr;
  }
}
</style>
