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
      <button class="retry-btn" @tap="loadDetail">重新加载</button>
    </view>

    <!-- ========== 编辑模式 ========== -->
    <view v-else-if="editing && task" class="edit-content">
      <view class="edit-header">
        <text class="edit-header-title">编辑任务</text>
        <view class="edit-header-x" @tap="cancelEdit">
          <text>✕</text>
        </view>
      </view>

      <scroll-view scroll-y class="edit-scroll">
        <!-- 标题 -->
        <view class="card">
          <text class="section-label">任务名称 *</text>
          <input class="edit-input" v-model="editForm.title" placeholder="做什么？" />
        </view>

        <!-- 锚点 -->
        <view class="card">
          <text class="section-label">⚓ 现实锚点（为什么做）</text>
          <textarea
            class="edit-textarea"
            v-model="editForm.anchorText"
            placeholder="这件事对什么有意义？"
            :maxlength="500"
          />
        </view>

        <!-- 时间预算 -->
        <view class="card">
          <text class="section-label">⏱ 时间预算（小时）</text>
          <input class="edit-input" v-model="editForm.budget" type="digit" placeholder="预计花多少小时" />
        </view>

        <!-- 实际用时 -->
        <view class="card">
          <text class="section-label">⏱ 实际用时（小时）</text>
          <input class="edit-input" v-model="editForm.actualTime" type="digit" placeholder="实际花了多少小时" />
        </view>

        <!-- 截止日期 -->
        <view class="card">
          <text class="section-label">📅 截止日期</text>
          <picker mode="date" :value="editForm.dueDate || ''" @change="onDueDateChange">
            <view class="date-picker-value">
              <text :class="{ placeholder: !editForm.dueDate }">
                {{ editForm.dueDate || '点击选择日期' }}
              </text>
              <text v-if="editForm.dueDate" class="clear-date" @tap.stop="editForm.dueDate = ''">✕</text>
            </view>
          </picker>
        </view>

        <!-- 状态 -->
        <view class="card">
          <text class="section-label">📌 状态</text>
          <view class="status-options">
            <view
              class="status-option"
              :class="{ active: editForm.status === 0 }"
              @tap="editForm.status = 0"
            >
              <text>● 进行中</text>
            </view>
            <view
              class="status-option"
              :class="{ active: editForm.status === 1 }"
              @tap="editForm.status = 1"
            >
              <text>✓ 已完成</text>
            </view>
            <view
              class="status-option"
              :class="{ active: editForm.status === 2 }"
              @tap="editForm.status = 2"
            >
              <text>✗ 已放弃</text>
            </view>
          </view>
        </view>

        <!-- 描述 -->
        <view class="card">
          <text class="section-label">📝 描述</text>
          <textarea
            class="edit-textarea"
            v-model="editForm.description"
            placeholder="补充说明"
            :maxlength="500"
          />
        </view>

        <!-- 关联原则 -->
        <view class="card">
          <text class="section-label">🧩 关联原则</text>
          <view class="principle-selector">
            <view v-if="editForm.principleId" class="selected-principle">
              <text class="selected-principle-text">{{ selectedPrincipleContent }}</text>
              <text class="clear-principle" @tap="clearPrinciple">✕ 解除关联</text>
            </view>
            <view v-else class="principle-picker" @tap="openPrinciplePicker">
              <text class="picker-placeholder">点击选择原则</text>
            </view>
            <view class="principle-actions-row">
              <text class="principle-action-link" @tap="openPrinciplePicker">
                {{ editForm.principleId ? '切换原则' : '从原则库选择' }}
              </text>
              <text class="principle-action-link" @tap="showNewPrincipleModal = true">新建原则</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <!-- 保存/取消 -->
      <view class="edit-footer">
        <button class="footer-btn cancel" @tap="cancelEdit">取消</button>
        <button class="footer-btn confirm" @tap="saveEdit" :loading="saving">保存</button>
      </view>
    </view>

    <!-- ========== 查看模式 ========== -->
    <view v-else-if="task" class="detail-content">
      <!-- 标题和状态 -->
      <view class="card">
        <view class="title-row">
          <text class="detail-title">{{ task.title }}</text>
          <view class="status-tag" :class="statusClass">
            {{ statusText }}
          </view>
        </view>
      </view>

      <!-- 锚点区 -->
      <view v-if="task.anchorText" class="card anchor-card">
        <text class="section-label">⚓ 现实锚点</text>
        <text class="anchor-content">{{ task.anchorText }}</text>
      </view>

      <!-- 时间预算 -->
      <view class="card">
        <text class="section-label">⏱ 时间预算</text>
        <view class="time-row">
          <view class="time-block">
            <text class="time-value">{{ displayBudget }}</text>
            <text class="time-unit">小时预算</text>
          </view>
          <view class="time-divider"></view>
          <view class="time-block">
            <text class="time-value actual">{{ displayActualTime }}</text>
            <text class="time-unit">小时实际</text>
          </view>
        </view>
        <!-- 进度条 -->
        <view v-if="task.budget" class="progress-bar">
          <view
            class="progress-fill"
            :class="{ over: progress > 100 }"
            :style="{ width: Math.min(progress, 100) + '%' }"
          ></view>
        </view>
        <text v-if="task.budget" class="progress-text">
          {{ progress > 100 ? '⚠ 超出预算' : progress.toFixed(0) + '% 已消耗' }}
        </text>
      </view>

      <!-- 截止日期 -->
      <view v-if="task.dueDate" class="card">
        <text class="section-label">📅 截止日期</text>
        <text class="desc-content">{{ task.dueDate }}</text>
        <text v-if="isOverdue" class="overdue-hint">⚠ 已过期</text>
      </view>

      <!-- 描述 -->
      <view v-if="task.description" class="card">
        <text class="section-label">📝 描述</text>
        <text class="desc-content">{{ task.description }}</text>
      </view>

      <!-- 关联原则 -->
      <view v-if="task.principleId" class="card principle-card">
        <text class="section-label">🧩 关联原则</text>
        <text class="principle-content-text">{{ linkedPrincipleContent || '加载中...' }}</text>
        <view v-if="linkedPrincipleTags.length" class="principle-tags-row">
          <text v-for="tag in linkedPrincipleTags" :key="tag" class="mini-tag">{{ tag }}</text>
        </view>
      </view>

      <!-- 被引用列表（反向链接） -->
      <view v-if="backlinks.length > 0" class="card backlink-card">
        <text class="section-label">🔗 被引用（{{ backlinks.length }}）</text>
        <view
          v-for="bl in backlinks"
          :key="bl.id"
          class="backlink-item"
          @tap="navigateToSource(bl)"
        >
          <text class="backlink-icon">💬</text>
          <view class="backlink-info">
            <text class="backlink-source">消息 #{{ bl.sourceId }}</text>
            <text class="backlink-hint">来自探索议题</text>
          </view>
          <text class="backlink-arrow">→</text>
        </view>
      </view>

      <!-- 操作区 -->
      <view class="action-area">
        <!-- 心流模式入口 -->
        <button v-if="task.status === 0" class="action-btn flow-btn" @tap="goFlowMode">⚡ 心流模式</button>
        <!-- 记录今日用时快捷按钮 -->
        <button v-if="task.status !== 2" class="action-btn log-btn" @tap="openLogModal">⏱ 记录今日用时</button>
        <button v-if="task.status !== 2" class="action-btn primary" @tap="toggleStatus">
          {{ task.status === 1 ? '↩ 恢复进行中' : '✓ 标记完成' }}
        </button>
        <button v-if="task.status !== 2" class="action-btn abandon" @tap="handleAbandon">✗ 放弃</button>
        <button v-if="task.status === 2" class="action-btn restore" @tap="handleRestore">↩ 恢复进行中</button>
        <button class="action-btn edit" @tap="goEdit">编辑</button>
        <button class="action-btn danger-btn" @tap="confirmDelete">删除</button>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">⏳</text>
      <text class="empty-text">暂无数据</text>
    </view>

    <!-- 原则选择器弹窗 -->
    <view v-if="showPrinciplePicker" class="modal-mask" @tap="showPrinciplePicker = false">
      <view class="modal-content principle-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">选择原则</text>
          <view class="modal-close" @tap="showPrinciplePicker = false">
            <text>✕</text>
          </view>
        </view>
        <view class="principle-search">
          <input class="principle-search-input" v-model="principleKeyword" placeholder="搜索原则..." @input="searchPrinciples" />
        </view>
        <scroll-view scroll-y class="principle-list-modal">
          <view v-if="principleOptions.length === 0" class="principle-empty">
            <text class="principle-empty-text">没有找到原则</text>
          </view>
          <view
            v-for="p in principleOptions"
            :key="p.id"
            class="principle-option"
            :class="{ active: editForm.principleId === p.id }"
            @tap="selectPrinciple(p)"
          >
            <text class="principle-option-text">{{ p.content }}</text>
            <view v-if="p.tags" class="principle-option-tags">
              <text v-for="tag in parsePrincipleTags(p.tags)" :key="tag" class="mini-tag">{{ tag }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 新建原则弹窗 -->
    <view v-if="showNewPrincipleModal" class="modal-mask" @tap="showNewPrincipleModal = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">🧩 新建原则</text>
          <view class="modal-close" @tap="showNewPrincipleModal = false">
            <text>✕</text>
          </view>
        </view>
        <view class="input-group">
          <text class="input-label">原则内容 *</text>
          <textarea
            class="input-field textarea"
            v-model="newPrincipleContent"
            placeholder="写出一条可执行的行动原则..."
            :maxlength="500"
          />
        </view>
        <view class="input-group">
          <text class="input-label">标签（逗号分隔）</text>
          <input class="input-field" v-model="newPrincipleTags" placeholder="决策,学习,沟通" />
        </view>
        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showNewPrincipleModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleCreatePrinciple" :loading="creatingPrinciple">创建并关联</button>
        </view>
      </view>
    </view>

    <!-- 记录今日用时弹窗 -->
    <view v-if="showLogModal" class="modal-mask" @tap="showLogModal = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">⏱ 记录今日用时</text>
          <view class="modal-close" @tap="showLogModal = false">
            <text>✕</text>
          </view>
        </view>

        <view class="log-task-name">
          <text class="log-task-label">任务：</text>
          <text class="log-task-title">{{ task?.title }}</text>
        </view>

        <view class="input-group">
          <text class="input-label">日期</text>
          <picker mode="date" :value="logForm.date" @change="onLogDateChange">
            <view class="date-picker-value">
              <text>{{ logForm.date }}</text>
            </view>
          </picker>
        </view>

        <view class="input-group">
          <text class="input-label">当日预算（小时）</text>
          <input class="input-field" v-model="logForm.budget" type="digit" placeholder="预计花多少小时" />
        </view>

        <view class="input-group">
          <text class="input-label">当日实际用时（小时）</text>
          <input class="input-field" v-model="logForm.actualTime" type="digit" placeholder="实际花了多少小时" />
        </view>

        <!-- 今日已有记录提示 -->
        <view v-if="existingLog" class="existing-log-hint">
          <text class="hint-text">📅 该日期已有记录，提交将覆盖更新</text>
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showLogModal = false">取消</button>
          <button class="modal-btn confirm" @tap="handleLogAttention" :loading="logging">提交</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getTaskDetail, deleteTask, toggleTaskStatus, updateTask, getPrincipleList, getPrincipleDetail, createPrinciple, createAttentionLog, getAttentionLogsByTask, getBacklinksByTarget } from '@/api'
