<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索原则..."
        @confirm="loadPrinciples"
        @input="onSearchInput"
      />
    </view>

    <!-- 原则列表 -->
    <scroll-view scroll-y class="principle-list" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <view v-if="principles.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">🧩</text>
        <text class="empty-text">还没有原则，从议题中提取吧</text>
      </view>

      <view
        v-for="p in principles"
        :key="p.id"
        class="principle-card card"
        @tap="goPrincipleDetail(p.id)"
      >
        <view class="principle-content-area">
          <text class="principle-text">{{ p.content }}</text>
          <view v-if="p.tags" class="tag-area">
            <text
              v-for="tag in parseTags(p.tags)"
              :key="tag"
              class="tag-item"
            >{{ tag }}</text>
          </view>
          <text v-if="p.sourceThreadId" class="source-link" @tap="goThread(p.sourceThreadId)">
            📎 来源议题 #{{ p.sourceThreadId }}
          </text>
        </view>
        <view class="principle-actions">
          <view class="action-btn edit" @tap="openEditPrinciple(p)">
            <text>编辑</text>
          </view>
          <view class="action-btn danger" @tap="confirmDelete(p.id)">
            <text>删除</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 添加原则按钮 -->
    <view class="fab" @tap="showCreateModal = true">
      <text class="fab-icon">+</text>
    </view>

    <!-- 创建原则弹窗 -->
    <view v-if="showCreateModal" class="modal-mask" @tap="showCreateModal = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">🧩 新建原则</text>
          <view class="modal-close" @tap="showCreateModal = false">
            <text>✕</text>
          </view>
        </view>

        <view class="input-group">
          <text class="input-label">原则内容 *</text>
          <textarea
            class="input-field textarea"
            v-model="newPrinciple.content"
            placeholder="写出一条可执行的行动原则..."
            :maxlength="500"
          />
        </view>

        <view class="input-group">
          <text class="input-label">来源议题ID（选填）</text>
          <input
            class="input-field"
            v-model="newPrinciple.sourceThreadId"
            type="number"
            placeholder="关联的议题编号"
          />
        </view>

        <view class="input-group">
          <text class="input-label">标签（逗号分隔）</text>
          <input class="input-field" v-model="newPrinciple.tags" placeholder="决策,学习,沟通" />
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showCreateModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleCreate" :loading="creating">创建</button>
        </view>
      </view>
    </view>

    <!-- 编辑原则弹窗 -->
    <view v-if="showEditModal" class="modal-mask" @tap="showEditModal = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">✏️ 编辑原则</text>
          <view class="modal-close" @tap="showEditModal = false">
            <text>✕</text>
          </view>
        </view>

        <view class="input-group">
          <text class="input-label">原则内容 *</text>
          <textarea
            class="input-field textarea"
            v-model="editPrincipleData.content"
            placeholder="写出一条可执行的行动原则..."
            :maxlength="500"
          />
        </view>

        <view class="input-group">
          <text class="input-label">标签（逗号分隔）</text>
          <input class="input-field" v-model="editPrincipleData.tags" placeholder="决策,学习,沟通" />
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showEditModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleEdit" :loading="editing">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPrincipleList, createPrinciple, updatePrinciple, deletePrinciple } from '@/api'

const principles = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const keyword = ref('')
const showCreateModal = ref(false)
const creating = ref(false)

const newPrinciple = ref({
  content: '',
  sourceThreadId: '',
  tags: ''
})

// 编辑原则相关
const showEditModal = ref(false)
const editing = ref(false)
const editPrincipleData = ref({
  id: 0,
  content: '',
  tags: ''
})

onMounted(() => {
  loadPrinciples()
})

async function loadPrinciples() {
  loading.value = true
  try {
    const res = await getPrincipleList(keyword.value || undefined)
    principles.value = res.data || []
  } catch (e) {
    console.error('加载原则失败', e)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 防抖搜索：300ms内不重复触发
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadPrinciples()
  }, 300)
}

async function onRefresh() {
  refreshing.value = true
  await loadPrinciples()
}

function parseTags(tags: string): string[] {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

async function handleCreate() {
  if (!newPrinciple.value.content.trim()) {
    uni.showToast({ title: '请输入原则内容', icon: 'none' })
    return
  }

  creating.value = true
  try {
    await createPrinciple({
      content: newPrinciple.value.content,
      sourceThreadId: newPrinciple.value.sourceThreadId ? Number(newPrinciple.value.sourceThreadId) : undefined,
      tags: newPrinciple.value.tags || undefined
    })
    uni.showToast({ title: '原则已创建', icon: 'success' })
    showCreateModal.value = false
    newPrinciple.value = { content: '', sourceThreadId: '', tags: '' }
    await loadPrinciples()
  } catch (e) {
    console.error('创建失败', e)
  } finally {
    creating.value = false
  }
}

function confirmDelete(id: number) {
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deletePrinciple(id)
          uni.showToast({ title: '已删除', icon: 'success' })
          await loadPrinciples()
        } catch (e) {
          console.error('删除失败', e)
        }
      }
    }
  })
}

function openEditPrinciple(p: any) {
  editPrincipleData.value = {
    id: p.id,
    content: p.content || '',
    tags: p.tags || ''
  }
  showEditModal.value = true
}

async function handleEdit() {
  if (!editPrincipleData.value.content.trim()) {
    uni.showToast({ title: '请输入原则内容', icon: 'none' })
    return
  }
  editing.value = true
  try {
    await updatePrinciple(editPrincipleData.value.id, {
      content: editPrincipleData.value.content,
      tags: editPrincipleData.value.tags || undefined
    })
    uni.showToast({ title: '原则已更新', icon: 'success' })
    showEditModal.value = false
    await loadPrinciples()
  } catch (e) {
    console.error('编辑失败', e)
    uni.showToast({ title: '编辑失败', icon: 'none' })
  } finally {
    editing.value = false
  }
}

function goThread(id: number) {
  uni.navigateTo({ url: `/pages/explore/detail?id=${id}` })
}

function goPrincipleDetail(id: number) {
  uni.navigateTo({ url: `/pages/principle/detail?id=${id}` })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
}

.search-bar {
  padding: 20rpx 30rpx;
}

.search-input {
  width: 100%;
  height: 76rpx;
  background: #FFFFFF;
  border-radius: 38rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.principle-list {
  height: calc(100vh - 180rpx);
}

.principle-card {
  position: relative;
}

.principle-content-area {
  margin-bottom: 16rpx;
}

.principle-text {
  font-size: 30rpx;
  color: #2D3436;
  line-height: 1.6;
  font-weight: 500;
}

.tag-area {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.tag-item {
  padding: 4rpx 16rpx;
  background: rgba(108, 92, 231, 0.08);
  color: #6C5CE7;
  border-radius: 8rpx;
  font-size: 22rpx;
}

.source-link {
  font-size: 24rpx;
  color: #0984E3;
  margin-top: 12rpx;
  display: block;
}

.principle-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1rpx solid #F0F0F0;
  padding-top: 16rpx;
}

.action-btn {
  padding: 8rpx 24rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
}

.action-btn.danger {
  color: #E17055;
  background: rgba(225, 112, 85, 0.08);
}

.action-btn.edit {
  color: #6C5CE7;
  background: rgba(108, 92, 231, 0.08);
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

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #2D3436;
}

.modal-close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #F5F6FA;
  color: #636E72;
  font-size: 28rpx;
}

.modal-close:active {
  background: #E8E8E8;
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
