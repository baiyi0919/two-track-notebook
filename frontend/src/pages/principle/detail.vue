<template>
  <view class="page" v-if="principle">
    <!-- 原则内容卡片 -->
    <view class="card content-card">
      <view class="principle-header">
        <text class="principle-title">📏 原则详情</text>
        <view class="header-actions">
          <text class="action-edit" @tap="handleEdit">✏️</text>
          <text class="action-delete" @tap="handleDelete">🗑️</text>
        </view>
      </view>

      <text class="principle-content">{{ principle.content }}</text>

      <!-- 标签 -->
      <view v-if="parsedTags.length > 0" class="tag-area">
        <text
          v-for="(tag, index) in parsedTags"
          :key="index"
          class="tag-item"
        >{{ tag }}</text>
      </view>

      <!-- 来源议题 -->
      <view v-if="principle.sourceThreadId" class="source-info">
        <text class="source-label">📎 来源议题：</text>
        <text class="source-link" @tap="goSourceThread">#{{ principle.sourceThreadId }}</text>
      </view>

      <!-- 创建时间 -->
      <view class="meta-info">
        <text class="meta-text">创建于 {{ formatDate(principle.createdAt) }}</text>
      </view>
    </view>

    <!-- 关联任务 -->
    <view class="card task-card">
      <view class="section-header">
        <text class="section-title">📋 关联任务</text>
        <text class="task-count">{{ relatedTasks.length }} 个任务</text>
      </view>

      <view v-if="relatedTasks.length === 0" class="empty-tasks">
        <text class="empty-text">暂无任务关联此原则</text>
      </view>

      <view
        v-else
        v-for="task in relatedTasks"
        :key="task.id"
        class="task-item"
        @tap="goTaskDetail(task.id)"
      >
        <view class="task-info">
          <text class="task-title">{{ task.title }}</text>
          <view class="task-meta">
            <text class="task-status" :class="getStatusClass(task.status)">{{ getStatusText(task.status) }}</text>
            <text v-if="task.budget" class="task-budget">预算 {{ fmtHoursNum(task.budget) }}h</text>
          </view>
        </view>
        <text class="task-arrow">›</text>
      </view>
    </view>

    <!-- 被引用列表（反向链接） -->
    <view v-if="backlinks.length > 0" class="card backlink-card">
      <view class="section-header">
        <text class="section-title">🔗 被引用（{{ backlinks.length }}）</text>
      </view>

      <view
        v-for="bl in backlinks"
        :key="bl.id"
        class="backlink-item"
        @tap="navigateToSource(bl)"
      >
        <text class="backlink-icon">💬</text>
        <view class="backlink-info">
          <text class="backlink-source">消息 #{{ bl.sourceId }}</text>
          <text class="backlink-hint">来自探索议题</text>
        </view>
        <text class="backlink-arrow">→</text>
      </view>
    </view>
  </view>

  <!-- 加载中 -->
  <view v-if="loading" class="loading-state">
    <text class="loading-text">加载中...</text>
  </view>

  <!-- 编辑弹窗 -->
  <view v-if="showEditModal" class="modal-mask" @tap="closeEditModal">
    <view class="modal-content" @tap.stop>
      <view class="modal-header">
        <text class="modal-title">✏️ 编辑原则</text>
        <text class="modal-close" @tap="closeEditModal">✕</text>
      </view>

      <view class="modal-body">
        <view class="form-item">
          <text class="form-label">原则内容 *</text>
          <textarea
            class="form-textarea"
            v-model="editForm.content"
            placeholder="写出一条可执行的行动原则..."
            :maxlength="500"
          />
        </view>

        <view class="form-item">
          <text class="form-label">标签（逗号分隔）</text>
          <input
            class="form-input"
            v-model="editForm.tags"
            placeholder="决策,学习,沟通"
          />
        </view>
      </view>

      <view class="modal-footer">
        <view class="btn btn-cancel" @tap="closeEditModal">取消</view>
        <view class="btn btn-confirm" @tap="handleUpdate">保存</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPrincipleDetail, updatePrinciple, deletePrinciple, getPrincipleRelatedTasks, getBacklinksByTarget } from '@/api'
import { fmtHoursNum } from '@/utils/format'

const principle = ref<any>(null)
const relatedTasks = ref<any[]>([])
const backlinks = ref<any[]>([])
const loading = ref(true)
const showEditModal = ref(false)
const editForm = ref({
  content: '',
  tags: ''
})

const parsedTags = computed(() => {
  if (!principle.value?.tags) return []
  return principle.value.tags.split(',').map((t: string) => t.trim()).filter(Boolean)
})

onMounted(() => {
  loadPrincipleDetail()
})

