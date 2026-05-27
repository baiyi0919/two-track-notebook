import { http } from '@/utils/request'

// ============ 认证 ============
export function login(username: string, password: string) {
  return http.post<string>('/auth/login', { username, password })
}

export function register(username: string, password: string, nickname?: string) {
  return http.post<string>('/auth/register', { username, password, nickname })
}

// ============ 用户个人信息 ============
export function getUserInfo() {
  return http.get<any>('/user/info')
}

export function updateUser(data: { nickname?: string; avatarUrl?: string }) {
  return http.put<any>('/user/info', data)
}

// ============ 任务（现实轨） ============
export function getTaskList(status?: number, sortBy?: string, sortOrder?: string) {
  const params: Record<string, any> = {}
  if (status !== undefined) params.status = status
  if (sortBy) params.sortBy = sortBy
  if (sortOrder) params.sortOrder = sortOrder
  return http.get<any[]>('/tasks', params)
}

export function getTaskDetail(id: number) {
  return http.get<any>(`/tasks/${id}`)
}

export function createTask(data: { title: string; anchorText?: string; budget?: number; description?: string }) {
  return http.post<any>('/tasks', data)
}

export function updateTask(id: number, data: any) {
  return http.put<any>(`/tasks/${id}`, data)
}

export function deleteTask(id: number) {
  return http.delete(`/tasks/${id}`)
}

export function toggleTaskStatus(id: number, status: number) {
  return http.patch<any>(`/tasks/${id}/status?status=${status}`)
}

// ============ 议题（探索轨） ============
export function getThreadList(status?: number) {
  return http.get<any[]>('/threads', status !== undefined ? { status } : {})
}

export function getThreadDetail(id: number) {
  return http.get<any>(`/threads/${id}`)
}

export function createThread(data: { topic: string; description?: string }) {
  return http.post<any>('/threads', data)
}

export function updateThread(id: number, data: { topic: string; description?: string }) {
  return http.put<any>(`/threads/${id}`, data)
}

export function deleteThread(id: number) {
  return http.delete(`/threads/${id}`)
}

export function closeThread(id: number) {
  return http.patch<any>(`/threads/${id}/close`)
}

// ============ 沙盒消息 ============
export function sendMessage(threadId: number, data: { roleName: string; content: string }) {
  return http.post<any>(`/threads/${threadId}/messages`, data)
}

export function getMessageList(threadId: number) {
  return http.get<any[]>(`/threads/${threadId}/messages`)
}

// ============ 灵感快寄 ============
export function quickInspiration(content: string) {
  return http.post<any>('/threads/inspiration', { content })
}

// ============ 原则 ============
export function getPrincipleList(keyword?: string) {
  return http.get<any[]>('/principles', keyword ? { keyword } : {})
}

export function getPrincipleDetail(id: number) {
  return http.get<any>(`/principles/${id}`)
}

export function createPrinciple(data: { content: string; sourceThreadId?: number; tags?: string }) {
  return http.post<any>('/principles', data)
}

export function updatePrinciple(id: number, data: { content?: string; tags?: string }) {
  return http.put<any>(`/principles/${id}`, data)
}

export function deletePrinciple(id: number) {
  return http.delete(`/principles/${id}`)
}

// ============ 原则反向关联 ============
export function getPrincipleRelatedTasks(id: number) {
  return http.get<any[]>(`/principles/${id}/related-tasks`)
}

// ============ 注意力报告 ============
export function getAttentionReport(date?: string) {
  return http.get<any>('/reports/attention', date ? { date } : {})
}

export function getAttentionTrend(days?: number) {
  return http.get<any>('/reports/attention/trend', days ? { days } : {})
}

// ============ 注意力日志 ============
export function createAttentionLog(data: { taskId: number; logDate?: string; budget?: number; actualTime?: number }) {
  return http.post<any>('/attention-logs', data)
}

export function getAttentionLogsByDate(date: string) {
  return http.get<any[]>('/attention-logs/by-date', { date })
}

export function getAttentionLogsByTask(taskId: number) {
  return http.get<any[]>(`/attention-logs/by-task/${taskId}`)
}

// ============ 知识引用 ============
export function createReference(data: { sourceType: string; sourceId: number; targetType: string; targetId: number }) {
  return http.post<any>('/references', data)
}

export function deleteReference(id: number) {
  return http.delete(`/references/${id}`)
}

export function getReferencesBySource(sourceType: string, sourceId: number) {
  return http.get<any[]>('/references/source', { sourceType, sourceId })
}

export function getBacklinksByTarget(targetType: string, targetId: number) {
  return http.get<any[]>('/references/target', { targetType, targetId })
}

// ============ 角色配置（多人模式） ============
export function getPersonaConfigList() {
  return http.get<any[]>('/persona-configs/mine')
}

export function createPersonaConfig(data: { name: string; avatar?: string; type?: string; personality?: string; model?: string; temperature?: string; apiKey?: string; apiUrl?: string }) {
  return http.post<any>('/persona-configs', data)
}

export function updatePersonaConfig(id: number, data: { name?: string; avatar?: string; type?: string; personality?: string; model?: string; temperature?: string; apiKey?: string; apiUrl?: string }) {
  return http.put<any>(`/persona-configs/${id}`, data)
}

export function deletePersonaConfig(id: number) {
  return http.delete(`/persona-configs/${id}`)
}

// ============ AI 代理（多人模式） ============
export function aiChat(threadId: number, personaId: number) {
  return http.post<any>('/ai/chat', { threadId, personaId })
}

// ============ 分析框架（多人模式） ============
export function getAnalysisFramework(personaId: number) {
  return http.get<any>(`/analysis-frameworks?personaId=${personaId}`)
}

export function saveOrUpdateAnalysisFramework(personaId: number, content: string) {
  return http.post<any>('/analysis-frameworks', { personaId, content })
}

export function updateAnalysisFrameworkNextUpdateTime(personaId: number, nextTime: string) {
  return http.put<any>(`/analysis-frameworks/next-update-time?personaId=${personaId}&nextTime=${encodeURIComponent(nextTime)}`)
}

/** 获取当前用户待更新框架数量 */
export function getFrameworkPendingCount() {
  return http.get<number>('/analysis-frameworks/pending-count')
}

/** 立即触发某个角色的框架更新 */
export function triggerFrameworkUpdate(personaId: number) {
  return http.post<any>(`/analysis-frameworks/trigger-update?personaId=${personaId}`)
}

/** 获取当前用户待更新的框架列表 */
export function getFrameworkPendingList() {
  return http.get<any[]>('/analysis-frameworks/pending-list')
}

/** 批量补全当前用户所有AI角色的框架记录（修复历史数据） */
export function ensureAllFrameworks() {
  return http.post<number>('/analysis-frameworks/ensure-all')
}