import { fmtHours, fmtHoursNum } from '@/utils/format'

const task = ref<any>(null)
const taskId = ref(0)
const loading = ref(true)
const loadError = ref('')

// 编辑模式
const editing = ref(false)
const saving = ref(false)
const editForm = ref({
  title: '',
  anchorText: '',
  budget: '',
  actualTime: '',
  dueDate: '',
  status: 0,
  description: '',
  principleId: null as number | null
})

// 原则选择器相关
const showPrinciplePicker = ref(false)
const principleOptions = ref<any[]>([])
const principleKeyword = ref('')
const selectedPrincipleContent = ref('')

// 新建原则相关
const showNewPrincipleModal = ref(false)
const newPrincipleContent = ref('')
const newPrincipleTags = ref('')
const creatingPrinciple = ref(false)

// 注意力日志相关
const showLogModal = ref(false)
const logging = ref(false)
const existingLog = ref(false)
const logForm = ref({
  date: '',
  budget: '',
  actualTime: ''
})

// 查看模式 - 关联原则详情
const linkedPrincipleContent = ref('')
const linkedPrincipleTags = ref<string[]>([])

// 查看模式 - 被引用列表（反向链接）
const backlinks = ref<any[]>([])
const loadingBacklinks = ref(false)

const statusClass = computed(() => {
  if (!task.value) return ''
  const s = task.value.status
  if (s === 1) return 'status-done'
  if (s === 2) return 'status-abandoned'
  return 'status-pending'
})

