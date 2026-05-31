<template>
  <view class="page">
    <!-- 分析框架更新提醒 -->
    <view v-if="showUpdateBanner && !loading" class="update-banner">
      <text class="banner-text">⚠️ {{ pendingUpdateCount }} 个角色的分析框架需要更新</text>
      <view class="banner-actions">
        <button class="banner-btn primary" :disabled="isUpdatingFramework" @tap="handleImmediateUpdate">
          {{ isUpdatingFramework ? '更新中..' : '立即更新' }}
        </button>
        <button class="banner-btn" @tap="handlePostpone">稍后</button>
      </view>
    </view>

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

      <!-- 角色栏 -->
      <view class="persona-bar">
        <scroll-view scroll-x class="persona-scroll">
          <view class="persona-list">
            <!-- 角色头像列表 -->
            <view
              v-for="p in personas"
              :key="p.id"
              class="persona-card"
              :class="{ active: currentPersonaId === p.id, 'ai-role': p.isAI || p.model || p.apiKey }"
              @tap="onPersonaCardTap(p)"
              @longpress="handlePersonaLongPress(p)"
            >
              <view class="avatar-wrap">
                <view class="avatar">{{ p.avatar || '👤' }}</view>
                <text v-if="p.isAI || p.model || p.apiKey" class="persona-ai-badge">AI</text>
              </view>
              <text class="persona-name">{{ p.name }}</text>
            </view>

            <!-- + 按钮 -->
            <view class="add-card" @tap="openPersonaModal()">
              <text class="add-icon">+</text>
            </view>

            <!-- 设置按钮 -->
            <view class="setting-card" @tap="openPersonaSetting()">
              <text class="setting-icon">⚙️</text>
            </view>
          </view>
        </scroll-view>
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
            <view class="msg-header">
              <view class="msg-avatar-wrapper" @tap="handleMsgAvatarTap(msg)" @longpress="handleMsgAvatarLongPress(msg)">
                <view class="msg-avatar">{{ getMsgAvatar(msg) }}</view>
                <text v-if="isAIMsg(msg)" class="ai-tag">AI</text>
              </view>
              <view class="role-badge" :style="{ background: getRoleColor(msg.roleName) }">
                <text class="role-name">{{ msg.roleName }}</text>
              </view>
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
            :disabled="isAutoConversation"
          />
          <!-- 自动对话中 -->
          <template v-if="isAutoConversation">
            <view class="auto-status">
              <text class="auto-text">自动对话中... ({{ currentAutoRound }}/{{ autoConversationRounds }}轮)</text>
            </view>
            <view class="stop-btn" @tap="requestStop">
              <text>强制停止</text>
            </view>
          </template>
          <!-- 正常模式 -->
          <template v-else>
            <view class="auto-btn" @tap="showRoundSetting">
              <text>自动对话</text>
            </view>
            <view class="send-btn" :class="{ active: inputText.trim() }" @tap="handleSend">
              <text>发送</text>
            </view>
          </template>
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

    <!-- 更新结果弹窗 -->
    <view v-if="showUpdateResultModal" class="modal-mask" @tap="showUpdateResultModal = false">
      <view class="update-result-content" @tap.stop>
        <view class="update-result-header">
          <text class="update-result-title">{{ updateResultTitle }}</text>
          <view class="update-result-x" @tap="showUpdateResultModal = false">
            <text>✕</text>
          </view>
        </view>
        <scroll-view scroll-y class="update-result-body">
          <text class="update-result-text">{{ updateResultContent }}</text>
        </scroll-view>
        <view class="update-result-footer">
          <button class="modal-btn confirm" @tap="showUpdateResultModal = false">知道了</button>
        </view>
      </view>
    </view>

    <!-- 轮次设置弹窗 -->
    <view v-if="showRoundSettingModal" class="modal-mask" @tap="showRoundSettingModal = false">
      <view class="round-modal" @tap.stop>
        <text class="modal-title">🤖 自动对话设置</text>
        <text class="modal-hint">设置自动对话轮次（1轮 = 所有AI角色各发言1次）</text>
        <view class="input-group">
          <text class="input-label">对话轮次</text>
          <input class="input-field" type="number" v-model="roundInput" placeholder="5" :maxlength="2" />
        </view>
        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showRoundSettingModal = false">取消</button>
          <button class="modal-btn confirm" @tap="startAutoConversation">开始对话</button>
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

    <!-- 角色配置弹窗 -->
    <view v-if="showPersonaModal" class="modal-mask" @tap="showPersonaModal = false">
      <view class="persona-modal" @tap.stop>
        <text class="modal-title">🎭 角色配置</text>

        <!-- 角色切换器（仅编辑模式显示） -->
        <view v-if="editingPersonaId > 0 && personaNameList.length > 0" class="persona-switcher">
          <text class="switcher-label">切换角色：</text>
          <select class="persona-select" :value="personaNameList.indexOf(currentEditingPersonaName)" @change="onPersonaSwitch">
            <option v-for="(name, idx) in personaNameList" :key="idx" :value="idx">{{ name }}</option>
          </select>
        </view>

        <!-- 表单 -->
        <view class="input-group">
          <text class="input-label">角色名称 *</text>
          <input class="input-field" v-model="personaForm.name" placeholder="给角色起个名字" />
        </view>

        <view class="input-group">
          <text class="input-label">头像（emoji）</text>
          <input class="input-field" v-model="personaForm.avatar" placeholder="👤" maxlength="4" />
        </view>

        <view class="input-group">
          <text class="input-label">性格设定</text>
          <textarea class="input-field textarea" v-model="personaForm.personality" placeholder="描述角色的性格、说话风格..." />
        </view>

        <view class="input-group">
          <text class="input-label">类型</text>
          <view class="type-switch">
            <view class="type-option" :class="{ active: !personaForm.isAI }" @tap="personaForm.isAI = false">🧑 人类</view>
            <view class="type-option" :class="{ active: personaForm.isAI }" @tap="personaForm.isAI = true">🤖 AI</view>
          </view>
        </view>

        <template v-if="personaForm.isAI">
          <view class="input-group">
            <text class="input-label">模型名称</text>
            <input class="input-field" v-model="personaForm.model" placeholder="gpt-3.5-turbo" />
          </view>
          <view class="input-group">
            <text class="input-label">API Key</text>
            <input class="input-field" v-model="personaForm.apiKey" placeholder="sk-..." type="password" />
          </view>
          <view class="input-group">
            <text class="input-label">API 地址（选填）</text>
            <input class="input-field" v-model="personaForm.apiUrl" placeholder="https://api.openai.com/v1" />
          </view>
        </template>

        <!-- 分析框架（仅编辑模式） -->
        <view v-if="editingPersonaId > 0" class="input-group framework-section">
          <view class="framework-header" @tap="toggleFramework">
            <text class="input-label">📋 分析框架</text>
            <text class="toggle-icon">{{ showFramework ? '▲' : '▼' }}</text>
          </view>
          <view v-if="showFramework" class="framework-body">
            <textarea
              v-if="frameworkContent"
              class="input-field textarea framework-text"
              :value="frameworkContent"
              disabled
              placeholder="暂无框架内容"
            />
            <text v-else class="framework-empty">暂无框架内容，点击"立即更新"生成</text>
            <view class="framework-actions">
              <button
                class="framework-btn"
                :disabled="frameworkLoading"
                @tap="handleUpdateFramework"
              >
                {{ frameworkLoading ? '更新中...' : '🔄 立即更新' }}
              </button>
            </view>
          </view>
        </view>

        <view class="modal-btns">
          <button class="modal-btn cancel" @tap="showPersonaModal = false">取消</button>
          <button class="modal-btn confirm" @tap="savePersona()">保存</button>
        </view>

        <!-- 删除按钮（仅编辑模式） -->
        <view v-if="editingPersonaId > 0" class="delete-area">
          <button class="modal-btn delete" @tap="handleDeletePersona()">🗑️ 删除此角色</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getThreadDetail, getMessageList, sendMessage, closeThread, updateThread, deleteThread, createPrinciple, createReference, deleteReference, getReferencesBySource, getBacklinksByTarget, getTaskList, getPrincipleList,
  getPersonaConfigList, createPersonaConfig, updatePersonaConfig, deletePersonaConfig,
  getThreadPersonas, addPersonaToThread, hidePersonaFromThread,
  aiChat, getFrameworkPendingCount, getFrameworkPendingList, ensureAllFrameworks, triggerFrameworkUpdate,
  getAnalysisFramework
} from '@/api'
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
const showUpdateBanner = ref(false)
const pendingUpdateCount = ref(0)
const isUpdatingFramework = ref(false)
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

