<template>
  <view class="page">
    <!-- 顶部灵感快寄入口 -->
    <view class="inspiration-bar" @tap="showInspiration = true">
      <text class="inspiration-icon">💡</text>
      <text class="inspiration-text">灵感快寄 — 心流中捕获想法</text>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <view
        class="filter-item"
        :class="{ active: currentStatus === null }"
        @tap="filterByStatus(null)"
      >
        <text>全部</text>
      </view>
      <view
        class="filter-item"
        :class="{ active: currentStatus === 0 }"
        @tap="filterByStatus(0)"
      >
        <text>活跃</text>
      </view>
      <view
        class="filter-item"
        :class="{ active: currentStatus === 1 }"
        @tap="filterByStatus(1)"
      >
        <text>已总结</text>
      </view>
    </view>

    <!-- 议题列表 -->
    <scroll-view scroll-y class="thread-list" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <view v-if="threadList.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">🔬</text>
        <text class="empty-text">暂无议题，开始探索吧</text>
      </view>

        <view
          v-for="thread in threadList"
          :key="thread.id"
          class="thread-card card"
          @tap="goDetail(thread.id)"
        >
          <view class="thread-header">
            <text class="thread-topic">{{ thread.topic }}</text>
            <view class="thread-actions">
              <view class="action-btn" @tap.stop="handleEdit(thread)">
                <text>✏️</text>
              </view>
              <view class="action-btn delete-btn" @tap.stop="handleDelete(thread)">
                <text>🗑️</text>
              </view>
            </view>
          <view class="status-tag" :class="thread.status === 1 ? 'status-closed' : 'status-pending'">
            {{ thread.status === 1 ? '📋 已总结' : '💬 活跃' }}
          </view>
        </view>

        <text v-if="thread.description" class="thread-desc">
          {{ thread.description.length > 80 ? thread.description.slice(0, 80) + '...' : thread.description }}
        </text>

        <!-- 灵感标记 -->
        <view v-if="thread.topic && thread.topic.startsWith('💡')" class="inspiration-tag">
          <text>灵感快寄</text>
        </view>

        <text class="thread-time">{{ formatTime(thread.createdAt) }}</text>
      </view>
    </scroll-view>

    <!-- 浮动添加按钮 -->
    <view class="fab" @tap="showCreateModal = true">
      <text class="fab-icon">+</text>
    </view>

    <!-- 创建议题弹窗 -->
    <view v-if="showCreateModal" class="modal-mask" @tap="showCreateModal = false">
      <view class="modal-content" @tap.stop>
        <text class="modal-title">新建议题</text>

        <view class="input-group">
          <text class="input-label">议题名称 *</text>
          <input class="input-field" v-model="newThread.topic" placeholder="想探讨什么？" />
        </view>

        <view class="input-group">
          <text class="input-label">描述（选填）</text>
          <textarea
            class="input-field textarea"
            v-model="newThread.description"
            placeholder="背景、困惑、假设..."
            :maxlength="500"
          />
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showCreateModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleCreate" :loading="creating">创建</button>
        </view>
      </view>
    </view>

    <!-- 灵感快寄弹窗 -->
    <view v-if="showInspiration" class="modal-mask" @tap="showInspiration = false">
      <view class="modal-content" @tap.stop>
        <text class="modal-title">💡 灵感快寄</text>
        <text class="modal-hint">快速记录一个想法，会自动存入灵感收件箱</text>

        <view class="input-group">
          <textarea
            class="input-field textarea"
            v-model="inspirationContent"
            placeholder="此刻的想法..."
            :maxlength="500"
            focus
          />
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showInspiration = false">取消</button>
          <button class="modal-btn confirm" @tap="handleInspiration" :loading="sendingInspiration">寄出</button>
        </view>
      </view>
    </view>

    <!-- 编辑议题弹窗 -->
    <view v-if="showEditModal" class="modal-mask" @tap="showEditModal = false">
      <view class="modal-content" @tap.stop>
        <text class="modal-title">编辑议题</text>

        <view class="input-group">
          <text class="input-label">议题名称 *</text>
          <input class="input-field" v-model="editingData.topic" placeholder="想探讨什么？" />
        </view>

        <view class="input-group">
          <text class="input-label">描述（选填）</text>
          <textarea
            class="input-field textarea"
            v-model="editingData.description"
            placeholder="背景、困惑、假设..."
            :maxlength="500"
          />
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showEditModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleUpdate">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getThreadList, createThread, updateThread, deleteThread, quickInspiration } from '@/api'


const threadList = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const currentStatus = ref<number | null>(null)
const showCreateModal = ref(false)
const creating = ref(false)
const showInspiration = ref(false)
const sendingInspiration = ref(false)
const inspirationContent = ref('')

const showEditModal = ref(false)
const editingThread = ref<any>(null)
const editingData = ref({ topic: '', description: '' })

const newThread = ref({
  topic: '',
  description: ''
})

onMounted(() => {
  loadThreads()
})

function filterByStatus(status: number | null) {
  currentStatus.value = status
  loadThreads()
}

