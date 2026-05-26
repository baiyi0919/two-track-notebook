<template>
  <view class="login-page">
    <!-- 顶部装饰 -->
    <view class="header-bg">
      <view class="circle circle-1"></view>
      <view class="circle circle-2"></view>
    </view>

    <!-- 标题区 -->
    <view class="title-area">
      <text class="app-icon">🧭</text>
      <text class="app-title">双轨笔记本</text>
      <text class="app-subtitle">现实锚点 · 思想沙盒 · 原则提取</text>
    </view>

    <!-- 表单区 -->
    <view class="form-area">
      <view class="input-group">
        <text class="input-label">用户名</text>
        <input
          class="input-field"
          v-model="form.username"
          placeholder="请输入用户名"
          :maxlength="20"
        />
      </view>

      <view class="input-group">
        <text class="input-label">密码</text>
        <input
          class="input-field"
          v-model="form.password"
          placeholder="请输入密码"
          :password="true"
          :maxlength="30"
        />
      </view>

      <view class="input-group" v-if="isRegister">
        <text class="input-label">昵称（选填）</text>
        <input
          class="input-field"
          v-model="form.nickname"
          placeholder="给自己起个名字吧"
          :maxlength="20"
        />
      </view>

      <button class="btn-primary submit-btn" @tap="handleSubmit" :loading="loading">
        {{ isRegister ? '注册' : '登录' }}
      </button>

      <view class="switch-mode" @tap="toggleMode">
        <text class="switch-text">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login, register } from '@/api'

const isRegister = ref(false)
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: ''
})

function toggleMode() {
  isRegister.value = !isRegister.value
}

async function handleSubmit() {
  if (!form.value.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!form.value.password.trim() || form.value.password.length < 6) {
    uni.showToast({ title: '密码至少6位', icon: 'none' })
    return
  }

  loading.value = true
  try {
    let token = ''
    if (isRegister.value) {
      const res = await register(form.value.username, form.value.password, form.value.nickname || undefined)
      token = res.data
      uni.showToast({ title: '注册成功', icon: 'success' })
    } else {
      const res = await login(form.value.username, form.value.password)
      token = res.data
      uni.showToast({ title: '登录成功', icon: 'success' })
    }

    // 保存token
    uni.setStorageSync('token', token)
    uni.setStorageSync('username', form.value.username)
    // 跳转首页
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } catch (e) {
    console.error('登录/注册失败', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #F5F6FA;
  position: relative;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 600rpx;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  border-radius: 0 0 60rpx 60rpx;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300rpx;
  height: 300rpx;
  top: -80rpx;
  right: -60rpx;
}

.circle-2 {
  width: 200rpx;
  height: 200rpx;
  top: 200rpx;
  left: -40rpx;
}

.title-area {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
}

.app-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.app-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 12rpx;
}

.app-subtitle {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.form-area {
  position: relative;
  z-index: 1;
  margin: 60rpx 50rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.08);
}

.input-group {
  margin-bottom: 36rpx;
}

.input-label {
  font-size: 28rpx;
  color: #636E72;
  margin-bottom: 12rpx;
  display: block;
}

.input-field {
  width: 100%;
  height: 88rpx;
  background: #F5F6FA;
  border-radius: 16rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
  color: #2D3436;
  box-sizing: border-box;
}

.submit-btn {
  margin-top: 20rpx;
  width: 100%;
}

.switch-mode {
  text-align: center;
  margin-top: 30rpx;
}

.switch-text {
  font-size: 28rpx;
  color: #6C5CE7;
}
</style>
