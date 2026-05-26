<template>
  <view class="flow-page">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar"></view>

    <!-- 未选择任务时：任务选择界面 -->
    <view v-if="!currentTask" class="select-view">
      <text class="select-title">选择一个任务进入心流</text>
      <text class="select-subtitle">专注当下，锚定意义</text>

      <scroll-view scroll-y class="task-select-list">
        <view v-if="pendingTasks.length === 0" class="empty-hint">
          <text class="empty-text">没有进行中的任务</text>
          <text class="empty-sub">先去创建一个任务吧</text>
        </view>

        <view
          v-for="task in pendingTasks"
          :key="task.id"
          class="task-select-item"
          @tap="enterFlow(task)"
        >
          <text class="task-select-title">{{ task.title }}</text>
          <view v-if="task.anchorText" class="task-select-anchor">
            <text class="anchor-icon">⚓</text>
            <text class="anchor-text">{{ task.anchorText }}</text>
          </view>
          <view class="task-select-meta">
            <text v-if="task.budget" class="meta-item">预算 {{ fmtHours(task.budget) }}</text>
            <text v-if="task.dueDate" class="meta-item" :class="{ overdue: isOverdue(task.dueDate) }">
              截止 {{ task.dueDate }}
            </text>
          </view>
        </view>
      </scroll-view>

      <view class="exit-btn" @tap="exitFlow">
        <text class="exit-text">退出心流</text>
      </view>
    </view>

    <!-- 心流模式主界面 -->
    <view v-else class="flow-view">
      <!-- 倒计时圆环 -->
      <view class="timer-section">
        <view class="timer-ring">
          <view class="timer-progress" :style="timerProgressStyle">
            <view class="timer-inner">
              <text class="timer-time">{{ displayTime }}</text>
              <text class="timer-label">{{ timerRunning ? '专注中' : (timerPaused ? '已暂停' : '准备开始') }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 当前任务信息 -->
      <view class="task-info">
        <text class="flow-task-title">{{ currentTask.title }}</text>
        <view v-if="currentTask.anchorText" class="flow-anchor">
          <text class="flow-anchor-icon">⚓</text>
          <text class="flow-anchor-text">{{ currentTask.anchorText }}</text>
        </view>
      </view>

      <!-- 控制按钮 -->
      <view class="control-bar">
        <view v-if="!timerRunning && !timerPaused" class="ctrl-btn start" @tap="startTimer">
          <text class="ctrl-icon">▶</text>
          <text class="ctrl-text">开始专注</text>
        </view>
        <view v-if="timerRunning" class="ctrl-btn pause" @tap="pauseTimer">
          <text class="ctrl-icon">⏸</text>
          <text class="ctrl-text">暂停</text>
        </view>
        <view v-if="timerPaused" class="ctrl-btn resume" @tap="resumeTimer">
          <text class="ctrl-icon">▶</text>
          <text class="ctrl-text">继续</text>
        </view>
        <view v-if="timerRunning || timerPaused" class="ctrl-btn stop" @tap="stopTimer">
          <text class="ctrl-icon">⏹</text>
          <text class="ctrl-text">结束</text>
        </view>
      </view>

      <!-- 已用时间统计 -->
      <view v-if="elapsedSeconds > 0" class="elapsed-info">
        <text class="elapsed-text">已专注 {{ formatElapsed }}</text>
      </view>

      <!-- 灵感快寄悬浮按钮 -->
      <view class="inspiration-fab" @tap="openInspiration">
        <text class="fab-icon">💡</text>
        <text class="fab-label">灵感快寄</text>
      </view>

      <!-- 退出按钮 -->
      <view class="flow-exit" @tap="confirmExit">
        <text class="flow-exit-text">退出心流</text>
      </view>
    </view>

    <!-- 灵感快寄弹窗 -->
    <view v-if="showInspirationModal" class="modal-mask" @tap="showInspirationModal = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">💡 灵感快寄</text>
          <view class="modal-close" @tap="showInspirationModal = false">
            <text>✕</text>
          </view>
        </view>
        <text class="modal-hint">捕捉脑海中闪过的念头，稍后到探索轨深入</text>
        <textarea
          class="inspiration-input"
          v-model="inspirationText"
          placeholder="这个想法是什么？"
          :maxlength="500"
          :focus="showInspirationModal"
        />
        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showInspirationModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleInspiration" :loading="sendingInspiration">寄出</button>
        </view>
      </view>
    </view>

    <!-- 结束专注弹窗 -->
    <view v-if="showEndModal" class="modal-mask" @tap="showEndModal = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">🎉 专注结束</text>
          <view class="modal-close" @tap="showEndModal = false">
            <text>✕</text>
          </view>
        </view>
        <text class="end-summary">本次专注 {{ formatElapsed }}</text>
        <view class="input-group">
          <text class="input-label">记录实际用时（小时）</text>
          <input
            class="input-field"
            v-model="endActualTime"
            type="digit"
            placeholder="本次实际用了几小时"
          />
        </view>
        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showEndModal = false">跳过</button>
          <button class="modal-btn confirm" @tap="handleEndFocus">记录并退出</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getTaskList, quickInspiration, createAttentionLog } from '@/api'
