<template>
  <view class="page">
    <!-- 加载中 -->
    <view v-if="loading" class="loading-state">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 加载失败 -->
    <view v-else-if="loadError" class="error-state">
      <text class="error-icon">⚠️</text>
      <text class="error-text">{{ loadError }}</text>
      <button class="retry-btn" @tap="loadData">重新加载</button>
    </view>

    <template v-else>
      <!-- 议题信息（固定高度） -->
      <view class="thread-info" v-if="thread">
        <view class="thread-header">
          <view class="thread-title-wrap">
            <text class="thread-topic">{{ thread.topic }}</text>
            <view class="thread-actions">
              <view v-if="thread.status !== 1" class="action-btn" @tap="handleEdit">
                <text>✏️</text>
              </view>
              <view class="action-btn delete-btn" @tap="handleDelete">
                <text>🗑️</text>
              </view>
              <view v-if="thread.status !== 1" class="close-btn" @tap="handleClose">
                <text class="close-icon">📋</text>
                <text class="close-text">总结关闭</text>
              </view>
            </view>
          </view>
          <text v-if="thread.description" class="thread-desc">{{ thread.description }}</text>
          <view class="thread-meta">
            <text class="thread-status" :class="thread.status === 1 ? 'closed' : 'open'">
              {{ thread.status === 1 ? '已关闭' : '进行中' }}
            </text>
            <text class="thread-time">{{ formatTime(thread.createdAt) }}</text>
          </view>
        </view>
      </view>

      <!-- 消息列表（自适应填充剩余空间） -->
      <view class="message-area">
        <scroll-view
          scroll-y
          class="message-scroll"
          :scroll-into-view="scrollToId"
          scroll-with-animation
        >
          <view v-if="messages.length === 0" class="empty-state">
            <text class="empty-icon">🎭</text>
            <text class="empty-text">沙盒已就位，换个视角发言吧</text>
            <text class="empty-hint">点击下方输入框，选择角色开始对话</text>
          </view>

          <view
            v-for="(msg, idx) in messages"
            :key="msg.id"
            :id="`msg-${msg.id}`"
            class="message-item"
            :class="{ 'self': msg.roleName === '我' }"
          >
            <view class="role-badge" :style="{ background: getRoleColor(msg.roleName) }">
              <text class="role-name">{{ msg.roleName }}</text>
            </view>
            <view class="msg-bubble">
              <text class="msg-content">{{ msg.content }}</text>
              <!-- 引用列表 -->
              <view v-if="msg.references && msg.references.length > 0" class="ref-list">
                <view
                  v-for="ref in msg.references"
                  :key="ref.id"
                  class="ref-tag"
                  @tap="navigateToRefTarget(ref)"
                >
                  <text class="ref-icon">🔗</text>
                  <text class="ref-label">{{ getRefLabel(ref) }}</text>
                </view>
              </view>
            </view>
            <view class="msg-footer">
              <text class="msg-time">{{ formatTime(msg.createdAt) }}</text>
              <view class="msg-actions">
                <text class="msg-action-btn" @tap="openReferencePicker(msg)">🔗 引用</text>
                <text
                  v-if="msg.references && msg.references.length > 0"
                  class="msg-action-btn danger"
                  @tap="handleRemoveReference(msg)"
                >取消引用</text>
              </view>
            </view>
          </view>
          <view style="height: 20rpx;"></view>
        </scroll-view>
      </view>

      <!-- 底部输入区（固定） -->
      <view class="bottom-area">
        <view class="input-area" v-if="thread && thread.status !== 1">
          <view class="role-selector" @tap="showRolePicker = true">
            <text class="current-role" :style="{ color: getRoleColor(currentRole) }">
              {{ currentRole }}
            </text>
            <text class="arrow">▼</text>
          </view>
          <input
            class="msg-input"
            v-model="inputText"
            placeholder="以这个视角说..."
            :adjust-position="true"
            @confirm="handleSend"
          />
          <view class="send-btn" :class="{ active: inputText.trim() }" @tap="handleSend">
            <text>发送</text>
          </view>
        </view>

        <view class="closed-hint" v-if="thread && thread.status === 1">
          <text>📋 此议题已总结关闭，不再接受发言</text>
        </view>
      </view>
    </template>

    <!-- 角色选择弹窗 -->
    <view v-if="showRolePicker" class="modal-mask" @tap="showRolePicker = false">
      <view class="role-picker" @tap.stop>
        <text class="picker-title">选择发言角色</text>
        <view class="role-list">
          <view
            v-for="role in roles"
            :key="role.name"
            class="role-option"
            :class="{ active: currentRole === role.name }"
            @tap="selectRole(role.name)"
          >
            <view class="role-dot" :style="{ background: role.color }"></view>
            <view class="role-info">
              <text class="role-option-name">{{ role.name }}</text>
              <text class="role-option-desc">{{ role.desc }}</text>
            </view>
          </view>
        </view>
        <!-- 自定义角色 -->
        <view class="custom-role">
          <input
            class="custom-input"
            v-model="customRoleName"
            placeholder="自定义角色名"
            :maxlength="10"
          />
          <button class="custom-btn" @tap="addCustomRole">添加</button>
        </view>
      </view>
    </view>

    <!-- 总结关闭确认弹窗 -->
    <view v-if="showCloseModal" class="modal-mask" @tap="showCloseModal = false">
      <view class="close-modal-content" @tap.stop>
        <view class="close-modal-header">
          <text class="close-modal-title">总结关闭</text>
          <view class="close-modal-x" @tap="showCloseModal = false">
            <text>✕</text>
          </view>
        </view>
        <text class="close-modal-hint">关闭后将不再接受发言，是否提取原则？</text>
        <view class="close-modal-btns">
          <button class="close-modal-btn cancel" @tap="handleDirectClose">直接关闭</button>
          <button class="close-modal-btn confirm" @tap="handleExtractPrinciple">提取原则</button>
        </view>
      </view>
    </view>

    <!-- 提取原则弹窗 -->
    <view v-if="showPrincipleModal" class="modal-mask" @tap="showPrincipleModal = false">
      <view class="modal-content" @tap.stop>
        <text class="modal-title">🧩 提取原则</text>
        <text class="modal-hint">从这次讨论中提炼出可复用的行动原则</text>
        <view class="input-group">
          <textarea
            class="input-field textarea"
            v-model="principleContent"
            placeholder="写出一条可执行的原则..."
            :maxlength="300"
          />
        </view>
        <view class="input-group">
          <text class="input-label">标签（逗号分隔）</text>
          <input class="input-field" v-model="principleTags" placeholder="决策,学习,沟通" />
        </view>
        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showPrincipleModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleCreatePrinciple">提取</button>
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

    <!-- 知识引用选择弹窗 -->
    <view v-if="showRefPicker" class="modal-mask" @tap="showRefPicker = false">
      <view class="ref-picker" @tap.stop>
        <text class="picker-title">🔗 添加知识引用</text>
        <text class="picker-hint">引用已有任务或原则，建立知识关联</text>

        <!-- 切换类型 -->
        <view class="ref-tabs">
          <view
            class="ref-tab"
            :class="{ active: refTargetType === 'TASK' }"
            @tap="refTargetType = 'TASK'"
          >📌 任务</view>
          <view
            class="ref-tab"
            :class="{ active: refTargetType === 'PRINCIPLE' }"
            @tap="refTargetType = 'PRINCIPLE'"
          >🧩 原则</view>
        </view>

        <!-- 任务列表 -->
        <scroll-view v-if="refTargetType === 'TASK'" scroll-y class="ref-list-scroll">
          <view
            v-if="refTaskList.length === 0"
            class="ref-empty"
          >暂无任务，去首页创建一个吧</view>
          <view
            v-for="task in refTaskList"
            :key="task.id"
            class="ref-option"
            @tap="handleCreateReference('TASK', task.id)"
          >
            <text class="ref-option-title">{{ task.title }}</text>
            <text class="ref-option-meta">{{ task.status === 'DONE' ? '✅' : '⏳' }} {{ fmtHours(task.budget || 0) }}</text>
          </view>
        </scroll-view>

        <!-- 原则列表 -->
        <scroll-view v-if="refTargetType === 'PRINCIPLE'" scroll-y class="ref-list-scroll">
          <view
            v-if="refPrincipleList.length === 0"
            class="ref-empty"
          >暂无原则，去原则库创建吧</view>
          <view
            v-for="p in refPrincipleList"
            :key="p.id"
            class="ref-option"
            @tap="handleCreateReference('PRINCIPLE', p.id)"
          >
            <text class="ref-option-title">{{ p.content.slice(0, 50) }}</text>
            <text class="ref-option-meta">{{ p.usageCount || 0 }}次使用</text>
          </view>
        </scroll-view>

        <view class="ref-picker-footer">
          <button class="ref-cancel-btn" @tap="showRefPicker = false">取消</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getThreadDetail, getMessageList, sendMessage, closeThread, updateThread, deleteThread, createPrinciple, createReference, deleteReference, getReferencesBySource, getBacklinksByTarget, getTaskList, getPrincipleList } from '@/api'
