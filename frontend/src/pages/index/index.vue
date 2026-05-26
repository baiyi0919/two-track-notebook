<template>
  <view class="page">
    <!-- 顶部统计 -->
    <view class="stats-bar">
      <view class="stat-item">
        <text class="stat-num">{{ pendingCount }}</text>
        <text class="stat-label">进行中</text>
      </view>
      <view class="stat-item">
        <text class="stat-num done">{{ doneCount }}</text>
        <text class="stat-label">已完成</text>
      </view>
      <view class="stat-item">
        <text class="stat-num budget">{{ totalBudget }}</text>
        <text class="stat-label">总预算(h)</text>
      </view>
      <view v-if="abandonedCount > 0" class="stat-item">
        <text class="stat-num abandoned">{{ abandonedCount }}</text>
        <text class="stat-label">已放弃</text>
      </view>
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
        <text>进行中</text>
      </view>
      <view
        class="filter-item"
        :class="{ active: currentStatus === 1 }"
        @tap="filterByStatus(1)"
      >
        <text>已完成</text>
      </view>
      <view
        class="filter-item"
        :class="{ active: currentStatus === 2 }"
        @tap="filterByStatus(2)"
      >
        <text>已放弃</text>
      </view>

      <!-- 排序按钮 -->
      <view class="sort-btn" @tap="showSortPicker = true">
        <text class="sort-icon">⇅</text>
        <text class="sort-text">{{ currentSortLabel }}</text>
      </view>
    </view>

    <!-- 排序选择弹窗 -->
    <view v-if="showSortPicker" class="modal-mask" @tap="showSortPicker = false">
      <view class="sort-picker" @tap.stop>
        <text class="sort-picker-title">排序方式</text>

        <view
          v-for="item in sortOptions"
          :key="item.value"
          class="sort-option"
          :class="{ active: sortBy === item.value && sortOrder === item.order }"
          @tap="applySort(item)"
        >
          <text class="sort-option-text">{{ item.label }}</text>
          <text v-if="sortBy === item.value && sortOrder === item.order" class="sort-option-check">✓</text>
        </view>
      </view>
    </view>

    <!-- 任务列表 -->
    <scroll-view scroll-y class="task-list" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <view v-if="taskList.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无任务，点击右下角添加</text>
      </view>

      <view
        v-for="task in taskList"
        :key="task.id"
        class="task-card card"
        @tap="goDetail(task.id)"
      >
        <view class="task-header">
          <text class="task-title" :class="{ 'line-through': task.status === 1 }">
            {{ task.title }}
          </text>
          <view class="status-tag" :class="task.status === 1 ? 'status-done' : task.status === 2 ? 'status-abandoned' : 'status-pending'">
            {{ task.status === 1 ? '✓ 完成' : task.status === 2 ? '✗ 放弃' : '● 进行中' }}
          </view>
        </view>

        <!-- 锚点 -->
        <view v-if="task.anchorText" class="anchor-area">
          <text class="anchor-icon">⚓</text>
          <text class="anchor-text">{{ task.anchorText }}</text>
        </view>

        <!-- 预算/实际 -->
        <view v-if="task.budget || task.actualTime" class="time-info">
          <text v-if="task.budget" class="time-item">预算 {{ fmtHours(task.budget) }}</text>
          <text v-if="task.actualTime" class="time-item actual">实际 {{ fmtHours(task.actualTime) }}</text>
        </view>

        <!-- 操作按钮 -->
        <view class="task-actions">
          <view class="action-btn" @tap.stop="toggleStatus(task)">
            <text>{{ task.status === 1 ? '↩ 恢复' : '✓ 完成' }}</text>
          </view>
          <view class="action-btn danger" @tap.stop="confirmDelete(task.id)">
            <text>删除</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 浮动添加按钮 -->
    <view class="fab" @tap="showCreateModal = true">
      <text class="fab-icon">+</text>
    </view>

    <!-- 心流模式入口 -->
    <view class="flow-fab" @tap="goFlow">
      <text class="flow-fab-icon">⚡</text>
    </view>

    <!-- 创建任务弹窗 -->
    <view v-if="showCreateModal" class="modal-mask" @tap="showCreateModal = false">
      <view class="modal-content" @tap.stop>
        <text class="modal-title">新建任务</text>

        <view class="input-group">
          <text class="input-label">任务名称 *</text>
          <input class="input-field" v-model="newTask.title" placeholder="做什么？" />
        </view>

        <view class="input-group">
          <text class="input-label">现实锚点（为什么做）</text>
          <textarea
            class="input-field textarea"
            v-model="newTask.anchorText"
            placeholder="这件事对什么有意义？"
            :maxlength="200"
          />
        </view>

        <view class="input-group">
          <text class="input-label">预算时间（小时）</text>
          <input
            class="input-field"
            v-model="newTask.budget"
            type="digit"
            placeholder="预计花多少小时"
          />
        </view>

        <view class="input-group">
          <text class="input-label">描述（选填）</text>
          <textarea
            class="input-field textarea"
            v-model="newTask.description"
            placeholder="补充说明"
            :maxlength="500"
          />
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showCreateModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleCreate" :loading="creating">创建</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTaskList, createTask, deleteTask, toggleTaskStatus } from '@/api'
import { fmtHours } from '@/utils/format'

