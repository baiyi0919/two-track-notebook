<template>
  <view class="page">
    <!-- 顶部概览卡片 -->
    <view class="overview-card">
      <text class="overview-title">📊 注意力审计报告</text>
      <text class="overview-date">{{ report?.date || '今日' }}</text>

      <view v-if="report" class="overview-stats">
        <view class="stat-row">
          <view class="stat-box">
            <text class="stat-num">{{ fmtHoursNum(report.totalBudget) }}</text>
            <text class="stat-label">总预算(h)</text>
          </view>
          <view class="stat-box">
            <text class="stat-num actual">{{ fmtHoursNum(report.totalActual) }}</text>
            <text class="stat-label">总实际(h)</text>
          </view>
          <view class="stat-box">
            <text class="stat-num" :class="report.budgetVariance > 0 ? 'over' : 'under'">
              {{ fmtVariance(report.budgetVariance) }}
            </text>
            <text class="stat-label">偏差</text>
          </view>
        </view>

        <!-- 消耗率进度条 -->
        <view class="efficiency-section">
          <view class="efficiency-header">
            <text class="efficiency-title">注意力消耗率</text>
            <text class="efficiency-pct" :class="{ over: efficiency > 100 }">
              {{ efficiency.toFixed(0) }}%
            </text>
          </view>
          <view class="bar-track">
            <view
              class="bar-fill"
              :class="{ over: efficiency > 100 }"
              :style="{ width: Math.min(efficiency, 100) + '%' }"
            ></view>
            <view v-if="efficiency > 100" class="bar-marker" :style="{ left: '100%' }"></view>
          </view>
          <view class="bar-labels">
            <text class="bar-label">0%</text>
            <text class="bar-label">100%</text>
          </view>
        </view>

        <!-- 状态统计 -->
        <view class="status-row">
          <view class="status-chip done">
            <text>{{ report.completedCount }} 已完成</text>
          </view>
          <view class="status-chip pending">
            <text>{{ report.pendingCount }} 进行中</text>
          </view>
          <view v-if="report.abandonedCount > 0" class="status-chip abandoned">
            <text>{{ report.abandonedCount }} 已放弃</text>
          </view>
        </view>

        <!-- 当日数据（来自日志） -->
        <view v-if="report.dayBudget > 0 || report.dayActual > 0" class="day-section">
          <text class="day-title">📅 今日记录</text>
          <view class="day-stats">
            <view class="day-stat">
              <text class="day-stat-val">{{ fmtHoursNum(report.dayBudget) }}</text>
              <text class="day-stat-label">今日预算(h)</text>
            </view>
            <view class="day-stat">
              <text class="day-stat-val actual">{{ fmtHoursNum(report.dayActual) }}</text>
              <text class="day-stat-label">今日实际(h)</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 按状态预算分布 -->
    <view v-if="report?.statusBudget" class="card section">
      <text class="section-title">📐 预算分布</text>

      <view class="budget-grid">
        <view class="budget-item">
          <text class="budget-label">✓ 已完成</text>
          <text class="budget-val">{{ fmtHoursNum(report.statusBudget.completedBudget) }}h / {{ fmtHoursNum(report.statusBudget.completedActual) }}h</text>
          <view class="mini-bar-track">
            <view
              class="mini-bar-fill done"
              :style="{ width: budgetBarWidth(report.statusBudget.completedBudget, report.totalBudget) + '%' }"
            ></view>
          </view>
        </view>
        <view class="budget-item">
          <text class="budget-label">● 进行中</text>
          <text class="budget-val">{{ fmtHoursNum(report.statusBudget.pendingBudget) }}h / {{ fmtHoursNum(report.statusBudget.pendingActual) }}h</text>
          <view class="mini-bar-track">
            <view
              class="mini-bar-fill pending"
              :style="{ width: budgetBarWidth(report.statusBudget.pendingBudget, report.totalBudget) + '%' }"
            ></view>
          </view>
        </view>
        <view v-if="report.abandonedCount > 0" class="budget-item">
          <text class="budget-label">✗ 已放弃</text>
          <text class="budget-val">{{ fmtHoursNum(report.statusBudget.abandonedBudget) }}h / {{ fmtHoursNum(report.statusBudget.abandonedActual) }}h</text>
          <view class="mini-bar-track">
            <view
              class="mini-bar-fill abandoned"
              :style="{ width: budgetBarWidth(report.statusBudget.abandonedBudget, report.totalBudget) + '%' }"
            ></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 趋势图 -->
    <view class="card section">
      <view class="section-header">
        <text class="section-title">📈 消耗趋势</text>
        <view class="trend-toggle">
          <text
            class="toggle-btn"
            :class="{ active: trendDays === 7 }"
            @tap="switchTrend(7)"
          >7天</text>
          <text
            class="toggle-btn"
            :class="{ active: trendDays === 30 }"
            @tap="switchTrend(30)"
          >30天</text>
        </view>
      </view>

      <view v-if="trend" class="trend-chart">
        <!-- 简易柱状图 -->
        <view class="chart-area">
          <view
            v-for="(day, idx) in trend.daily"
            :key="idx"
            class="chart-bar-group"
          >
            <view class="bar-pair">
              <view
                class="chart-bar budget"
                :style="{ height: barHeight(day.budget) + 'rpx' }"
              ></view>
              <view
                class="chart-bar actual"
                :class="{ over: day.efficiency > 100 }"
                :style="{ height: barHeight(day.actualTime) + 'rpx' }"
              ></view>
            </view>
            <text v-if="trendDays === 7 || idx % 5 === 0" class="chart-label">
              {{ shortDate(day.date) }}
            </text>
          </view>
        </view>
        <view class="chart-legend">
          <view class="legend-item">
            <view class="legend-dot budget"></view>
            <text class="legend-text">预算</text>
          </view>
          <view class="legend-item">
            <view class="legend-dot actual"></view>
            <text class="legend-text">实际</text>
          </view>
        </view>

        <!-- 趋势汇总 -->
        <view class="trend-summary">
          <view class="trend-stat">
            <text class="trend-stat-val">{{ fmtHoursNum(trend.periodTotalBudget) }}h</text>
            <text class="trend-stat-label">期间总预算</text>
          </view>
          <view class="trend-stat">
            <text class="trend-stat-val">{{ fmtHoursNum(trend.periodTotalActual) }}h</text>
            <text class="trend-stat-label">期间总实际</text>
          </view>
          <view class="trend-stat">
            <text class="trend-stat-val">{{ fmtPct(trend.avgEfficiency) }}</text>
            <text class="trend-stat-label">平均消耗率</text>
          </view>
          <view class="trend-stat">
            <text class="trend-stat-val">{{ trend.periodCompletedCount }}</text>
            <text class="trend-stat-label">完成任务</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 任务明细列表 -->
    <view class="card section">
      <text class="section-title">📋 任务明细</text>

      <view v-if="report?.tasks?.length" class="task-list">
        <view
          v-for="task in report.tasks"
          :key="task.taskId"
          class="task-item"
          @tap="goTaskDetail(task.taskId)"
        >
          <view class="task-item-header">
            <text class="task-item-title" :class="{ 'line-through': task.status === 1 }">
              {{ task.title }}
            </text>
            <view class="task-status-tag" :class="statusClass(task.status)">
              {{ statusText(task.status) }}
            </view>
          </view>

          <view v-if="task.anchorText" class="task-anchor">
            <text class="task-anchor-icon">⚓</text>
            <text class="task-anchor-text">{{ task.anchorText }}</text>
          </view>

          <view class="task-item-time">
            <text class="time-label">预算 {{ fmtHours(task.budget) }}</text>
            <text class="time-label">实际 {{ fmtHours(task.actualTime) }}</text>
            <text v-if="task.overBudget" class="time-over">⚠ 超支</text>
            <text v-else-if="task.budget > 0" class="time-ok">✓ 适中</text>
          </view>

          <!-- 每个任务的消耗率小条 -->
          <view v-if="task.budget > 0" class="task-bar-track">
            <view
              class="task-bar-fill"
              :class="{ over: task.efficiency > 100 }"
              :style="{ width: Math.min(task.efficiency || 0, 100) + '%' }"
            ></view>
          </view>
        </view>
      </view>

      <view v-else class="empty-tasks">
        <text class="empty-text">暂无任务数据</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAttentionReport, getAttentionTrend } from '@/api'