import type { Ref } from 'vue'

const thread = ref<any>(null)
const messages = ref<any[]>([])
const threadId = ref(0)
const inputText = ref('')
const scrollToId = ref('')
const showRolePicker = ref(false)
const showPrincipleModal = ref(false)
const showCloseModal = ref(false)
const principleContent = ref('')
const principleTags = ref('')
const customRoleName = ref('')
const loading = ref(true)
const loadError = ref('')
const showEditModal = ref(false)
const editingData = ref({ topic: '', description: '' })

const roles = ref([
  { name: '我', color: '#6C5CE7', desc: '第一人称，直接表达' },
  { name: '理性分析师', color: '#0984E3', desc: '数据和逻辑驱动' },
  { name: '魔鬼代言人', color: '#E17055', desc: '故意唱反调，挑战假设' },
  { name: '乐观者', color: '#00B894', desc: '看到可能性和机会' },
  { name: '悲观者', color: '#636E72', desc: '预见风险和问题' },
  { name: '远见者', color: '#FDCB6E', desc: '从长期视角思考' }
])

const currentRole = ref('我')

// ============ 知识引用相关 ============
const showRefPicker = ref(false)
const refTargetType = ref<'TASK' | 'PRINCIPLE'>('TASK')
const refTaskList = ref<any[]>([])
const refPrincipleList = ref<any[]>([])
const currentRefMsg = ref<any>(null)