async function loadPrincipleDetail() {
  loading.value = true
  try {
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const id = currentPage.options?.id

    if (!id) {
      uni.showToast({ title: '缺少原则ID', icon: 'none' })
      return
    }

    const res = await getPrincipleDetail(Number(id))
    principle.value = res.data
    
    // 加载关联任务
    await loadRelatedTasks(Number(id))
    
    // 加载被引用列表（反向链接）
    await loadBacklinks(Number(id))
  } catch (e: any) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadRelatedTasks(principleId: number) {
  try {
    // 直接调用后端接口，获取关联到此原则的任务列表
    const res = await getPrincipleRelatedTasks(principleId)
    relatedTasks.value = res.data || []
  } catch (e) {
    console.error('加载关联任务失败', e)
  }
}

// 加载被引用列表（反向链接）
async function loadBacklinks(principleId: number) {
  try {
    const res: any = await getBacklinksByTarget('PRINCIPLE', principleId)
    backlinks.value = res.data || []
  } catch (e) {
    console.error('加载被引用列表失败', e)
    backlinks.value = []
  }
}

// 跳转到引用此原则的消息所在议题
function navigateToSource(ref: any) {
  if (ref.sourceType === 'MESSAGE' && ref.sourceId) {
    uni.showModal({
      title: '引用来源',
      content: `此原则被消息 #${ref.sourceId} 引用，请到对应的探索议题中查看。`,
      showCancel: false
    })
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}-${d.getDate()}`
}

function getStatusClass(status: number): string {
  return ['status-pending', 'status-completed', 'status-abandoned'][status] || ''
}

function getStatusText(status: number): string {
  return ['进行中', '已完成', '已放弃'][status] || ''
}

function goSourceThread() {
  if (principle.value?.sourceThreadId) {
    uni.navigateTo({ url: `/pages/explore/detail?id=${principle.value.sourceThreadId}` })
  }
}

function goTaskDetail(taskId: number) {
  uni.navigateTo({ url: `/pages/index/detail?id=${taskId}` })
}

function handleEdit() {
  editForm.value = {
    content: principle.value?.content || '',
    tags: principle.value?.tags || ''
  }
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
}

async function handleUpdate() {
  if (!editForm.value.content.trim()) {
    uni.showToast({ title: '请输入原则内容', icon: 'none' })
    return
  }

  try {
    await updatePrinciple(principle.value.id, {
      content: editForm.value.content.trim(),
      tags: editForm.value.tags || undefined
    })
    uni.showToast({ title: '原则已更新', icon: 'success' })
    showEditModal.value = false
    await loadPrincipleDetail()
  } catch (e: any) {
    uni.showToast({ title: e.message || '更新失败', icon: 'none' })
  }
}

function handleDelete() {
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deletePrinciple(principle.value.id)
          uni.showToast({ title: '已删除', icon: 'success' })
          uni.navigateBack()
        } catch (e: any) {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
  padding: 20rpx;
}

.card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.principle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.principle-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
}

.header-actions {
  display: flex;
  gap: 20rpx;
}

.action-edit,
.action-delete {
  font-size: 36rpx;
  padding: 10rpx;
}

.principle-content {
  font-size: 32rpx;
  color: #2D3436;
  line-height: 1.8;
  display: block;
  margin-bottom: 24rpx;
}

.tag-area {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.tag-item {
  padding: 6rpx 20rpx;
  background: rgba(108, 92, 231, 0.1);
  color: #6C5CE7;
  border-radius: 8rpx;
  font-size: 24rpx;
}

.source-info {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
  padding: 16rpx;
  background: #F8F9FA;
  border-radius: 12rpx;
}

.source-label {
  font-size: 26rpx;
  color: #636E72;
}

.source-link {
  font-size: 26rpx;
  color: #0984E3;
  margin-left: 8rpx;
}

.meta-info {
  padding-top: 16rpx;
  border-top: 1rpx solid #F0F0F0;
}

.meta-text {
  font-size: 24rpx;
  color: #B2BEC3;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
}

.task-count {
  font-size: 24rpx;
  color: #6C5CE7;
  background: rgba(108, 92, 231, 0.1);
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}

.empty-tasks {
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #B2BEC3;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F5F6FA;
}

.task-item:last-child {
  border-bottom: none;
}

.task-info {
  flex: 1;
  margin-right: 20rpx;
}

.task-title {
  font-size: 28rpx;
  color: #2D3436;
  display: block;
  margin-bottom: 8rpx;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.task-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.task-status.status-pending {
  background: rgba(253, 203, 110, 0.2);
  color: #E17055;
}

.task-status.status-completed {
  background: rgba(0, 184, 148, 0.2);
  color: #00B894;
}

.task-status.status-abandoned {
  background: rgba(225, 112, 85, 0.2);
  color: #E17055;
}

.task-budget {
  font-size: 22rpx;
  color: #636E72;
}

.task-arrow {
  font-size: 36rpx;
  color: #B2BEC3;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #B2BEC3;
}

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content {
  width: 600rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #F5F6FA;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
}

.modal-close {
  font-size: 36rpx;
  color: #B2BEC3;
  padding: 10rpx;
}

.modal-body {
  padding: 30rpx;
}

.form-item {
  margin-bottom: 30rpx;
}

.form-label {
  display: block;
  font-size: 28rpx;
  color: #636E72;
  margin-bottom: 12rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  border: 1rpx solid #E0E0E0;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #2D3436;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  height: 200rpx;
  border: 1rpx solid #E0E0E0;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: #2D3436;
  box-sizing: border-box;
  line-height: 1.5;
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #F5F6FA;
}

.btn {
  flex: 1;
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 600;
}

.btn-cancel {
  color: #636E72;
  border-right: 1rpx solid #F5F6FA;
}

.btn-confirm {
  color: #6C5CE7;
}

/* === 被引用列表（反向链接）=== */
.backlink-card {
  background: rgba(0, 184, 148, 0.04);
  border-left: 6rpx solid #00B894;
}

.backlink-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  background: rgba(0, 184, 148, 0.06);
  border-radius: 12rpx;
  margin-top: 12rpx;
}

.backlink-item:active {
  background: rgba(0, 184, 148, 0.12);
}

.backlink-icon {
  font-size: 28rpx;
  flex-shrink: 0;
}

.backlink-info {
  flex: 1;
  min-width: 0;
}

.backlink-source {
  font-size: 26rpx;
  color: #2D3436;
  font-weight: 600;
  display: block;
}

.backlink-hint {
  font-size: 20rpx;
  color: #B2BEC3;
  display: block;
  margin-top: 4rpx;
}

.backlink-arrow {
  font-size: 24rpx;
  color: #B2BEC3;
  flex-shrink: 0;
}
</style>