const statusText = computed(() => {
  if (!task.value) return ''
  const s = task.value.status
  if (s === 1) return '✓ 完成'
  if (s === 2) return '✗ 放弃'
  return '● 进行中'
})

// 预算/实际 显示（格式化）
const displayBudget = computed(() => fmtHoursNum(task.value?.budget || 0))
const displayActualTime = computed(() => fmtHoursNum(task.value?.actualTime || 0))

const progress = computed(() => {
  if (!task.value || !task.value.budget) return 0
  return ((task.value.actualTime || 0) / task.value.budget) * 100
})

const isOverdue = computed(() => {
  if (!task.value || !task.value.dueDate) return false
  return new Date(task.value.dueDate) < new Date()
})

onLoad((options: any) => {
  const id = Number(options?.id || 0)
  if (id) {
    taskId.value = id
    loadDetail()
  } else {
    loading.value = false
    loadError.value = '缺少任务ID'
  }
})

async function loadDetail() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getTaskDetail(taskId.value)
    task.value = res.data
    // 加载关联原则详情
    if (task.value?.principleId) {
      loadLinkedPrinciple(task.value.principleId)
    } else {
      linkedPrincipleContent.value = ''
      linkedPrincipleTags.value = []
    }
    // 加载被引用列表（反向链接）
    await loadBacklinks()
  } catch (e: any) {
    console.error('加载详情失败', e)
    loadError.value = e?.message || '加载失败，请检查网络或权限'
  } finally {
    loading.value = false
  }
}

