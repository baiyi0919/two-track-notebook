<template>
  <view class="page">
    <!-- 用户信息 -->
    <view class="user-card">
      <image v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" class="avatar-img" />
      <view v-else class="avatar">🧭</view>
      <view class="user-info">
        <text class="username">{{ userInfo?.nickname || username }}</text>
        <text class="user-desc">双轨笔记本人</text>
      </view>
      <view class="edit-btn" @tap="showEditModal">✏️</view>
    </view>

    <!-- 注意力报告 -->
    <view class="card report-card">
      <view class="section-header">
        <text class="section-title">📊 注意力审计</text>
        <text class="section-date" @tap="pickDate">{{ reportDate || '今日' }}</text>
      </view>

      <view v-if="report" class="report-stats">
        <view class="report-row">
          <view class="report-item">
            <text class="report-num">{{ fmtHoursNum(report.totalBudget) }}</text>
            <text class="report-label">预算(h)</text>
          </view>
          <view class="report-item">
            <text class="report-num actual">{{ fmtHoursNum(report.totalActual) }}</text>
            <text class="report-label">实际(h)</text>
          </view>
          <view class="report-item">
            <text class="report-num done">{{ report.completedCount }}</text>
            <text class="report-label">已完成</text>
          </view>
          <view class="report-item">
            <text class="report-num pending">{{ report.pendingCount }}</text>
            <text class="report-label">进行中</text>
          </view>
        </view>

        <!-- 消耗率 -->
        <view v-if="report.totalBudget > 0" class="efficiency-bar">
          <text class="efficiency-label">注意力消耗率</text>
          <view class="efficiency-track">
            <view
              class="efficiency-fill"
              :class="{ over: efficiency > 100 }"
              :style="{ width: Math.min(efficiency, 100) + '%' }"
            ></view>
          </view>
          <text class="efficiency-value">{{ efficiency.toFixed(0) }}%</text>
        </view>
      </view>

      <view v-else class="no-report">
        <text class="no-report-text">暂无数据</text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="card menu-card">
      <view class="menu-item" @tap="goCalendar">
        <text class="menu-icon">📅</text>
        <text class="menu-text">日历视图</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goAttentionReport">
        <text class="menu-icon">📊</text>
        <text class="menu-text">注意力报告详情</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="refreshReport">
        <text class="menu-icon">🔄</text>
        <text class="menu-text">刷新数据</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="card menu-card">
      <view class="menu-item" @tap="handleLogout">
        <text class="menu-icon">🚪</text>
        <text class="menu-text">退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 版本信息 -->
    <view class="version-info">
      <text>双轨笔记本 v1.0.0</text>
    </view>

    <!-- 编辑个人信息弹窗 -->
    <view v-if="showModal" class="modal-mask" @tap="closeModal">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">编辑个人信息</text>
          <text class="modal-close" @tap="closeModal">✕</text>
        </view>
        <view class="modal-body">
          <view class="form-item">
            <text class="form-label">昵称</text>
            <input
              class="form-input"
              v-model="editForm.nickname"
              placeholder="请输入昵称"
              maxlength="20"
            />
          </view>
          <view class="form-item">
            <text class="form-label">头像链接</text>
            <input
              class="form-input"
              v-model="editForm.avatarUrl"
              placeholder="请输入头像URL（可选）"
            />
          </view>
        </view>
        <view class="modal-footer">
          <view class="btn btn-cancel" @tap="closeModal">取消</view>
          <view class="btn btn-confirm" @tap="handleUpdate">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getUserInfo, updateUser } from '@/api'
import { getAttentionReport } from '@/api'
import { fmtHoursNum } from '@/utils/format'

const username = ref(uni.getStorageSync('username') || '用户')
const userInfo = ref<any>(null)
const report = ref<any>(null)
const reportDate = ref('')

const showModal = ref(false)
const editForm = ref({
  nickname: '',
  avatarUrl: ''
})