async function loadThreads() {
  loading.value = true
  try {
    const res = await getThreadList(currentStatus.value !== null ? currentStatus.value! : undefined)
    threadList.value = res.data || []
  } catch (e) {
    console.error('加载议题失败', e)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadThreads()
}

async function handleCreate() {
  if (!newThread.value.topic.trim()) {
    uni.showToast({ title: '请输入议题名称', icon: 'none' })
    return
  }

  creating.value = true
  try {
    await createThread({
      topic: newThread.value.topic,
      description: newThread.value.description || undefined
    })
    uni.showToast({ title: '创建成功', icon: 'success' })
    showCreateModal.value = false
    newThread.value = { topic: '', description: '' }
    await loadThreads()
  } catch (e) {
    console.error('创建失败', e)
  } finally {
    creating.value = false
  }
}

async function handleInspiration() {
  if (!inspirationContent.value.trim()) {
    uni.showToast({ title: '请输入灵感内容', icon: 'none' })
    return
  }

  sendingInspiration.value = true
  try {
    await quickInspiration(inspirationContent.value)
    uni.showToast({ title: '灵感已寄出 ✨', icon: 'success' })
    showInspiration.value = false
    inspirationContent.value = ''
    await loadThreads()
  } catch (e) {
    console.error('灵感快寄失败', e)
  } finally {
    sendingInspiration.value = false
  }
}

function handleEdit(thread: any) {
  editingThread.value = thread
  editingData.value = { topic: thread.topic, description: thread.description || '' }
  showEditModal.value = true
}

async function handleUpdate() {
  if (!editingData.value.topic.trim()) {
    uni.showToast({ title: '请输入议题名称', icon: 'none' })
    return
  }
  try {
    await updateThread(editingThread.value.id, editingData.value)
    uni.showToast({ title: '更新成功', icon: 'success' })
    showEditModal.value = false
    editingThread.value = null
    await loadThreads()
  } catch (e) {
    console.error('更新失败', e)
  }
}

async function handleDelete(thread: any) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除议题「${thread.topic}」吗？关联的发言也会一并删除。`,
    confirmText: '删除',
    confirmColor: '#E17055',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteThread(thread.id)
          uni.showToast({ title: '已删除', icon: 'success' })
          await loadThreads()
        } catch (e) {
          console.error('删除失败', e)
        }
      }
    }
  })
}

function formatTime(time: string) {
  if (!time) return ''
  return time.slice(0, 16).replace('T', ' ')
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/explore/detail?id=${id}` })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
}

.inspiration-bar {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  padding: 24rpx 30rpx;
  margin: 20rpx 30rpx;
  border-radius: 16rpx;
}

.inspiration-icon {
  font-size: 36rpx;
  margin-right: 16rpx;
}

.inspiration-text {
  font-size: 28rpx;
  color: #FFFFFF;
  font-weight: 500;
}

.filter-bar {
  display: flex;
  padding: 10rpx 30rpx;
  gap: 20rpx;
}

.filter-item {
  padding: 12rpx 30rpx;
  border-radius: 30rpx;
  background: #FFFFFF;
  font-size: 26rpx;
  color: #636E72;
}

.filter-item.active {
  background: #6C5CE7;
  color: #FFFFFF;
}

.thread-list {
  height: calc(100vh - 350rpx);
}

.thread-card {
  position: relative;
}

.thread-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12rpx;
}

.thread-topic {
  font-size: 32rpx;
  font-weight: 600;
  color: #2D3436;
  flex: 1;
  margin-right: 16rpx;
}

.thread-desc {
  font-size: 26rpx;
  color: #636E72;
  line-height: 1.5;
  margin-bottom: 12rpx;
}

.inspiration-tag {
  display: inline-block;
  background: rgba(253, 203, 110, 0.2);
  color: #E17055;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  margin-bottom: 12rpx;
}

.thread-time {
  font-size: 22rpx;
  color: #B2BEC3;
}

/* 浮动按钮 */
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 200rpx;
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(108, 92, 231, 0.4);
  z-index: 100;
}

.fab-icon {
  font-size: 56rpx;
  color: #FFFFFF;
  font-weight: 300;
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: 90%;
  max-height: 85vh;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 40rpx;
  overflow-y: auto;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #2D3436;
  display: block;
  margin-bottom: 10rpx;
}

.modal-hint {
  font-size: 26rpx;
  color: #B2BEC3;
  display: block;
  margin-bottom: 30rpx;
}

.input-group {
  margin-bottom: 28rpx;
}

.input-label {
  font-size: 26rpx;
  color: #636E72;
  margin-bottom: 10rpx;
  display: block;
}

.input-field {
  width: 100%;
  height: 80rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #2D3436;
  box-sizing: border-box;
}

.input-field.textarea {
  height: 200rpx;
  padding: 20rpx 24rpx;
  line-height: 1.5;
}

.modal-btns {
  display: flex;
  gap: 20rpx;
  margin-top: 30rpx;
}

.modal-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  text-align: center;
  border: none;
}

.modal-btn.cancel {
  background: #F5F6FA;
  color: #636E72;
}

.modal-btn.confirm {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
}
</style>