// 加载引用可选列表
async function loadRefTargets() {
  try {
    const [taskRes, priRes] = await Promise.all([
      getTaskList(),
      getPrincipleList()
    ])
    refTaskList.value = taskRes.data || []
    refPrincipleList.value = priRes.data || []
  } catch (e) {
    console.error('加载引用列表失败', e)
  }
}

// 打开引用选择弹窗
async function openReferencePicker(msg: any) {
  currentRefMsg.value = msg
  await loadRefTargets()
  showRefPicker.value = true
}

// 创建引用
async function handleCreateReference(targetType: 'TASK' | 'PRINCIPLE', targetId: number) {
  if (!currentRefMsg.value) return
  try {
    await createReference({
      sourceType: 'MESSAGE',
      sourceId: currentRefMsg.value.id,
      targetType,
      targetId
    })
    uni.showToast({ title: '引用成功', icon: 'success' })
    showRefPicker.value = false
    await loadData()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '引用失败', icon: 'none' })
  }
}

// 移除引用（取该消息的第一个引用删除，或弹窗选择）
async function handleRemoveReference(msg: any) {
  if (!msg.references || msg.references.length === 0) return
  // 如果只有一个引用，直接删除
  if (msg.references.length === 1) {
    try {
      await deleteReference(msg.references[0].id)
      uni.showToast({ title: '已取消引用', icon: 'success' })
      await loadData()
    } catch (e) {
      console.error('取消引用失败', e)
    }
    return
  }
  // 多个引用时让用户选择
  const labels = msg.references.map((r: any) => getRefLabel(r))
  uni.showActionSheet({
    itemList: labels,
    success: async (res) => {
      try {
        await deleteReference(msg.references[res.tapIndex].id)
        uni.showToast({ title: '已取消引用', icon: 'success' })
        await loadData()
      } catch (e) {
        console.error('取消引用失败', e)
      }
    }
  })
}