async function loadLinkedPrinciple(id: number) {
  try {
    const res = await getPrincipleDetail(id)
    const p = res.data
    linkedPrincipleContent.value = p?.content || ''
    linkedPrincipleTags.value = p?.tags ? parsePrincipleTags(p.tags) : []
  } catch (e) {
    linkedPrincipleContent.value = '原则 #' + id
    linkedPrincipleTags.value = []
  }
}

// 加载被引用列表（反向链接）
async function loadBacklinks() {
  loadingBacklinks.value = true
  try {
    const res: any = await getBacklinksByTarget('TASK', taskId.value)
    backlinks.value = res.data || []
  } catch (e) {
    console.error('加载被引用列表失败', e)
    backlinks.value = []
  } finally {
    loadingBacklinks.value = false
  }
}

// 跳转到引用此任务的消息所在议题
function navigateToSource(ref: any) {
  if (ref.sourceType === 'MESSAGE' && ref.sourceId) {
    // 需要先获取消息所属的 threadId
    uni.showToast({ title: '正在跳转...', icon: 'loading' })
    // 简化：直接提示用户去探索轨查找
    uni.showModal({
      title: '引用来源',
      content: `此任务被消息 #${ref.sourceId} 引用，请到对应的探索议题中查看。`,
      showCancel: false
    })
  }
}