import { fmtHours } from '@/utils/format'

const pendingTasks = ref<any[]>([])
const currentTask = ref<any>(null)

// 倒计时相关
const budgetMinutes = ref(0) // 预算分钟数
const remainingSeconds = ref(0)
const elapsedSeconds = ref(0)
const timerRunning = ref(false)
const timerPaused = ref(false)
let timerInterval: ReturnType<typeof setInterval> | null = null

// 灵感快寄
const showInspirationModal = ref(false)
const inspirationText = ref('')
const sendingInspiration = ref(false)

// 结束弹窗
const showEndModal = ref(false)
const endActualTime = ref('')

// 计算属性
const displayTime = computed(() => {
  const totalSec = Math.max(0, remainingSeconds.value)
  const h = Math.floor(totalSec / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return `${m}:${String(s).padStart(2, '0')}`
})

const formatElapsed = computed(() => {
  const sec = elapsedSeconds.value
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  if (h > 0) return `${h}小时${m}分钟`
  return `${m}分钟`
})

const timerProgressStyle = computed(() => {
  if (budgetMinutes.value <= 0) return {}
  const total = budgetMinutes.value * 60
  const progress = Math.max(0, Math.min(1, remainingSeconds.value / total))
  const angle = progress * 360
  return {
    background: `conic-gradient(#FFFFFF ${angle}deg, rgba(255,255,255,0.15) ${angle}deg)`
  }
})

function isOverdue(dateStr: string) {
  return new Date(dateStr) < new Date(new Date().toDateString())
}

function getTodayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  loadPendingTasks()
})

onUnmounted(() => {
  clearTimer()
})

async function loadPendingTasks() {
  try {
    const res = await getTaskList(0) // 只取进行中的任务
    pendingTasks.value = res.data || []
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

function enterFlow(task: any) {
  currentTask.value = task
  budgetMinutes.value = Math.round((task.budget || 1) * 60) // budget是小时，转分钟
  remainingSeconds.value = budgetMinutes.value * 60 // 转秒
  elapsedSeconds.value = 0
  timerRunning.value = false
  timerPaused.value = false
}

function startTimer() {
  timerRunning.value = true
  timerPaused.value = false
  startInterval()
}

function pauseTimer() {
  timerPaused.value = true
  timerRunning.value = false
  clearTimer()
}

function resumeTimer() {
  timerPaused.value = false
  timerRunning.value = true
  startInterval()
}

function stopTimer() {
  timerRunning.value = false
  timerPaused.value = false
  clearTimer()
  // 显示结束弹窗
  endActualTime.value = elapsedSeconds.value > 0
    ? String(Math.round(elapsedSeconds.value / 3600 * 10) / 10)
    : ''
  showEndModal.value = true
}

function startInterval() {
  clearTimer()
  timerInterval = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    }
    elapsedSeconds.value++
    // 时间用完提醒
    if (remainingSeconds.value === 0 && budgetMinutes.value > 0) {
      timerRunning.value = false
      clearTimer()
      uni.vibrateLong()
      uni.showModal({
        title: '⏰ 预算时间已用完',
        content: '你的注意力预算已耗尽，可以选择继续或结束',
        confirmText: '继续专注',
        cancelText: '结束',
        success: (res) => {
          if (res.confirm) {
            // 继续但不再倒计时，变成正计时
            remainingSeconds.value = 0
            timerRunning.value = true
            startInterval()
          } else {
            stopTimer()
          }
        }
      })
    }
  }, 1000)
}

function clearTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

// 灵感快寄
function openInspiration() {
  inspirationText.value = ''
  showInspirationModal.value = true
  // 暂停计时
  if (timerRunning.value) {
    pauseTimer()
  }
}

async function handleInspiration() {
  if (!inspirationText.value.trim()) {
    uni.showToast({ title: '请输入灵感内容', icon: 'none' })
    return
  }
  sendingInspiration.value = true
  try {
    await quickInspiration(inspirationText.value)
    uni.showToast({ title: '灵感已寄到探索轨', icon: 'success' })
    showInspirationModal.value = false
    inspirationText.value = ''
    // 自动恢复计时
    if (timerPaused.value) {
      resumeTimer()
    }
  } catch (e) {
    console.error('灵感快寄失败', e)
    uni.showToast({ title: '寄送失败', icon: 'none' })
  } finally {
    sendingInspiration.value = false
  }
}

// 结束专注
async function handleEndFocus() {
  const actual = endActualTime.value ? Number(endActualTime.value) : 0
  if (actual > 0 && currentTask.value) {
    try {
      await createAttentionLog({
        taskId: currentTask.value.id,
        logDate: getTodayStr(),
        actualTime: actual
      })
    } catch (e) {
      console.error('记录用时失败', e)
    }
  }
  showEndModal.value = false
  exitFlow()
}

function confirmExit() {
  if (elapsedSeconds.value > 0) {
    uni.showModal({
      title: '确认退出心流？',
      content: '你已专注了一段时间，退出将结束本次专注',
      success: (res) => {
        if (res.confirm) {
          stopTimer()
        }
      }
    })
  } else {
    exitFlow()
  }
}

function exitFlow() {
  clearTimer()
  currentTask.value = null
  timerRunning.value = false
  timerPaused.value = false
  remainingSeconds.value = 0
  elapsedSeconds.value = 0
  uni.navigateBack()
}
</script>

<style scoped>
.flow-page {
  min-height: 100vh;
  background: #1A1A2E;
  color: #FFFFFF;
}

.status-bar {
  height: var(--status-bar-height, 44px);
}

/* === 任务选择界面 === */
.select-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  min-height: 100vh;
}

.select-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 16rpx;
}

.select-subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 60rpx;
}

.task-select-list {
  width: 100%;
  flex: 1;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 32rpx;
  color: rgba(255, 255, 255, 0.4);
}

.empty-sub {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.25);
  margin-top: 12rpx;
}

.task-select-item {
  background: rgba(255, 255, 255, 0.06);
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.task-select-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
  display: block;
  margin-bottom: 16rpx;
}

.task-select-anchor {
  display: flex;
  align-items: flex-start;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
}

.anchor-icon {
  font-size: 26rpx;
  margin-right: 10rpx;
}

.anchor-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.6);
  flex: 1;
}

.task-select-meta {
  display: flex;
  gap: 24rpx;
}

.meta-item {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.4);
}

.meta-item.overdue {
  color: #E17055;
}