// ============ 多角色相关 ============
const personas = ref<any[]>([])  // 当前议题激活的角色列表（从 thread_persona 表加载）
const allPersonas = ref<any[]>([]) // 所有全局角色（用于消息头像回退查找）
const currentPersonaId = ref<number>(0)  // 0 = 本地默认"我"
const showPersonaModal = ref(false)  // 角色配置弹窗
const editingPersonaId = ref<number>(0)  // 0=新建，>0=编辑
const personaForm = ref({
  name: '',
  avatar: '',
  personality: '',
  isAI: false,
  model: '',
  apiKey: '',
  apiUrl: ''
})
const currentEditingPersonaName = ref('')  // 当前编辑的角色名称

// ============ 分析框架查看/更新 ============
const frameworkContent = ref('')
const showFramework = ref(false)
const frameworkLoading = ref(false)

// ============ 更新结果弹窗 ============
const showUpdateResultModal = ref(false)
const updateResultTitle = ref('')
const updateResultContent = ref('')

// ============ 自动接力相关 ============
const isAutoConversation = ref(false)  // 是否正在自动对话
const autoConversationRounds = ref(5)  // 目标轮次数
const currentAutoRound = ref(0)      // 当前已完成轮次
const stopRequested = ref(false)       // 是否请求停止
const showRoundSettingModal = ref(false) // 轮次设置弹窗
const roundInput = ref(5)             // 输入的轮次数

