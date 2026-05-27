package com.twotrack.notebook.service;

import com.twotrack.notebook.entity.Message;

public interface AiProxyService {

    /**
     * 触发 AI 角色回复
     *
     * @param threadId  议题ID
     * @param personaId AI角色配置ID
     * @return 保存后的消息
     */
    Message chat(Long threadId, Long personaId);

    /**
     * 直接调用 AI API 生成内容（不保存为消息）
     *
     * @param personaId AI角色配置ID（用于获取 model/apiKey/apiUrl/systemPrompt/personality）
     * @param prompt     用户输入的 prompt（作为 user 消息）
     * @return AI 回复的文本内容
     */
    String generateContent(Long personaId, String prompt);
}