const taskList = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const currentStatus = ref<number | null>(null)
const showCreateModal = ref(false)
const creating = ref(false)
const showSortPicker = ref(false)
const sortBy = ref('createdAt')
const sortOrder = ref('desc')

const sortOptions = [
  { label: '创建时间 (最新优先)', value: 'createdAt', order: 'desc' },
  { label: '创建时间 (最早优先)', value: 'createdAt', order: 'asc' },
  { label: '截止日期 (最近优先)', value: 'dueDate', order: 'asc' },
  { label: '截止日期 (最远优先)', value: 'dueDate', order: 'desc' },
  { label: '预算时间 (多→少)', value: 'budget', order: 'desc' },
  { label: '预算时间 (少→多)', value: 'budget', order: 'asc' },
  { label: '实际用时 (多→少)', value: 'actualTime', order: 'desc' },
  { label: '实际用时 (少→多)', value: 'actualTime', order: 'asc' },
  { label: '状态 (进行中优先)', value: 'status', order: 'asc' },
  { label: '状态 (已完成优先)', value: 'status', order: 'desc' },
]

const currentSortLabel = computed(() => {
  const match = sortOptions.find(o => o.value === sortBy.value && o.order === sortOrder.value)
  return match ? match.label.replace(/\s*\(.*\)/, '') : '排序'
})

const newTask = ref({
  title: '',
  anchorText: '',
  budget: '',
  description: ''
})

const pendingCount = computed(() => taskList.value.filter(t => t.status === 0).length)
const doneCount = computed(() => taskList.value.filter(t => t.status === 1).length)
const abandonedCount = computed(() => taskList.value.filter(t => t.status === 2).length)
const totalBudget = computed(() => taskList.value.filter(t => t.status !== 2).reduce((sum, t) => sum + (t.budget || 0), 0))

onMounted(() => {
  loadTasks()
})

function filterByStatus(status: number | null) {
  currentStatus.value = status
  loadTasks()
}

function applySort(item: { value: string; order: string }) {
  sortBy.value = item.value
  sortOrder.value = item.order
  showSortPicker.value = false
  loadTasks()
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTaskList(
      currentStatus.value !== null ? currentStatus.value! : undefined,
      sortBy.value,
      sortOrder.value
    )
    taskList.value = res.data || []
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadTasks()
}

async function toggleStatus(task: any) {
  try {
    const newStatus = task.status === 1 ? 0 : 1
    await toggleTaskStatus(task.id, newStatus)
    uni.showToast({ title: newStatus === 1 ? '已完成' : '已恢复', icon: 'success' })
    await loadTasks()
  } catch (e) {
    console.error('状态切换失败', e)
  }
}

