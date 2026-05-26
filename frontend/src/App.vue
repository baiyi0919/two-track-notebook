<template>
</template>

<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 检查登录状态，未登录则跳转登录页
  const token = uni.getStorageSync('token')
  if (!token) {
    // 小延迟确保 TabBar 页面已初始化后再跳转，避免白屏
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/index' })
    }, 50)
  }
})
</script>

<style>
/* 全局样式 - 双轨笔记本主题 */
page {
  background-color: #F5F6FA;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333333;
  box-sizing: border-box;
}

/* 主题色 */
:root {
  --primary: #6C5CE7;
  --primary-light: #A29BFE;
  --primary-dark: #5A4BD1;
  --success: #00B894;
  --warning: #FDCB6E;
  --danger: #E17055;
  --bg: #F5F6FA;
  --card: #FFFFFF;
  --text-primary: #2D3436;
  --text-secondary: #636E72;
  --text-hint: #B2BEC3;
  --border: #DFE6E9;
}

/* 通用卡片 */
.card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin: 20rpx 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

/* 通用按钮 */
.btn-primary {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
  border: none;
  border-radius: 16rpx;
  padding: 24rpx 0;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  width: 100%;
}

.btn-primary::after {
  border: none;
}

/* 锚点高亮 */
.anchor-tag {
  display: inline-block;
  background: rgba(108, 92, 231, 0.1);
  color: #6C5CE7;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  margin-top: 10rpx;
}

/* 状态标签 */
.status-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
}

.status-pending {
  background: rgba(253, 203, 110, 0.2);
  color: #E17055;
}

.status-done {
  background: rgba(0, 184, 148, 0.2);
  color: #00B894;
}

.status-closed {
  background: rgba(108, 92, 231, 0.15);
  color: #6C5CE7;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
  color: #B2BEC3;
}

.empty-state .empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-state .empty-text {
  font-size: 28rpx;
}

/* 隐藏滚动条 */
::-webkit-scrollbar {
  display: none;
}
</style>