// ========== 编辑功能 ==========
function goEdit() {
  if (!task.value) return
  const t = task.value
  editForm.value = {
    title: t.title || '',
    anchorText: t.anchorText || '',
    budget: t.budget ? String(t.budget) : '',
    actualTime: t.actualTime ? String(t.actualTime) : '',
    dueDate: t.dueDate || '',
    status: t.status ?? 0,
    description: t.description || '',
    principleId: t.principleId || null
  }
  selectedPrincipleContent.value = t.principleId ? linkedPrincipleContent.value : ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

function onDueDateChange(e: any) {
  editForm.value.dueDate = e.detail.value || ''
}

async function saveEdit() {
  if (!editForm.value.title.trim()) {
    uni.showToast({ title: '请输入任务名称', icon: 'none' })
    return
  }

  saving.value = true
  try {
    const budgetVal = editForm.value.budget ? Number(editForm.value.budget) : undefined
    const actualVal = editForm.value.actualTime ? Number(editForm.value.actualTime) : undefined

    await updateTask(taskId.value, {
      title: editForm.value.title,
      anchorText: editForm.value.anchorText,
      description: editForm.value.description,
      budget: budgetVal,
      actualTime: actualVal,
      dueDate: editForm.value.dueDate || undefined,
      status: editForm.value.status,
      principleId: editForm.value.principleId || 0
    })
    uni.showToast({ title: '保存成功', icon: 'success' })
    editing.value = false
    await loadDetail()
  } catch (e: any) {
    console.error('保存失败', e)
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

// ========== 状态切换 ==========
async function toggleStatus() {
  if (!task.value) return
  try {
    const newStatus = task.value.status === 1 ? 0 : 1
    await toggleTaskStatus(task.value.id, newStatus)
    uni.showToast({ title: newStatus === 1 ? '已完成' : '已恢复', icon: 'success' })
    await loadDetail()
  } catch (e) {
    console.error('状态切换失败', e)
  }
}

async function handleAbandon() {
  uni.showModal({
    title: '确认放弃',
    content: '放弃后可在编辑中恢复，确定？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await toggleTaskStatus(taskId.value, 2)
          uni.showToast({ title: '已放弃', icon: 'success' })
          await loadDetail()
        } catch (e) {
          console.error('操作失败', e)
        }
      }
    }
  })
}

async function handleRestore() {
  try {
    await toggleTaskStatus(taskId.value, 0)
    uni.showToast({ title: '已恢复进行中', icon: 'success' })
    await loadDetail()
  } catch (e) {
    console.error('操作失败', e)
  }
}

function goFlowMode() {
  uni.navigateTo({ url: '/pages/index/flow' })
}

function confirmDelete() {
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteTask(taskId.value)
          uni.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 500)
        } catch (e) {
          console.error('删除失败', e)
        }
      }
    }
  })
}