function openUpdateResultModal(title: string, content: string) {
  updateResultTitle.value = title
  updateResultContent.value = content
  showUpdateResultModal.value = true
}

async function loadFramework(personaId: number) {
  frameworkLoading.value = true
  try {
    const res = await getAnalysisFramework(personaId)
    frameworkContent.value = res.data?.content || ''
  } catch (e: any) {
    console.error('加载框架失败', e)
    frameworkContent.value = ''
  } finally {
    frameworkLoading.value = false
  }
}

function toggleFramework() {
  showFramework.value = !showFramework.value
  if (showFramework.value && editingPersonaId.value > 0) {
    loadFramework(editingPersonaId.value)
  }
}

async function handleUpdateFramework() {
  if (editingPersonaId.value <= 0) return
  frameworkLoading.value = true
  try {
    await triggerFrameworkUpdate(editingPersonaId.value)
    await loadFramework(editingPersonaId.value)
    const personaName = currentEditingPersonaName.value || '角色'
    openUpdateResultModal(
      `✅ ${personaName} 的分析框架已更新`,
      frameworkContent.value || '（框架内容为空）'
    )
  } catch (e: any) {
    uni.showToast({ title: e.message || '更新失败', icon: 'none' })
  } finally {
    frameworkLoading.value = false
  }
}

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
    // 确保先加载 allPersonas，再加载消息（用于 roleName 回退查找）
    loadPersonas().then(() => {
      loadData()
    })
    checkPendingUpdates()
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
    // 补全 roleName（兼容角色在当前议题被隐藏的情况）
    messages.value.forEach((msg: any) => {
      if (!msg.roleName && msg.personaId) {
        const p = allPersonas.value.find((p: any) => p.id === msg.personaId)
        if (p) msg.roleName = p.name
      }
    })
    await nextTick()
    scrollToBottom()
  } catch (e: any) {
    console.error('加载数据失败', e)
    loadError.value = e?.message || '加载失败，请检查网络或权限'
  } finally {
    loading.value = false
  }
}