import { fmtHours, fmtHoursNum, fmtPct, fmtVariance } from '@/utils/format'

const report = ref<any>(null)
const trend = ref<any>(null)
const trendDays = ref(7)

const efficiency = computed(() => {
  if (!report.value || !report.value.totalBudget) return 0
  return report.value.efficiency || ((report.value.totalActual || 0) / report.value.totalBudget * 100)
})

onMounted(() => {
  loadData()
})

async function loadData() {
  try {
    const [reportRes, trendRes] = await Promise.all([
      getAttentionReport(),
      getAttentionTrend(trendDays.value)
    ])
    report.value = reportRes.data
    trend.value = trendRes.data
  } catch (e) {
    console.error('加载报告失败', e)
  }
}

async function switchTrend(days: number) {
  trendDays.value = days
  try {
    const res = await getAttentionTrend(days)
    trend.value = res.data
  } catch (e) {
    console.error('加载趋势失败', e)
  }
}

function budgetBarWidth(budget: number, total: number): number {
  if (!total) return 0
  return Math.min((budget / total) * 100, 100)
}

function barHeight(value: number): number {
  if (!trend.value?.daily) return 0
  const maxVal = Math.max(...trend.value.daily.map((d: any) => Math.max(d.budget || 0, d.actualTime || 0)), 1)
  return Math.max((value / maxVal) * 200, 4)
}