// 获取引用标签文字
function getRefLabel(ref: any): string {
  const typeMap: Record<string, string> = { TASK: '📌 任务', PRINCIPLE: '🧩 原则', THREAD: '💬 议题', MESSAGE: '💬 消息' }
  return `${typeMap[ref.targetType] || ref.targetType} #${ref.targetId}`
}

// 点击引用标签跳转
function navigateToRefTarget(ref: any) {
  if (ref.targetType === 'TASK') {
    uni.navigateTo({ url: `/pages/index/detail?id=${ref.targetId}` })
  } else if (ref.targetType === 'PRINCIPLE') {
    uni.navigateTo({ url: `/pages/principle/detail?id=${ref.targetId}` })
  } else if (ref.targetType === 'THREAD') {
    uni.navigateTo({ url: `/pages/explore/detail?id=${ref.targetId}` })
  }
}

onLoad((options: any) => {
  const id = Number(options?.id || 0)
  if (id) {
    threadId.value = id
    loadData()
  } else {
    loading.value = false
    loadError.value = '缺少议题ID'
  }
})

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [threadRes, msgRes] = await Promise.all([
      getThreadDetail(threadId.value),
      getMessageList(threadId.value)
    ])
    thread.value = threadRes.data
    const msgs = msgRes.data || []
    // 为每条消息加载引用关系
    const msgsWithRefs = await Promise.all(
      msgs.map(async (msg: any) => {
        try {
          const refRes: any = await getReferencesBySource('MESSAGE', msg.id)
          msg.references = refRes.data || []
        } catch {
          msg.references = []
        }
        return msg
      })
    )
    messages.value = msgsWithRefs
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    console.error('加载数据失败', e)
    loadError.value = e?.message || '加载失败，请检查网络或权限'
  } finally {
    loading.value = false
  }
}

function getRoleColor(roleName: string): string {
  const role = roles.value.find(r => r.name === roleName)
  return role ? role.color : '#636E72'
}

function selectRole(name: string) {
  currentRole.value = name
  showRolePicker.value = false
}

function addCustomRole() {
  if (!customRoleName.value.trim()) return
  const name = customRoleName.value.trim()
  if (roles.value.some(r => r.name === name)) {
    currentRole.value = name
    showRolePicker.value = false
    customRoleName.value = ''
    return
  }
  const colors = ['#E84393', '#00CEC9', '#FAB1A0', '#55EFC4', '#74B9FF']
  const color = colors[roles.value.length % colors.length]
  roles.value.push({ name, color, desc: '自定义角色' })
  currentRole.value = name
  customRoleName.value = ''
  showRolePicker.value = false
}

async function handleSend() {
  if (!inputText.value.trim()) return
  try {
    await sendMessage(threadId.value, {
      roleName: currentRole.value,
      content: inputText.value.trim()
    })
    inputText.value = ''
    await loadData()
  } catch (e) {
    console.error('发送失败', e)
  }
}

function handleClose() {
  showCloseModal.value = true
}

async function handleDirectClose() {
  showCloseModal.value = false
  try {
    await closeThread(threadId.value)
    uni.showToast({ title: '议题已关闭', icon: 'success' })
    await loadData()
  } catch (e) {
    console.error('关闭失败', e)
  }
}