async function checkPendingUpdates() {
  try {
    await ensureAllFrameworks()
    const res: any = await getFrameworkPendingCount()
    pendingUpdateCount.value = res.data || 0
    showUpdateBanner.value = pendingUpdateCount.value > 0
  } catch (e) {
    console.error('检查分析框架更新失败', e)
  }
}

async function handleImmediateUpdate() {
  if (isUpdatingFramework.value) return
  isUpdatingFramework.value = true
  let successCount = 0
  let failCount = 0
  const failedIds: number[] = []
  try {
    const res: any = await getFrameworkPendingList()
    const list = res.data || []
    for (const item of list) {
      try {
        await triggerFrameworkUpdate(item.personaId)
        successCount++
      } catch (e: any) {
        failCount++
        failedIds.push(item.personaId)
        console.error('更新框架失败 personaId=' + item.personaId, e)
      }
    }
    await checkPendingUpdates()

    let content = `成功更新 ${successCount} 个角色的分析框架`
    if (failCount > 0) {
      content += `\n失败 ${failCount} 个（角色ID：${failedIds.join('、')}）`
    }
    openUpdateResultModal('✅ 批量更新完成', content)
  } catch (e) {
    console.error('立即更新失败', e)
    uni.showToast({ title: '更新失败', icon: 'none' })
  } finally {
    isUpdatingFramework.value = false
  }
}

function handlePostpone() {
  showUpdateBanner.value = false
}

function getRoleColor(roleName: string): string {
  const role = roles.value.find(r => r.name === roleName)
  return role ? role.color : '#636E72'
}