// ========== 原则选择器 ==========
function parsePrincipleTags(tags: string): string[] {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

async function searchPrinciples() {
  try {
    const res = await getPrincipleList(principleKeyword.value || undefined)
    principleOptions.value = res.data || []
  } catch (e) {
    console.error('搜索原则失败', e)
  }
}

async function openPrinciplePicker() {
  showPrinciplePicker.value = true
  principleKeyword.value = ''
  await searchPrinciples()
}

function selectPrinciple(p: any) {
  editForm.value.principleId = p.id
  selectedPrincipleContent.value = p.content
  showPrinciplePicker.value = false
}

function clearPrinciple() {
  editForm.value.principleId = null
  selectedPrincipleContent.value = ''
}

async function handleCreatePrinciple() {
  if (!newPrincipleContent.value.trim()) {
    uni.showToast({ title: '请输入原则内容', icon: 'none' })
    return
  }
  creatingPrinciple.value = true
  try {
    const res = await createPrinciple({
      content: newPrincipleContent.value,
      tags: newPrincipleTags.value || undefined
    })
    const created = res.data
    editForm.value.principleId = created.id
    selectedPrincipleContent.value = created.content
    showNewPrincipleModal.value = false
    newPrincipleContent.value = ''
    newPrincipleTags.value = ''
    uni.showToast({ title: '原则已创建并关联', icon: 'success' })
  } catch (e) {
    console.error('创建原则失败', e)
    uni.showToast({ title: '创建失败', icon: 'none' })
  } finally {
    creatingPrinciple.value = false
  }
}

// ========== 注意力日志 ==========
function getTodayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function openLogModal() {
  const today = getTodayStr()
  logForm.value = {
    date: today,
    budget: task.value?.budget ? String(task.value.budget) : '',
    actualTime: ''
  }
  existingLog.value = false

  // 检查是否已有该天的日志
  try {
    const res = await getAttentionLogsByTask(taskId.value)
    const logs = res.data || []
    const existLog = logs.find((l: any) => l.logDate === today)
    if (existLog) {
      existingLog.value = true
      logForm.value.budget = existLog.budget ? String(existLog.budget) : ''
      logForm.value.actualTime = existLog.actualTime ? String(existLog.actualTime) : ''
    }
  } catch (e) {
    console.error('查询日志失败', e)
  }

  showLogModal.value = true
}

function onLogDateChange(e: any) {
  logForm.value.date = e.detail.value || getTodayStr()
}

async function handleLogAttention() {
  const actualTime = logForm.value.actualTime ? Number(logForm.value.actualTime) : 0
  if (actualTime <= 0) {
    uni.showToast({ title: '请输入实际用时', icon: 'none' })
    return
  }

  logging.value = true
  try {
    await createAttentionLog({
      taskId: taskId.value,
      logDate: logForm.value.date,
      budget: logForm.value.budget ? Number(logForm.value.budget) : undefined,
      actualTime: actualTime
    })
    uni.showToast({ title: '记录成功', icon: 'success' })
    showLogModal.value = false
    await loadDetail()
  } catch (e: any) {
    console.error('记录失败', e)
    uni.showToast({ title: e?.message || '记录失败', icon: 'none' })
  } finally {
    logging.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F5F6FA;
  padding-bottom: 200rpx;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 60rpx;
}

.loading-text {
  font-size: 30rpx;
  color: #B2BEC3;
}

.error-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.error-text {
  font-size: 30rpx;
  color: #636E72;
  margin-bottom: 30rpx;
  text-align: center;
}

.retry-btn {
  padding: 16rpx 48rpx;
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
  border-radius: 16rpx;
  font-size: 30rpx;
  border: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 60rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 30rpx;
  color: #B2BEC3;
}

/* === 通用卡片 === */
.card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 30rpx;
  margin: 20rpx 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.section-label {
  font-size: 26rpx;
  color: #636E72;
  margin-bottom: 16rpx;
  display: block;
}

/* === 查看模式 === */
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #2D3436;
  flex: 1;
}

.status-tag {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
  margin-left: 16rpx;
}

.status-pending {
  background: rgba(108, 92, 231, 0.1);
  color: #6C5CE7;
}

.status-done {
  background: rgba(0, 184, 148, 0.1);
  color: #00B894;
}

.status-abandoned {
  background: rgba(225, 112, 85, 0.1);
  color: #E17055;
}

.anchor-card {
  background: rgba(108, 92, 231, 0.04);
  border-left: 6rpx solid #6C5CE7;
}

.anchor-content {
  font-size: 30rpx;
  color: #6C5CE7;
  line-height: 1.6;
}

.time-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: 20rpx 0;
}

