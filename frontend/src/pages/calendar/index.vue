<template>
  <view class="calendar-page">
    <!-- 顶部导航 -->
    <view class="header">
      <view class="nav-btn" @tap="prevMonth">‹</view>
      <text class="title">{{ currentYear }}年{{ currentMonth + 1 }}月</text>
      <view class="nav-btn" @tap="nextMonth">›</view>
      <view class="today-btn" @tap="goToday">今天</view>
    </view>

    <!-- 星期标题 -->
    <view class="weekdays">
      <text v-for="w in weekDays" :key="w" class="weekday">{{ w }}</text>
    </view>

    <!-- 日历网格 -->
    <view class="days-grid">
      <view
        v-for="(day, idx) in calendarDays"
        :key="idx"
        class="day-cell"
        :class="{
          'other-month': !day.isCurrentMonth,
          'is-today': day.isToday,
          'selected': day.fullDate === selectedDate,
          'has-tasks': day.taskCount > 0
        }"
        @tap="selectDate(day)"
      >
        <text class="day-num">{{ day.day }}</text>
        <!-- 任务状态标记 -->
        <view v-if="day.taskCount > 0" class="task-dots">
          <view
            v-for="(status, sIdx) in day.statusSummary"
            :key="sIdx"
            class="dot"
            :class="'dot-' + status"
          ></view>
          <text v-if="day.taskCount > 3" class="more">+{{ day.taskCount - 3 }}</text>
        </view>
        <!-- 时间统计 -->
        <text v-if="day.totalActual > 0" class="time-label">
          {{ fmtHoursNum(day.totalActual) }}h
        </text>
      </view>
    </view>

    <!-- 当日任务列表 -->
    <view class="day-tasks" v-if="selectedDate">
      <view class="section-title">
        <text>{{ selectedDateLabel }} 的任务</text>
        <text class="task-count">{{ dayTasks.length }} 个</text>
      </view>

      <view v-if="dayTasks.length === 0" class="empty-tip">
        当天没有任务
      </view>

      <view
        v-for="task in dayTasks"
        :key="task.id"
        class="task-item card"
        @tap="goTaskDetail(task.id)"
      >
        <view class="task-status-dot" :class="'dot-' + task.status"></view>
        <view class="task-info">
          <text class="task-title">{{ task.title }}</text>
          <view class="task-meta">
            <text class="meta-item" v-if="task.budget">
              预算 {{ fmtHoursNum(task.budget) }}h
            </text>
            <text class="meta-item" v-if="task.actualTime">
              实际 {{ fmtHoursNum(task.actualTime) }}h
            </text>
            <text class="over-budget" v-if="task.overBudget">⚠ 超支</text>
          </view>
        </view>
        <text class="task-status-label" :class="'label-' + task.status">
          {{ statusLabel(task.status) }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTaskList } from '@/api'
import { fmtHoursNum } from '@/utils/format'

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth())  // 0-indexed
const selectedDate = ref('')

const allTasks = ref<any[]>([])
const dayTasks = ref<any[]>([])

// 选中的日期标签
const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const d = new Date(selectedDate.value)
  return `${d.getMonth() + 1}月${d.getDate()}日`
})

// 生成日历天数
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value

  const firstDay = new Date(year, month, 1).getDay()  // 当月1号是星期几
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const daysInPrevMonth = new Date(year, month, 0).getDate()

  const days: any[] = []
  const todayStr = fmtDate(today)

  // 上月补齐
  for (let i = firstDay - 1; i >= 0; i--) {
    const d = daysInPrevMonth - i
    const m = month === 0 ? 11 : month - 1
    const y = month === 0 ? year - 1 : year
    const full = fmtDate(new Date(y, m, d))
    days.push({
      year: y, month: m, day: d,
      fullDate: full,
      isCurrentMonth: false,
      isToday: full === todayStr,
      taskCount: 0, statusSummary: [], totalActual: 0
    })
  }

  // 当月
  for (let d = 1; d <= daysInMonth; d++) {
    const full = fmtDate(new Date(year, month, d))
    const tasksForDay = getTasksForDate(full)
    days.push({
      year, month, day: d,
      fullDate: full,
      isCurrentMonth: true,
      isToday: full === todayStr,
      taskCount: tasksForDay.count,
      statusSummary: tasksForDay.statuses,
      totalActual: tasksForDay.totalActual
    })
  }

  // 下月补齐
  const remaining = 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const m = month === 11 ? 0 : month + 1
    const y = month === 11 ? year + 1 : year
    const full = fmtDate(new Date(y, m, d))
    days.push({
      year: y, month: m, day: d,
      fullDate: full,
      isCurrentMonth: false,
      isToday: full === todayStr,
      taskCount: 0, statusSummary: [], totalActual: 0
    })
  }

  return days
})