const efficiency = computed(() => {
  if (!report.value || !report.value.totalBudget) return 0
  return ((report.value.totalActual || 0) / report.value.totalBudget) * 100
})

onMounted(() => {
  loadUserInfo()
  loadReport()
})

async function loadUserInfo() {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

async function loadReport() {
  try {
    const res = await getAttentionReport(reportDate.value || undefined)
    report.value = res.data
  } catch (e) {
    console.error('加载报告失败', e)
  }
}

function refreshReport() {
  loadReport()
  uni.showToast({ title: '已刷新', icon: 'success' })
}

function pickDate() {
  uni.showModal({
    title: '选择日期',
    editable: true,
    placeholderText: '格式: 2026-05-25',
    content: reportDate.value || '',
    success: (res) => {
      if (res.confirm && res.content) {
        const d = res.content.trim()
        if (/^\d{4}-\d{2}-\d{2}$/.test(d)) {
          reportDate.value = d
          loadReport()
        } else {
          uni.showToast({ title: '日期格式不正确', icon: 'none' })
        }
      }
    }
  })
}

function goAttentionReport() {
  uni.navigateTo({ url: '/pages/mine/report' })
}

function goCalendar() {
  uni.navigateTo({ url: '/pages/calendar/index' })
}

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '退出后需要重新登录',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('username')
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }
  })
}

function showEditModal() {
  editForm.value = {
    nickname: userInfo.value?.nickname || username.value,
    avatarUrl: userInfo.value?.avatarUrl || ''
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function handleUpdate() {
  if (!editForm.value.nickname || editForm.value.nickname.trim().length < 2) {
    uni.showToast({ title: '昵称至少2个字符', icon: 'none' })
    return
  }

  try {
    const res = await updateUser({
      nickname: editForm.value.nickname.trim(),
      avatarUrl: editForm.value.avatarUrl || undefined
    })
    userInfo.value = res.data
    username.value = res.data.nickname
    uni.setStorageSync('username', res.data.nickname)
    showModal.value = false
    uni.showToast({ title: '更新成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e.message || '更新失败', icon: 'none' })
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
}

.user-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  padding: 60rpx 40rpx 50rpx;
  border-radius: 0 0 40rpx 40rpx;
  position: relative;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  margin-right: 30rpx;
}

.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin-right: 30rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.user-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.username {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}

.user-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8rpx;
}

.edit-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}

.report-card {
  margin-top: -30rpx;
  position: relative;
  z-index: 1;
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

.section-date {
  font-size: 24rpx;
  color: #6C5CE7;
}

.report-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24rpx;
}

.report-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.report-num {
  font-size: 44rpx;
  font-weight: 700;
  color: #2D3436;
}

.report-num.actual {
  color: #E17055;
}

.report-num.done {
  color: #00B894;
}

.report-num.pending {
  color: #FDCB6E;
}

.report-label {
  font-size: 22rpx;
  color: #B2BEC3;
  margin-top: 6rpx;
}

.efficiency-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.efficiency-label {
  font-size: 24rpx;
  color: #636E72;
  white-space: nowrap;
}

.efficiency-track {
  flex: 1;
  height: 16rpx;
  background: #F0F0F0;
  border-radius: 8rpx;
  overflow: hidden;
}

.efficiency-fill {
  height: 100%;
  background: linear-gradient(90deg, #6C5CE7, #A29BFE);
  border-radius: 8rpx;
  transition: width 0.3s;
}

.efficiency-fill.over {
  background: linear-gradient(90deg, #E17055, #FDCB6E);
}

.efficiency-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #6C5CE7;
  min-width: 80rpx;
  text-align: right;
}

.no-report {
  padding: 40rpx 0;
  text-align: center;
}

.no-report-text {
  font-size: 28rpx;
  color: #B2BEC3;
}

.menu-card {
  padding: 0;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #F5F6FA;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #2D3436;
}

.menu-arrow {
  font-size: 36rpx;
  color: #B2BEC3;
}

.version-info {
  text-align: center;
  padding: 40rpx 0;
  font-size: 24rpx;
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
</style>