.exit-btn {
  margin-top: 40rpx;
  padding: 20rpx 60rpx;
  border-radius: 40rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.2);
}

.exit-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
}

/* === 心流主界面 === */
.flow-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 40rpx;
  position: relative;
}

/* 倒计时圆环 */
.timer-section {
  margin-top: 80rpx;
  margin-bottom: 60rpx;
}

.timer-ring {
  width: 440rpx;
  height: 440rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.timer-progress {
  width: 440rpx;
  height: 440rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 1s linear;
}

.timer-inner {
  width: 380rpx;
  height: 380rpx;
  border-radius: 50%;
  background: #1A1A2E;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.timer-time {
  font-size: 72rpx;
  font-weight: 200;
  color: #FFFFFF;
  letter-spacing: 4rpx;
  font-variant-numeric: tabular-nums;
}

.timer-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 8rpx;
}

/* 任务信息 */
.task-info {
  width: 100%;
  text-align: center;
  margin-bottom: 50rpx;
}

.flow-task-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #FFFFFF;
  display: block;
  margin-bottom: 20rpx;
}

.flow-anchor {
  display: inline-flex;
  align-items: flex-start;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 16rpx;
  padding: 16rpx 28rpx;
  max-width: 90%;
}

.flow-anchor-icon {
  font-size: 28rpx;
  margin-right: 10rpx;
  flex-shrink: 0;
}

.flow-anchor-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
}

/* 控制按钮 */
.control-bar {
  display: flex;
  gap: 40rpx;
  margin-bottom: 30rpx;
}

.ctrl-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 28rpx 48rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.08);
  border: 1rpx solid rgba(255, 255, 255, 0.12);
}

.ctrl-btn.start,
.ctrl-btn.resume {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
}

.ctrl-btn.stop {
  background: rgba(225, 112, 85, 0.15);
  border-color: rgba(225, 112, 85, 0.3);
}

.ctrl-icon {
  font-size: 36rpx;
  color: #FFFFFF;
}

.ctrl-text {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

/* 已用时间 */
.elapsed-info {
  margin-bottom: 30rpx;
}

.elapsed-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.4);
}

/* 灵感快寄悬浮按钮 */
.inspiration-fab {
  position: fixed;
  right: 40rpx;
  bottom: 180rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 20rpx 32rpx;
  border-radius: 48rpx;
  background: rgba(253, 203, 110, 0.2);
  border: 1rpx solid rgba(253, 203, 110, 0.4);
  z-index: 100;
}

.fab-icon {
  font-size: 36rpx;
}

.fab-label {
  font-size: 24rpx;
  color: #FDCB6E;
  font-weight: 500;
}

/* 退出按钮 */
.flow-exit {
  position: fixed;
  left: 40rpx;
  bottom: 180rpx;
  padding: 16rpx 28rpx;
  border-radius: 32rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.15);
}

.flow-exit-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.35);
}

/* === 弹窗 === */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: 85%;
  background: #2D2D44;
  border-radius: 24rpx;
  padding: 40rpx;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.modal-close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.6);
}

.modal-hint {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
  display: block;
  margin-bottom: 24rpx;
}

.inspiration-input {
  width: 100%;
  min-height: 200rpx;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #FFFFFF;
  margin-bottom: 24rpx;
  box-sizing: border-box;
}

.end-summary {
  font-size: 32rpx;
  color: #FDCB6E;
  font-weight: 600;
  display: block;
  text-align: center;
  margin-bottom: 30rpx;
}

.input-group {
  margin-bottom: 24rpx;
}

.input-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.6);
  display: block;
  margin-bottom: 12rpx;
}

.input-field {
  width: 100%;
  height: 80rpx;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #FFFFFF;
  box-sizing: border-box;
}

.modal-btns {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
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
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
}

.modal-btn.confirm {
  background: rgba(253, 203, 110, 0.25);
  color: #FDCB6E;
  font-weight: 600;
}
</style>