function selectRole(name: string) {
  currentRole.value = name
  showRolePicker.value = false
  // 同步更新 currentPersonaId：在 personas 中查找匹配的角色
  const matched = personas.value.find((p: any) => p.name === name)
  currentPersonaId.value = matched ? matched.id : 0
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
      content: inputText.value.trim(),
      personaId: currentPersonaId.value === 0 ? 0 : (currentPersonaId.value || undefined)
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

// ============ 多角色：核心函数 ============
// 判断消息是否由 AI 角色发送
function isAIMsg(msg: any): boolean {
  // 优先通过 personaId 判断
  if (msg.personaId && msg.personaId !== 0) {
    const p = personas.value.find((p: any) => p.id == msg.personaId)
    if (p) return !!(p.type === 'ai' || p.isAI || p.model || p.apiKey)
  }
  // 兜底：通过 roleName 在 personas 中查找（兼容旧数据）
  if (msg.roleName) {
    const p = personas.value.find((p: any) => p.name === msg.roleName)
    if (p) return !!(p.type === 'ai' || p.isAI || p.model || p.apiKey)
  }
  return false
}

// 加载当前议题的角色列表（从 thread_persona 表读取）
async function loadPersonas() {
  try {
    // 优先从议题关联表加载
    const res: any = await getThreadPersonas(threadId.value)
    const threadPersonas = res.data || []

    // 同时加载全局角色配置（用于获取详细信息，也用于消息头像回退）
    const allRes: any = await getPersonaConfigList()
    const globalList = allRes.data || []
    allPersonas.value = globalList

    // 将议题关联记录与全局角色配置合并
    const merged = threadPersonas.map((tp: any) => {
      const config = globalList.find((p: any) => p.id === tp.personaId)
      return {
        ...config,
        id: tp.personaId,
        threadPersonaId: tp.id,
        isDeletedInThread: tp.isDeleted,
        sortOrder: tp.sortOrder
      }
    })

    // 添加本地默认"我"
    const localMe = { id: 0, name: '我', avatar: '👤', type: 'human', isAI: false }
    personas.value = [localMe, ...merged]

    // 如果没有选中的角色，默认选中"我"
    if (currentPersonaId.value === 0 && personas.value.length > 0) {
      currentPersonaId.value = 0
    }
  } catch (e) {
    console.error('加载角色列表失败', e)
    // 降级：至少显示本地默认"我"
    personas.value = [{ id: 0, name: '我', avatar: '👤', type: 'human', isAI: false }]
  }
}

// 点击角色卡片：人类=切换发言角色，AI=触发发言
function onPersonaCardTap(p: any) {
  const isAI = !!(p.type === 'ai' || p.isAI || p.model || p.apiKey)
  if (isAI) {
    // AI 角色：触发发言
    triggerSingleAIReply(p)
  } else {
    // 人类角色：切换为当前发言角色
    selectPersona(p)
  }
}

// 选择角色（切换到当前发言角色）
function selectPersona(p: any) {
  currentPersonaId.value = p.id
  currentRole.value = p.name || '我'
  uni.showToast({ title: `已切换到 ${p.name || '我'}`, icon: 'none' })
}

// 打开角色配置弹窗（新建或编辑）
function openPersonaModal(personaId?: number) {
  if (personaId && personaId > 0) {
    const p = personas.value.find((p: any) => p.id === personaId)
    if (p) {
      editingPersonaId.value = personaId
      personaForm.value = {
        name: p.name || '',
        avatar: p.avatar || '',
        personality: p.personality || '',
        isAI: p.isAI || p.type === 'ai' || !!(p.model || p.apiKey),
        model: p.model || '',
        apiKey: p.apiKey || '',
        apiUrl: p.apiUrl || ''
      }
      currentEditingPersonaName.value = p.name
      frameworkContent.value = ''
      showFramework.value = false
    } else {
      loadPersonas().then(() => {
        const p2 = personas.value.find((p: any) => p.id === personaId)
        if (p2) {
          editingPersonaId.value = personaId
          personaForm.value = {
            name: p2.name || '',
            avatar: p2.avatar || '',
            personality: p2.personality || '',
            isAI: p2.isAI || p2.type === 'ai',
            model: p2.model || '',
            apiKey: p2.apiKey || '',
            apiUrl: p2.apiUrl || ''
          }
          currentEditingPersonaName.value = p2.name
          frameworkContent.value = ''
          showFramework.value = false
        }
      })
    }
  } else {
    editingPersonaId.value = 0
    personaForm.value = { name: '', avatar: '', personality: '', isAI: false, model: '', apiKey: '', apiUrl: '' }
    currentEditingPersonaName.value = ''
    frameworkContent.value = ''
    showFramework.value = false
  }
  showPersonaModal.value = true
}

// 打开角色设置（默认打开第一个 AI 角色）
function openPersonaSetting() {
  const aiList = personas.value.filter((p: any) => p.id > 0 && (p.type === 'ai' || p.isAI || p.model || p.apiKey))
  const target = aiList.length > 0 ? aiList[0] : (personas.value.length > 1 ? personas.value[1] : null)
  if (target) {
    openPersonaModal(target.id)
  } else {
    openPersonaModal()
  }
}

// 角色名称列表（用于配置弹窗中的角色切换器）
const personaNameList = computed(() => {
  return personas.value.filter((p: any) => p.id > 0).map((p: any) => p.name)
})

// 角色切换器改变时触发
function onPersonaSwitch(e: any) {
  const idx = Number(e.target.value)
  const list = personas.value.filter((p: any) => p.id > 0)
  const p = list[idx]
  if (p) {
    openPersonaModal(p.id)
  }
}

// 点击角色头像
function handlePersonaAvatarTap(p: any) {
  uni.vibrateShort({ type: 'light' })
  if (p.type === 'ai' || p.model || p.apiKey) {
    triggerSingleAIReply(p)
  } else {
    selectPersona(p)
  }
}

// 构建上下文（最近 10 条消息）
function buildContext(): string {
  const recent = messages.value.slice(-10)
  return recent.map((m: any) => `${m.roleName || '我'}: ${m.content}`).join('\n')
}

// 触发单个 AI 角色回复（模拟，待接入真实 AI API）
async function triggerSingleAIReply(p: any) {
  try {
    uni.showLoading({ title: '思考中...' })
    // 调用后端 AI 代理接口
    await aiChat(threadId.value, p.id)
    await loadData()
  } catch (e: any) {
    uni.showToast({ title: e?.message || 'AI 调用失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 长按角色头像（打开配置弹窗）
function handlePersonaLongPress(p: any) {
  uni.vibrateShort({ type: 'medium' })
  if (p.id === 0) {
    uni.showToast({ title: '默认角色不能编辑', icon: 'none' })
    return
  }
  openPersonaModal(p.id)
}

// 长按消息头像（打开对应角色的配置弹窗）
function handleMsgAvatarLongPress(msg: any) {
  uni.vibrateShort({ type: 'medium' })
  const pid = msg.personaId
  if (!pid || pid === 0) {
    uni.showToast({ title: '默认角色不能编辑', icon: 'none' })
    return
  }
  openPersonaModal(pid)
}

// 从议题中隐藏角色（仅本议题不可见，不影响其他议题）
async function handleDeletePersona() {
  if (editingPersonaId.value <= 0) return
  uni.showModal({
    title: '隐藏角色',
    content: `确定要在本议题中隐藏角色「${personaForm.value.name}」吗？\n（其他议题不受影响）`,
    confirmText: '隐藏',
    confirmColor: '#E17055',
    success: async (res) => {
      if (res.confirm) {
        try {
          await hidePersonaFromThread(threadId.value, editingPersonaId.value)
          uni.showToast({ title: '已隐藏（仅本议题）', icon: 'success' })
          showPersonaModal.value = false
          await loadPersonas()
          if (currentPersonaId.value === editingPersonaId.value) {
            currentPersonaId.value = 0
          }
        } catch (e: any) {
          uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
        }
      }
    }
  })
}

// 获取消息对应的角色头像（优先从当前议题角色中查找，找不到则回退到全局角色列表）
function getMsgAvatar(msg: any): string {
  if (!msg.personaId || msg.personaId === 0) return '👤'
  // 先在当前议题激活角色中查找
  let p = personas.value.find((p: any) => p.id === msg.personaId)
  // 找不到（角色已被隐藏），回退到全局角色列表
  if (!p) {
    p = allPersonas.value.find((p: any) => p.id === msg.personaId)
  }
  return p ? (p.avatar || '👤') : (msg.roleName || '👤')
}

// 点击消息头像（暂不特殊处理）
function handleMsgAvatarTap(msg: any) {
  // 可以后续扩展为跳转角色详情
}

// 保存角色配置
async function savePersona() {
  if (!personaForm.value.name.trim()) {
    uni.showToast({ title: '请输入角色名称', icon: 'none' })
    return
  }
  try {
    if (editingPersonaId.value > 0) {
      await updatePersonaConfig(editingPersonaId.value, personaForm.value)
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      const res: any = await createPersonaConfig(personaForm.value)
      uni.showToast({ title: '创建成功', icon: 'success' })
      // 新建成功后，自动添加到当前议题
      if (res && res.data && res.data.id) {
        try {
          await addPersonaToThread(threadId.value, res.data.id)
        } catch (e) {
          console.warn('自动添加到议题失败', e)
        }
      }
    }
    showPersonaModal.value = false
    await loadPersonas()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  }
}

// ============ 自动接力相关 ============

// 显示轮次设置弹窗
function showRoundSetting() {
  showRoundSettingModal.value = true
}

// 请求停止（等待当前回复完成后停止）
function requestStop() {
  stopRequested.value = true
  uni.showToast({ title: '将在当前回复完成后停止', icon: 'none' })
}

// 开始自动对话
async function startAutoConversation() {
  if (roundInput.value < 1) {
    uni.showToast({ title: '轮次至少为1', icon: 'none' })
    return
  }
  
  showRoundSettingModal.value = false
  isAutoConversation.value = true
  currentAutoRound.value = 0
  stopRequested.value = false
  autoConversationRounds.value = roundInput.value
  
  uni.showLoading({ title: '自动对话中...' })
  await runAutoConversationRound()
}

// 执行一轮自动对话（所有AI角色各发言1次）
async function runAutoConversationRound() {
  if (stopRequested.value) {
    finishAutoConversation()
    return
  }
  
  if (currentAutoRound.value >= autoConversationRounds.value) {
    finishAutoConversation()
    return
  }
  
  currentAutoRound.value++
    
  // 获取所有AI角色
  const aiPersonas = personas.value.filter((p: any) => 
    p.id > 0 && (p.type === 'ai' || p.isAI || p.model || p.apiKey)
  )
    
  if (aiPersonas.length === 0) {
    uni.showToast({ title: '没有AI角色，无法自动对话', icon: 'none' })
    finishAutoConversation()
    return
  }
    
  // 让每个AI角色依次发言
  for (const p of aiPersonas) {
    if (stopRequested.value) break
      
    try {
      await triggerSingleAIReplyForAuto(p)
    } catch (e: any) {
      console.error('AI回复失败', e)
    }
  }
    
  // 本轮完成，继续下一轮
  if (!stopRequested.value && currentAutoRound.value < autoConversationRounds.value) {
    await runAutoConversationRound()
  } else {
    finishAutoConversation()
  }
}

// 为自动对话触发单个AI回复
async function triggerSingleAIReplyForAuto(p: any) {
  try {
    await aiChat(threadId.value, p.id)
    await loadData()
  } catch (e: any) {
    console.error('AI回复失败', e)
  }
}

// 完成自动对话
function finishAutoConversation() {
  isAutoConversation.value = false
  uni.hideLoading()
    
  if (stopRequested.value) {
    uni.showToast({ title: `已停止（完成 ${currentAutoRound.value} 轮）`, icon: 'none' })
  } else {
    uni.showToast({ title: `自动对话完成（共 ${autoConversationRounds.value} 轮）`, icon: 'success' })
  }
}
</script>

<style scoped>
/* === 分析框架更新提醒横幅 === */
.update-banner {
  background: #FFF3E0;
  color: #333;
  padding: 20rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
  margin: 0 20rpx;
  margin-top: 12rpx;
  border-radius: 16rpx;
}

.banner-text {
  font-size: 28rpx;
  flex: 1;
}

.banner-actions {
  display: flex;
  gap: 16rpx;
  flex-shrink: 0;
}

.banner-btn {
  padding: 12rpx 32rpx;
  border-radius: 8rpx;
  font-size: 26rpx;
  border: none;
  background: #E0E0E0;
  color: #333;
}

.banner-btn.primary {
  background: #FF9800;
  color: #fff;
}

.banner-btn:disabled {
  opacity: 0.6;
}

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
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
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

.thread-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.action-btn:active {
  background: #E8E8E8;
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

/* === 角色栏 === */
.persona-bar {
  flex-shrink: 0;
  background: #FFFFFF;
  border-bottom: 1rpx solid #E8E8E8;
  padding: 16rpx 20rpx;
}

.persona-scroll {
  width: 100%;
  white-space: nowrap;
}

.persona-list {
  display: inline-flex;
  align-items: center;
  gap: 20rpx;
  padding: 8rpx 0;
}

.persona-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 16rpx;
  border-radius: 16rpx;
  transition: all 0.2s ease;
  cursor: pointer;
}

.persona-card.active {
  background: rgba(108, 92, 231, 0.08);
  border: 2rpx solid #6C5CE7;
}

.persona-card.ai-role .avatar {
  background: rgba(108, 92, 231, 0.12);
  border: 2rpx solid #6C5CE7;
}

.persona-card .avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: #F5F6FA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  transition: transform 0.1s ease;
}

.persona-card.active .avatar {
  box-shadow: 0 4rpx 12rpx rgba(108, 92, 231, 0.3);
}

.persona-name {
  font-size: 22rpx;
  color: #2D3436;
  max-width: 100rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.persona-card.active .persona-name {
  color: #6C5CE7;
  font-weight: 600;
}

.avatar-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.persona-ai-badge {
  position: absolute;
  bottom: -4rpx;
  right: -4rpx;
  font-size: 16rpx;
  color: #FFFFFF;
  background: #6C5CE7;
  padding: 2rpx 8rpx;
  border-radius: 8rpx;
  font-weight: 600;
}

.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx 16rpx;
  border-radius: 16rpx;
  border: 2rpx dashed #B2BEC3;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-card:active {
  transform: scale(0.9);
}

.add-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: #F5F6FA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  color: #B2BEC3;
}

.setting-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx 16rpx;
  border-radius: 16rpx;
  cursor: pointer;
  transition: all 0.2s ease;
}

.setting-card:active {
  transform: scale(0.9);
}

.setting-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(108, 92, 231, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.setting-card:active .setting-icon {
  background: rgba(108, 92, 231, 0.15);
}

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

.msg-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.message-item.self .msg-header {
  flex-direction: row-reverse;
}

.msg-avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  cursor: pointer;
}

.msg-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #F5F6FA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.msg-avatar-wrapper:active .msg-avatar {
  transform: scale(0.9);
}

.ai-tag {
  font-size: 16rpx;
  color: #6C5CE7;
  background: rgba(108, 92, 231, 0.1);
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
  font-weight: 600;
}

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
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
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
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

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

/* === 分析框架区域 === */
.framework-section { margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid #eee; }

.framework-header { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 0; }

.framework-header .input-label { font-size: 28rpx; font-weight: 600; color: #333; }

.toggle-icon { font-size: 24rpx; color: #999; margin-left: 12rpx; }

.framework-body { padding: 16rpx 0; }

.framework-text { width: 100%; min-height: 200rpx; padding: 16rpx; border: 1rpx solid #ddd; border-radius: 8rpx; font-size: 26rpx; line-height: 1.6; background: #f8f8f8; color: #333; box-sizing: border-box; }

.framework-empty { display: block; padding: 20rpx 0; font-size: 26rpx; color: #999; }

.framework-actions { margin-top: 16rpx; }

.framework-btn { padding: 12rpx 30rpx; background: #07c160; color: #fff; border: none; border-radius: 8rpx; font-size: 26rpx; }

.framework-btn:disabled { background: #ccc; }

/* === 更新结果弹窗 === */
.update-result-content {
  width: 85%;
  max-height: 70vh;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
}

.update-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.update-result-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #2D3436;
}

.update-result-x {
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

.update-result-x:active { background: #E8E8E8; }

.update-result-body {
  flex: 1;
  max-height: 50vh;
  margin-bottom: 24rpx;
}

.update-result-text {
  font-size: 28rpx;
  color: #2D3436;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.update-result-footer {
  display: flex;
  justify-content: center;
}

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

/* === 角色配置弹窗 === */
.persona-modal {
  width: 90%;
  max-height: 85vh;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 40rpx;
  overflow-y: auto;
}

.persona-switcher {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
  padding: 16rpx;
  background: #F5F6FA;
  border-radius: 12rpx;
}

.switcher-label {
  font-size: 26rpx;
  color: #636E72;
  flex-shrink: 0;
}

.persona-select {
  flex: 1;
  height: 60rpx;
  background: #FFFFFF;
  border-radius: 8rpx;
  border: 1rpx solid #E8E8E8;
  padding: 0 16rpx;
  font-size: 28rpx;
  color: #2D3436;
}

.type-switch {
  display: flex;
  gap: 16rpx;
}

.type-option {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #636E72;
  background: #F5F6FA;
  border: 2rpx solid transparent;
  cursor: pointer;
}

.type-option.active {
  background: rgba(108, 92, 231, 0.08);
  color: #6C5CE7;
  border-color: #6C5CE7;
}

.delete-area {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #F5F6FA;
}

.modal-btn.delete {
  width: 100%;
  background: rgba(225, 112, 85, 0.1);
  color: #E17055;
}

/* === 自动接力相关 === */
.auto-status {
  display: flex;
  align-items: center;
  padding: 0 16rpx;
  font-size: 24rpx;
  color: #6C5CE7;
}

.auto-text {
  flex: 1;
}

.stop-btn {
  padding: 10rpx 28rpx;
  background: #E17055;
  color: #FFFFFF;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.auto-btn {
  padding: 10rpx 28rpx;
  background: #FF9800;
  color: #FFFFFF;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.round-modal {
  width: 80%;
  max-width: 600rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 40rpx;
}
</style>