async function handleExtractPrinciple() {
  showCloseModal.value = false
  try {
    await closeThread(threadId.value)
    showPrincipleModal.value = true
  } catch (e) {
    console.error('关闭失败', e)
  }
}

async function handleCreatePrinciple() {
  if (!principleContent.value.trim()) {
    uni.showToast({ title: '请输入原则内容', icon: 'none' })
    return
  }
  try {
    await createPrinciple({
      content: principleContent.value.trim(),
      sourceThreadId: threadId.value,
      tags: principleTags.value || undefined
    })
    uni.showToast({ title: '原则已提取 ✨', icon: 'success' })
    showPrincipleModal.value = false
    principleContent.value = ''
    principleTags.value = ''
    await loadData()
  } catch (e) {
    console.error('提取原则失败', e)
  }
}

function handleEdit() {
  if (!thread.value) return
  editingData.value = { topic: thread.value.topic, description: thread.value.description || '' }
  showEditModal.value = true
}

async function handleUpdate() {
  if (!editingData.value.topic.trim()) {
    uni.showToast({ title: '请输入议题名称', icon: 'none' })
    return
  }
  try {
    await updateThread(threadId.value, editingData.value)
    uni.showToast({ title: '更新成功', icon: 'success' })
    showEditModal.value = false
    await loadData()
  } catch (e) {
    console.error('更新失败', e)
  }
}