.time-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.time-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #2D3436;
}

.time-value.actual {
  color: #E17055;
}

.time-unit {
  font-size: 24rpx;
  color: #B2BEC3;
  margin-top: 8rpx;
}

.time-divider {
  width: 2rpx;
  height: 60rpx;
  background: #DFE6E9;
}

.progress-bar {
  height: 12rpx;
  background: #F0F0F0;
  border-radius: 6rpx;
  overflow: hidden;
  margin-top: 20rpx;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6C5CE7, #A29BFE);
  border-radius: 6rpx;
  transition: width 0.3s;
}

.progress-fill.over {
  background: linear-gradient(90deg, #E17055, #FDCB6E);
}

.progress-text {
  font-size: 24rpx;
  color: #636E72;
  margin-top: 10rpx;
  display: block;
}

.desc-content {
  font-size: 28rpx;
  color: #2D3436;
  line-height: 1.6;
}

.principle-link {
  font-size: 28rpx;
  color: #6C5CE7;
}

.overdue-hint {
  font-size: 24rpx;
  color: #E17055;
  margin-top: 10rpx;
  display: block;
}

.action-area {
  margin: 40rpx 30rpx;
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}

.action-btn {
  flex: 1;
  min-width: 140rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  font-size: 26rpx;
  text-align: center;
  border: none;
}

.action-btn.primary {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
}

.action-btn.abandon {
  background: rgba(225, 112, 85, 0.1);
  color: #E17055;
}

.action-btn.restore {
  background: rgba(0, 184, 148, 0.1);
  color: #00B894;
}

.action-btn.edit {
  background: #F5F6FA;
  color: #636E72;
}

.action-btn.danger-btn {
  background: rgba(225, 112, 85, 0.1);
  color: #E17055;
}

.action-btn.log-btn {
  background: rgba(108, 92, 231, 0.08);
  color: #6C5CE7;
  width: 100%;
  flex: none;
  margin-bottom: 12rpx;
  font-weight: 500;
}

.action-btn.flow-btn {
  background: linear-gradient(135deg, #1A1A2E, #2D2D44);
  color: #FDCB6E;
  width: 100%;
  flex: none;
  margin-bottom: 12rpx;
  font-weight: 600;
  border: 1rpx solid rgba(253, 203, 110, 0.3);
}

/* === 编辑模式 === */
.edit-content {
  min-height: 100vh;
  background: #F5F6FA;
  display: flex;
  flex-direction: column;
}

.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background: #FFFFFF;
  border-bottom: 1rpx solid #F0F0F0;
}

.edit-header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #2D3436;
}

.edit-header-x {
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

.edit-header-x:active {
  background: #E8E8E8;
}

.edit-scroll {
  flex: 1;
  min-height: 0;
}

.edit-input {
  width: 100%;
  height: 80rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #2D3436;
  box-sizing: border-box;
}

.edit-textarea {
  width: 100%;
  height: 160rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: #2D3436;
  box-sizing: border-box;
  line-height: 1.5;
}

/* 日期选择器 */
.date-picker-value {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 80rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #2D3436;
}

.date-picker-value .placeholder {
  color: #B2BEC3;
}

.clear-date {
  color: #636E72;
  font-size: 26rpx;
  padding: 8rpx 16rpx;
}

/* 状态选项 */
.status-options {
  display: flex;
  gap: 16rpx;
}

.status-option {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: #F5F6FA;
  font-size: 26rpx;
  color: #636E72;
}

.status-option.active {
  background: rgba(108, 92, 231, 0.12);
  color: #6C5CE7;
  font-weight: 600;
}

/* 底部按钮 */
.edit-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  border-top: 1rpx solid #F0F0F0;
}

.footer-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  text-align: center;
  border: none;
}

.footer-btn.cancel {
  background: #F5F6FA;
  color: #636E72;
}

.footer-btn.confirm {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
  color: #FFFFFF;
}