// 获取某天的任务统计
function getTasksForDate(fullDate: string) {
  const tasks = allTasks.value.filter(t => t.dueDate?.startsWith(fullDate))
  const statuses = [...new Set(tasks.map(t => t.status))]
  const totalActual = tasks.reduce((sum, t) => sum + (t.actualTime || 0), 0)
  return { count: tasks.length, statuses: statuses.slice(0, 3), totalActual }
}

// 选择日期
function selectDate(day: any) {
  if (!day.isCurrentMonth) {
    // 切换到对应月份
    currentYear.value = day.year
    currentMonth.value = day.month
  }
  selectedDate.value = day.fullDate
  loadDayTasks()
}

// 加载当天任务
function loadDayTasks() {
  if (!selectedDate.value) {
    dayTasks.value = []
    return
  }
  dayTasks.value = allTasks.value.filter(
    t => t.dueDate?.startsWith(selectedDate.value)
  )
}

// 切换月份
function prevMonth() {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
  // 清除选中
  selectedDate.value = ''
  dayTasks.value = []
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
  selectedDate.value = ''
  dayTasks.value = []
}

function goToday() {
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth()
  selectedDate.value = fmtDate(today)
  loadDayTasks()
}

// 跳转到任务详情
function goTaskDetail(taskId: number) {
  uni.navigateTo({ url: `/pages/index/detail?id=${taskId}` })
}

// 状态标签
function statusLabel(status: string) {
  const map: Record<string, string> = {
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'ABANDONED': '已放弃'
  }
  return map[status] || status
}

// 格式化日期为 YYYY-MM-DD
function fmtDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(async () => {
  try {
    const res = await getTaskList()
    allTasks.value = res.data || []
    // 默认选中今天
    goToday()
  } catch (e) {
    console.error('加载任务失败', e)
  }
})
</script>

<style scoped>
.calendar-page {
  padding: 20rpx;
  padding-bottom: 120rpx;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 顶部导航 */
.header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
  padding: 20rpx 0;
}
.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #18191c;
}
.nav-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  color: #18191c;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.08);
}
.today-btn {
  margin-left: auto;
  padding: 8rpx 24rpx;
  font-size: 26rpx;
  color: #4080ff;
  background: #e8f0fe;
  border-radius: 20rpx;
}

/* 星期标题 */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin: 16rpx 0 8rpx;
}
.weekday {
  text-align: center;
  font-size: 24rpx;
  color: #8c8c8c;
  font-weight: 600;
}

/* 日历网格 */
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 12rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.06);
}
.day-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 8rpx 0;
  border-radius: 16rpx;
  min-height: 100rpx;
  cursor: pointer;
  transition: background 0.2s;
}
.day-cell:active {
  background: #f0f0f0;
}
.day-cell.other-month .day-num {
  color: #ccc;
}
.day-cell.is-today .day-num {
  background: #4080ff;
  color: #fff;
  border-radius: 50%;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.day-cell.selected {
  background: #e8f0fe;
}
.day-num {
  font-size: 28rpx;
  color: #18191c;
  font-weight: 500;
}

/* 任务标记点 */
.task-dots {
  display: flex;
  gap: 4rpx;
  margin-top: 4rpx;
  flex-wrap: wrap;
  justify-content: center;
}
.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.dot-IN_PROGRESS { background: #f5a623; }
.dot-COMPLETED { background: #52c41a; }
.dot-ABANDONED { background: #ff4d4f; }

.more {
  font-size: 18rpx;
  color: #8c8c8c;
}

.time-label {
  font-size: 18rpx;
  color: #8c8c8c;
  margin-top: 2rpx;
}

/* 当日任务列表 */
.day-tasks {
  margin-top: 24rpx;
}
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 30rpx;
  font-weight: 700;
  color: #18191c;
  margin-bottom: 16rpx;
}
.task-count {
  font-size: 24rpx;
  color: #8c8c8c;
  font-weight: 400;
}
.empty-tip {
  text-align: center;
  color: #8c8c8c;
  padding: 60rpx 0;
  font-size: 28rpx;
}

/* 任务项 */
.task-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  margin-bottom: 12rpx;
}
.task-status-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.task-status-dot.dot-IN_PROGRESS { background: #f5a623; }
.task-status-dot.dot-COMPLETED { background: #52c41a; }
.task-status-dot.dot-ABANDONED { background: #ff4d4f; }

.task-info {
  flex: 1;
  min-width: 0;
}
.task-title {
  font-size: 28rpx;
  color: #18191c;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}
.task-meta {
  display: flex;
  gap: 12rpx;
  margin-top: 6rpx;
}
.meta-item {
  font-size: 22rpx;
  color: #8c8c8c;
}
.over-budget {
  font-size: 22rpx;
  color: #ff4d4f;
}

.task-status-label {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}
.label-IN_PROGRESS { color: #f5a623; background: #fff7e6; }
.label-COMPLETED { color: #52c41a; background: #f6ffed; }
.label-ABANDONED { color: #ff4d4f; background: #fff2f0; }
</style>