async function handleDelete() {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除议题「${thread.value?.topic}」吗？关联的发言也会一并删除。`,
    confirmText: '删除',
    confirmColor: '#E17055',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteThread(threadId.value)
          uni.showToast({ title: '已删除', icon: 'success' })
          uni.navigateBack()
        } catch (e) {
          console.error('删除失败', e)
        }
      }
    }
  })
}

function scrollToBottom() {
  if (messages.value.length > 0) {
    const lastMsg = messages.value[messages.value.length - 1]
    scrollToId.value = `msg-${lastMsg.id}`
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return time.slice(5, 16).replace('T', ' ')
}

function fmtHours(h: number): string {
  if (h === 0) return '0h'
  if (h < 1) return `${Math.round(h * 60)}分钟`
  const intPart = Math.floor(h)
  const decPart = Math.round((h - intPart) * 10)
  return decPart > 0 ? `${intPart}.${decPart}h` : `${intPart}h`
}
</script>

<style scoped>
/* === 页面根容器 === */
.page {
  height: 100vh;
  background: #F5F6FA;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* === 加载/错误状态 === */
.loading-state, .error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx;
}
.loading-text { font-size: 30rpx; color: #B2BEC3; }
.error-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.error-text { font-size: 30rpx; color: #636E72; margin-bottom: 30rpx; text-align: center; }
.retry-btn {
  padding: 16rpx 48rpx;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
  border-radius: 16rpx;
  font-size: 30rpx;
  border: none;
}

/* === 议题信息区（固定高度，不压缩） === */
.thread-info {
  flex-shrink: 0;
  background: #FFFFFF;
  margin: 0 20rpx;
  margin-top: 12rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  border-left: 6rpx solid #6C5CE7;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.thread-header {
  display: flex;
  flex-direction: column;
}

.thread-title-wrap {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}

.thread-topic {
  font-size: 32rpx;
  font-weight: 700;
  color: #2D3436;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  word-break: break-all;
}

.close-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 16rpx;
  background: rgba(108, 92, 231, 0.1);
  border-radius: 12rpx;
  flex-shrink: 0;
  white-space: nowrap;
}
.close-icon { font-size: 24rpx; }
.close-text { font-size: 24rpx; color: #6C5CE7; }

.thread-desc {
  font-size: 26rpx;
  color: #636E72;
  margin-top: 10rpx;
  line-height: 1.5;
  word-break: break-all;
}

.thread-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 12rpx;
}
.thread-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-weight: 600;
}
.thread-status.open { background: rgba(0, 184, 148, 0.1); color: #00B894; }
.thread-status.closed { background: rgba(99, 110, 114, 0.1); color: #636E72; }
.thread-time { font-size: 22rpx; color: #B2BEC3; }

/* === 消息区域（自适应填充剩余空间） === */
.message-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 0 20rpx;
  margin-top: 12rpx;
}

.message-scroll {
  height: 100%;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
}
.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 30rpx; color: #636E72; margin-bottom: 12rpx; }
.empty-hint { font-size: 24rpx; color: #B2BEC3; }

.message-item {
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.message-item.self { align-items: flex-end; }

.role-badge {
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  margin-bottom: 8rpx;
}
.role-name { font-size: 22rpx; color: #FFFFFF; font-weight: 600; }

.msg-bubble {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  max-width: 80%;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  word-break: break-all;
}
.message-item.self .msg-bubble { background: rgba(108, 92, 231, 0.08); }
.msg-content { font-size: 28rpx; color: #2D3436; line-height: 1.6; }
.msg-time { font-size: 20rpx; color: #B2BEC3; margin-top: 6rpx; }

/* === 底部输入区（固定，不压缩） === */
.bottom-area {
  flex-shrink: 0;
  background: #FFFFFF;
  border-top: 1rpx solid #E8E8E8;
  padding-bottom: env(safe-area-inset-bottom);
}

.input-area {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  gap: 12rpx;
}

.role-selector {
  display: flex;
  align-items: center;
  padding: 10rpx 16rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  min-width: 120rpx;
  justify-content: center;
  flex-shrink: 0;
}
.current-role { font-size: 24rpx; font-weight: 600; margin-right: 6rpx; }
.arrow { font-size: 18rpx; color: #B2BEC3; }

.msg-input {
  flex: 1;
  height: 72rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  min-width: 0;
}

.send-btn {
  padding: 10rpx 28rpx;
  background: #DFE6E9;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #B2BEC3;
  flex-shrink: 0;
}
.send-btn.active {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
}

.closed-hint {
  text-align: center;
  padding: 24rpx;
  font-size: 28rpx;
  color: #B2BEC3;
}

/* === 角色选择弹窗 === */
.role-picker {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx;
  max-height: 80vh;
  overflow-y: auto;
}
.picker-title { font-size: 32rpx; font-weight: 700; color: #2D3436; display: block; margin-bottom: 30rpx; }
.role-list { margin-bottom: 20rpx; }
.role-option {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F5F6FA;
}
.role-option.active { background: rgba(108, 92, 231, 0.04); border-radius: 12rpx; padding: 24rpx 16rpx; }
.role-dot { width: 24rpx; height: 24rpx; border-radius: 50%; margin-right: 20rpx; flex-shrink: 0; }
.role-info { display: flex; flex-direction: column; }
.role-option-name { font-size: 30rpx; font-weight: 600; color: #2D3436; }
.role-option-desc { font-size: 24rpx; color: #B2BEC3; margin-top: 4rpx; }
.custom-role { display: flex; gap: 12rpx; margin-top: 20rpx; }
.custom-input { flex: 1; height: 72rpx; background: #F5F6FA; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; }
.custom-btn { padding: 0 30rpx; height: 72rpx; line-height: 72rpx; background: #6C5CE7; color: #FFFFFF; border-radius: 12rpx; font-size: 28rpx; border: none; }

/* === 提取原则弹窗 === */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
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
.modal-title { font-size: 36rpx; font-weight: 700; color: #2D3436; display: block; margin-bottom: 10rpx; }
.modal-hint { font-size: 26rpx; color: #B2BEC3; display: block; margin-bottom: 30rpx; }
.input-group { margin-bottom: 28rpx; }
.input-label { font-size: 26rpx; color: #636E72; margin-bottom: 10rpx; display: block; }
.input-field { width: 100%; height: 80rpx; background: #F5F6FA; border-radius: 12rpx; padding: 0 24rpx; font-size: 28rpx; color: #2D3436; box-sizing: border-box; }
.input-field.textarea { height: 200rpx; padding: 20rpx 24rpx; line-height: 1.5; }
.modal-btns { display: flex; gap: 20rpx; margin-top: 30rpx; }
.modal-btn { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 16rpx; font-size: 30rpx; text-align: center; border: none; }
.modal-btn.cancel { background: #F5F6FA; color: #636E72; }
.modal-btn.confirm { background: linear-gradient(135deg, #6C5CE7, #A29BFE); color: #FFFFFF; }

/* === 总结关闭确认弹窗 === */
.close-modal-content {
  width: 80%;
  max-width: 600rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 40rpx;
}
.close-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.close-modal-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #2D3436;
}
.close-modal-x {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #F5F6FA;
  color: #636E72;
  font-size: 28rpx;
  margin-left: 16rpx;
  flex-shrink: 0;
}
.close-modal-x:active {
  background: #E8E8E8;
}
.close-modal-hint {
  font-size: 28rpx;
  color: #636E72;
  line-height: 1.5;
  display: block;
  margin-bottom: 40rpx;
}
.close-modal-btns {
  display: flex;
  gap: 20rpx;
}
.close-modal-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  text-align: center;
  border: none;
}
.close-modal-btn.cancel {
  background: #F5F6FA;
  color: #636E72;
}
.close-modal-btn.confirm {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
}

/* === 消息底部操作区 === */
.msg-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
  gap: 12rpx;
}
.msg-time { font-size: 20rpx; color: #B2BEC3; flex-shrink: 0; }
.msg-actions { display: flex; gap: 16rpx; }
.msg-action-btn {
  font-size: 20rpx;
  color: #6C5CE7;
  padding: 4rpx 8rpx;
  border-radius: 6rpx;
  background: rgba(108, 92, 231, 0.08);
}
.msg-action-btn.danger {
  color: #E17055;
  background: rgba(225, 112, 85, 0.08);
}

/* === 引用列表 === */
.ref-list {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.ref-tag {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 6rpx 12rpx;
  background: rgba(108, 92, 231, 0.06);
  border-radius: 8rpx;
  border: 1rpx solid rgba(108, 92, 231, 0.15);
  align-self: flex-start;
}
.ref-tag:active { background: rgba(108, 92, 231, 0.12); }
.ref-icon { font-size: 20rpx; }
.ref-label { font-size: 22rpx; color: #6C5CE7; }

/* === 引用选择弹窗 === */
.ref-picker {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx;
  max-height: 85vh;
  overflow-y: auto;
  z-index: 210;
}
.picker-title { font-size: 32rpx; font-weight: 700; color: #2D3436; display: block; margin-bottom: 8rpx; }
.picker-hint { font-size: 24rpx; color: #B2BEC3; display: block; margin-bottom: 24rpx; }

.ref-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.ref-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #636E72;
  background: #F5F6FA;
  font-weight: 600;
}
.ref-tab.active {
  background: rgba(108, 92, 231, 0.1);
  color: #6C5CE7;
}

.ref-list-scroll {
  max-height: 50vh;
  overflow-y: auto;
}
.ref-empty {
  text-align: center;
  padding: 60rpx 0;
  font-size: 26rpx;
  color: #B2BEC3;
}
.ref-option {
  padding: 24rpx 16rpx;
  border-bottom: 1rpx solid #F5F6FA;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ref-option:active { background: #F5F6FA; }
.ref-option-title { font-size: 28rpx; color: #2D3436; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ref-option-meta { font-size: 22rpx; color: #B2BEC3; flex-shrink: 0; margin-left: 12rpx; }

.ref-picker-footer {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F5F6FA;
}
.ref-cancel-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #F5F6FA;
  color: #636E72;
  border-radius: 16rpx;
  font-size: 30rpx;
  border: none;
}
</style>