/* === 原则选择器 === */
.principle-selector {
  margin-top: 10rpx;
}

.selected-principle {
  background: rgba(108, 92, 231, 0.06);
  border-radius: 12rpx;
  padding: 20rpx;
  border-left: 6rpx solid #6C5CE7;
  margin-bottom: 16rpx;
}

.selected-principle-text {
  font-size: 28rpx;
  color: #2D3436;
  line-height: 1.5;
  display: block;
  margin-bottom: 12rpx;
}

.clear-principle {
  font-size: 24rpx;
  color: #E17055;
  padding: 4rpx 0;
}

.principle-picker {
  height: 80rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  margin-bottom: 16rpx;
}

.picker-placeholder {
  font-size: 28rpx;
  color: #B2BEC3;
}

.principle-actions-row {
  display: flex;
  gap: 24rpx;
}

.principle-action-link {
  font-size: 24rpx;
  color: #6C5CE7;
  padding: 8rpx 0;
}

/* 查看模式原则卡片 */
.principle-card {
  background: rgba(108, 92, 231, 0.04);
  border-left: 6rpx solid #6C5CE7;
}

.principle-content-text {
  font-size: 28rpx;
  color: #2D3436;
  line-height: 1.6;
  display: block;
  margin-bottom: 12rpx;
}

.principle-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.mini-tag {
  padding: 4rpx 14rpx;
  background: rgba(108, 92, 231, 0.08);
  color: #6C5CE7;
  border-radius: 8rpx;
  font-size: 22rpx;
}

/* === 弹窗通用 === */
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

/* === 原则选择器弹窗 === */
.principle-modal {
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.principle-search {
  margin-bottom: 20rpx;
}

.principle-search-input {
  width: 100%;
  height: 76rpx;
  background: #F5F6FA;
  border-radius: 38rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.principle-list-modal {
  max-height: 50vh;
}

.principle-empty {
  padding: 60rpx 0;
  text-align: center;
}

.principle-empty-text {
  font-size: 28rpx;
  color: #B2BEC3;
}

.principle-option {
  padding: 24rpx;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
  background: #F5F6FA;
  transition: all 0.2s;
}

.principle-option.active {
  background: rgba(108, 92, 231, 0.12);
  border: 2rpx solid #6C5CE7;
}

.principle-option-text {
  font-size: 28rpx;
  color: #2D3436;
  line-height: 1.5;
  display: block;
  margin-bottom: 8rpx;
}

.principle-option-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

/* === 日志弹窗 === */
.log-task-name {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding: 16rpx 20rpx;
  background: rgba(108, 92, 231, 0.06);
  border-radius: 12rpx;
}

.log-task-label {
  font-size: 26rpx;
  color: #636E72;
  margin-right: 8rpx;
}

.log-task-title {
  font-size: 28rpx;
  color: #2D3436;
  font-weight: 600;
}

.existing-log-hint {
  background: rgba(253, 203, 110, 0.15);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
}

.hint-text {
  font-size: 24rpx;
  color: #E17055;
}

/* === 被引用列表（反向链接）=== */
.backlink-card {
  background: rgba(0, 184, 148, 0.04);
  border-left: 6rpx solid #00B894;
}

.backlink-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  background: rgba(0, 184, 148, 0.06);
  border-radius: 12rpx;
  margin-top: 12rpx;
}

.backlink-item:active {
  background: rgba(0, 184, 148, 0.12);
}

.backlink-icon {
  font-size: 28rpx;
  flex-shrink: 0;
}

.backlink-info {
  flex: 1;
  min-width: 0;
}

.backlink-source {
  font-size: 26rpx;
  color: #2D3436;
  font-weight: 600;
  display: block;
}

.backlink-hint {
  font-size: 20rpx;
  color: #B2BEC3;
  display: block;
  margin-top: 4rpx;
}

.backlink-arrow {
  font-size: 24rpx;
  color: #B2BEC3;
  flex-shrink: 0;
}
</style>