function shortDate(dateStr: string): string {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return parts.length >= 3 ? `${parts[1]}/${parts[2]}` : dateStr
}

function statusClass(status: number): string {
  return status === 1 ? 'done' : status === 2 ? 'abandoned' : 'pending'
}

function statusText(status: number): string {
  return status === 1 ? '已完成' : status === 2 ? '已放弃' : '进行中'
}

function goTaskDetail(id: number) {
  uni.navigateTo({ url: `/pages/index/detail?id=${id}` })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
  padding-bottom: 40rpx;
}

/* ===== 概览卡片 ===== */
.overview-card {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  padding: 40rpx;
  border-radius: 0 0 40rpx 40rpx;
}

.overview-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
  display: block;
}

.overview-date {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8rpx;
  display: block;
}

.overview-stats {
  margin-top: 30rpx;
}

.stat-row {
  display: flex;
  justify-content: space-around;
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 48rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.stat-num.actual {
  color: #FDCB6E;
}

.stat-num.over {
  color: #FF7675;
}

.stat-num.under {
  color: #00E676;
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 6rpx;
}

/* 消耗率 */
.efficiency-section {
  margin-top: 30rpx;
}

.efficiency-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.efficiency-title {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.efficiency-pct {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.efficiency-pct.over {
  color: #FF7675;
}

.bar-track {
  height: 20rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10rpx;
  overflow: visible;
  position: relative;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #00E676, #55EFC4);
  border-radius: 10rpx;
  transition: width 0.5s;
  position: relative;
}

.bar-fill.over {
  background: linear-gradient(90deg, #FDCB6E, #FF7675);
}

.bar-marker {
  position: absolute;
  top: -4rpx;
  width: 4rpx;
  height: 28rpx;
  background: #FFFFFF;
  border-radius: 2rpx;
  transform: translateX(-2rpx);
}

.bar-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6rpx;
}

.bar-label {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.5);
}

/* 状态标签 */
.status-row {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
  flex-wrap: wrap;
}

.status-chip {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  color: #FFFFFF;
}

.status-chip.done {
  background: rgba(0, 184, 148, 0.4);
}

.status-chip.pending {
  background: rgba(253, 203, 110, 0.4);
}

.status-chip.abandoned {
  background: rgba(255, 118, 117, 0.4);
}

/* 当日数据 */
.day-section {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.2);
}

.day-title {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  display: block;
  margin-bottom: 12rpx;
}

.day-stats {
  display: flex;
  justify-content: space-around;
}

.day-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.day-stat-val {
  font-size: 40rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.day-stat-val.actual {
  color: #FDCB6E;
}

.day-stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4rpx;
}

/* ===== 通用区块 ===== */
.section {
  margin: 24rpx 24rpx 0;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
  display: block;
  margin-bottom: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

/* ===== 预算分布 ===== */
.budget-grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.budget-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.budget-label {
  font-size: 28rpx;
  color: #2D3436;
  font-weight: 500;
}

.budget-val {
  font-size: 24rpx;
  color: #636E72;
}

.mini-bar-track {
  height: 12rpx;
  background: #F0F0F0;
  border-radius: 6rpx;
  overflow: hidden;
}

.mini-bar-fill {
  height: 100%;
  border-radius: 6rpx;
}

.mini-bar-fill.done {
  background: #00B894;
}

.mini-bar-fill.pending {
  background: #FDCB6E;
}

.mini-bar-fill.abandoned {
  background: #FF7675;
}

/* ===== 趋势图 ===== */
.trend-toggle {
  display: flex;
  gap: 12rpx;
}

.toggle-btn {
  padding: 8rpx 20rpx;
  border-radius: 16rpx;
  font-size: 24rpx;
  color: #636E72;
  background: #F5F6FA;
}

.toggle-btn.active {
  background: #6C5CE7;
  color: #FFFFFF;
}

.chart-area {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 280rpx;
  padding: 0 10rpx;
  border-bottom: 1rpx solid #F0F0F0;
}

.chart-bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar-pair {
  display: flex;
  gap: 4rpx;
  align-items: flex-end;
}

.chart-bar {
  width: 20rpx;
  border-radius: 4rpx 4rpx 0 0;
  min-height: 4rpx;
}

.chart-bar.budget {
  background: #A29BFE;
}

.chart-bar.actual {
  background: #6C5CE7;
}

.chart-bar.actual.over {
  background: #FF7675;
}

.chart-label {
  font-size: 18rpx;
  color: #B2BEC3;
  margin-top: 8rpx;
  white-space: nowrap;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  margin-top: 20rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.legend-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}

.legend-dot.budget {
  background: #A29BFE;
}

.legend-dot.actual {
  background: #6C5CE7;
}

.legend-text {
  font-size: 22rpx;
  color: #636E72;
}

.trend-summary {
  display: flex;
  justify-content: space-around;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #F0F0F0;
}

.trend-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.trend-stat-val {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
}

.trend-stat-label {
  font-size: 20rpx;
  color: #B2BEC3;
  margin-top: 4rpx;
}

/* ===== 任务明细 ===== */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.task-item {
  background: #F5F6FA;
  border-radius: 16rpx;
  padding: 24rpx;
}

.task-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-item-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2D3436;
  flex: 1;
}

.task-item-title.line-through {
  text-decoration: line-through;
  color: #B2BEC3;
}

.task-status-tag {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  color: #FFFFFF;
}

.task-status-tag.done {
  background: #00B894;
}

.task-status-tag.pending {
  background: #FDCB6E;
  color: #2D3436;
}

.task-status-tag.abandoned {
  background: #FF7675;
}

.task-anchor {
  display: flex;
  align-items: flex-start;
  margin-top: 12rpx;
  gap: 8rpx;
}

.task-anchor-icon {
  font-size: 24rpx;
}

.task-anchor-text {
  font-size: 24rpx;
  color: #6C5CE7;
  flex: 1;
}

.task-item-time {
  display: flex;
  gap: 20rpx;
  margin-top: 12rpx;
  align-items: center;
}

.time-label {
  font-size: 24rpx;
  color: #636E72;
}

.time-over {
  font-size: 22rpx;
  color: #FF7675;
  font-weight: 600;
}

.time-ok {
  font-size: 22rpx;
  color: #00B894;
}

.task-bar-track {
  height: 8rpx;
  background: #E0E0E0;
  border-radius: 4rpx;
  margin-top: 12rpx;
  overflow: hidden;
}

.task-bar-fill {
  height: 100%;
  background: #6C5CE7;
  border-radius: 4rpx;
}

.task-bar-fill.over {
  background: #FF7675;
}

.empty-tasks {
  padding: 40rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #B2BEC3;
}
</style>