function confirmDelete(id: number) {
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复，确定？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteTask(id)
          uni.showToast({ title: '已删除', icon: 'success' })
          await loadTasks()
        } catch (e) {
          console.error('删除失败', e)
        }
      }
    }
  })
}

async function handleCreate() {
  if (!newTask.value.title.trim()) {
    uni.showToast({ title: '请输入任务名称', icon: 'none' })
    return
  }

  creating.value = true
  try {
    await createTask({
      title: newTask.value.title,
      anchorText: newTask.value.anchorText || undefined,
      budget: newTask.value.budget ? Number(newTask.value.budget) : undefined,
      description: newTask.value.description || undefined
    })
    uni.showToast({ title: '创建成功', icon: 'success' })
    showCreateModal.value = false
    // 重置表单
    newTask.value = { title: '', anchorText: '', budget: '', description: '' }
    await loadTasks()
  } catch (e) {
    console.error('创建失败', e)
  } finally {
    creating.value = false
  }
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/index/detail?id=${id}` })
}

function goFlow() {
  uni.navigateTo({ url: '/pages/index/flow' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
}

.stats-bar {
  display: flex;
  justify-content: space-around;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  padding: 40rpx 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 48rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.stat-num.done {
  color: #00E676;
}

.stat-num.abandoned {
  color: #FDCB6E;
}

.stat-num.budget {
  font-size: 40rpx;
}

.stat-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
}

.filter-bar {
  display: flex;
  padding: 20rpx 30rpx;
  gap: 20rpx;
  align-items: center;
}

.sort-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 12rpx 20rpx;
  border-radius: 30rpx;
  background: #FFFFFF;
  font-size: 24rpx;
  color: #636E72;
}

.sort-icon {
  font-size: 26rpx;
}

.sort-text {
  font-size: 24rpx;
  color: #6C5CE7;
  font-weight: 500;
}

.sort-picker {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx;
  z-index: 201;
}

.sort-picker-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
  display: block;
  margin-bottom: 30rpx;
}

.sort-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 20rpx;
  border-bottom: 1rpx solid #F0F0F0;
}

.sort-option.active {
  background: rgba(108, 92, 231, 0.06);
  border-radius: 12rpx;
}

.sort-option-text {
  font-size: 28rpx;
  color: #2D3436;
}

.sort-option.active .sort-option-text {
  color: #6C5CE7;
  font-weight: 600;
}

.sort-option-check {
  font-size: 32rpx;
  color: #6C5CE7;
  font-weight: 700;
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

.task-list {
  height: calc(100vh - 380rpx);
}

.task-card {
  position: relative;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.task-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #2D3436;
  flex: 1;
}

.task-title.line-through {
  text-decoration: line-through;
  color: #B2BEC3;
}

.anchor-area {
  display: flex;
  align-items: flex-start;
  background: rgba(108, 92, 231, 0.06);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
}

.anchor-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.anchor-text {
  font-size: 26rpx;
  color: #6C5CE7;
  flex: 1;
}

.time-info {
  display: flex;
  gap: 30rpx;
  margin-bottom: 16rpx;
}

.time-item {
  font-size: 24rpx;
  color: #636E72;
}

.time-item.actual {
  color: #E17055;
}

.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  border-top: 1rpx solid #F0F0F0;
  padding-top: 20rpx;
}

.action-btn {
  padding: 8rpx 24rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  color: #6C5CE7;
  background: rgba(108, 92, 231, 0.08);
}

.action-btn.danger {
  color: #E17055;
  background: rgba(225, 112, 85, 0.08);
}

/* 放弃状态标签 */
.status-abandoned {
  background: rgba(225, 112, 85, 0.1);
  color: #E17055;
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

.flow-fab {
  position: fixed;
  right: 40rpx;
  bottom: 340rpx;
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1A1A2E, #2D2D44);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(26, 26, 46, 0.5);
  z-index: 100;
  border: 2rpx solid rgba(253, 203, 110, 0.3);
}

.flow-fab-icon {
  font-size: 44rpx;
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
  height: 160rpx;
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
