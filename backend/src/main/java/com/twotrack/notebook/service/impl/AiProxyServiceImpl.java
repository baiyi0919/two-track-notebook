package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.dto.MessageCreateDto;
import com.twotrack.notebook.entity.Message;
import com.twotrack.notebook.entity.PersonaConfig;
import com.twotrack.notebook.entity.Thread;
import com.twotrack.notebook.mapper.MessageMapper;
import com.twotrack.notebook.mapper.PersonaConfigMapper;
import com.twotrack.notebook.mapper.ThreadMapper;
import com.twotrack.notebook.service.AiProxyService;
import com.twotrack.notebook.service.MessageService;
import com.twotrack.notebook.service.AnalysisFrameworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProxyServiceImpl implements AiProxyService {

    private final PersonaConfigMapper personaConfigMapper;
    private final MessageMapper messageMapper;
    private final ThreadMapper threadMapper;
    private final MessageService messageService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ApplicationContext applicationContext;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public Message chat(Long threadId, Long personaId) {
        // 1. 校验议题和角色归属
        Thread thread = threadMapper.selectById(threadId);
        if (thread == null || !thread.getUserId().equals(currentUserId())) {
            throw new RuntimeException("议题不存在或无权访问");
        }
        if (thread.getStatus() == 1) {
            throw new RuntimeException("议题已关闭");
        }

        PersonaConfig persona = personaConfigMapper.selectById(personaId);
        if (persona == null || persona.getIsDeleted() == 1) {
            throw new RuntimeException("AI角色配置不存在");
        }
        if (!persona.getUserId().equals(currentUserId())) {
            throw new RuntimeException("无权访问该角色配置");
        }
        if (persona.getApiKey() == null || persona.getApiKey().isBlank()) {
            throw new RuntimeException("该AI角色未配置 API Key，请在角色设置中添加");
        }
        if (persona.getModel() == null || persona.getModel().isBlank()) {
            throw new RuntimeException("该AI角色未配置模型名称，请在角色设置中添加");
        }

        // 2. 获取最近消息（最多10条）
        List<Message> history = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getThreadId, threadId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT 10")
        );
        Collections.reverse(history); // 按时间正序

        // 3. 构造 OpenAI 兼容格式的请求
        String apiUrl = resolveApiUrl(persona.getApiUrl());
        Map<String, Object> requestBody = buildRequestBody(persona, history, thread);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(persona.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 4. 调用 AI API
        log.info("调用 AI API: persona={}, model={}, url={}", persona.getName(), persona.getModel(), apiUrl);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.error("AI API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage());
        }

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("AI 服务返回异常，状态码: " + response.getStatusCode());
        }

        // 5. 解析回复内容
        String replyContent = extractContent(response.getBody());
        if (replyContent == null || replyContent.isBlank()) {
            throw new RuntimeException("AI 未返回有效内容");
        }

        // 6. 保存为消息
        MessageCreateDto dto = new MessageCreateDto();
        dto.setRoleName(persona.getName());
        dto.setContent(replyContent);
        dto.setPersonaId(personaId);

        Message savedMessage = messageService.send(threadId, dto);

        // 7. 异步触发对话后框架更新（不阻塞回复）
        final Long triggerPersonaId = personaId;
        CompletableFuture.runAsync(() -> {
            try {
                AnalysisFrameworkService afService = applicationContext.getBean(AnalysisFrameworkService.class);
                afService.updateFrameworkFromConversation(triggerPersonaId);
            } catch (Exception e) {
                log.warn("对话后框架更新失败（非阻塞）：{}", e.getMessage());
            }
        });

        return savedMessage;
    }

    /** 补全 API URL：如果用户只填了 base URL，自动补全 /v1/chat/completions */
    private String resolveApiUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            return "https://api.openai.com/v1/chat/completions";
        }
        String url = apiUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.contains("/chat/completions")) {
            url = url + "/v1/chat/completions";
        }
        return url;
    }

    /** 构建请求体 */
    private Map<String, Object> buildRequestBody(PersonaConfig persona, List<Message> history, Thread thread) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System 消息：角色设定 + 议题背景
        StringBuilder systemPrompt = new StringBuilder();
        if (persona.getSystemPrompt() != null && !persona.getSystemPrompt().isBlank()) {
            systemPrompt.append(persona.getSystemPrompt());
        }
        if (persona.getPersonality() != null && !persona.getPersonality().isBlank()) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("你的性格特点：").append(persona.getPersonality());
        }
        if (thread.getTopic() != null && !thread.getTopic().isBlank()) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("当前讨论议题：").append(thread.getTopic());
        }
        systemPrompt.append("\n\n请用中文回复，保持角色一致性，简短有力（100字以内）。不要总结全部内容，只针对最新话题发表你的看法。");

        messages.add(Map.of("role", "system", "content", systemPrompt.toString()));

        // 历史消息
        for (Message msg : history) {
            String role = "user";
            // 如果这条消息是该 AI 角色自己发的，标记为 assistant
            if (msg.getPersonaId() != null && msg.getPersonaId().equals(persona.getId())) {
                role = "assistant";
            }
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", persona.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 512);
        body.put("stream", false);

        return body;
    }

    @Override
    public String generateContent(Long personaId, String prompt) {
        // 1. 校验角色归属和配置
        PersonaConfig persona = personaConfigMapper.selectById(personaId);
        if (persona == null || persona.getIsDeleted() == 1) {
            throw new RuntimeException("AI角色配置不存在");
        }
        if (!persona.getUserId().equals(currentUserId())) {
            throw new RuntimeException("无权访问该角色配置");
        }
        if (persona.getApiKey() == null || persona.getApiKey().isBlank()) {
            throw new RuntimeException("该AI角色未配置 API Key，请在角色设置中添加");
        }
        if (persona.getModel() == null || persona.getModel().isBlank()) {
            throw new RuntimeException("该AI角色未配置模型名称，请在角色设置中添加");
        }

        // 2. 构造请求体（system prompt + 用户prompt）
        String apiUrl = resolveApiUrl(persona.getApiUrl());
        Map<String, Object> requestBody = buildGenerateRequestBody(persona, prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(persona.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 3. 调用 AI API
        log.info("调用 AI API(generate): persona={}, model={}, url={}", persona.getName(), persona.getModel(), apiUrl);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.error("AI API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage());
        }

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("AI 服务返回异常，状态码: " + response.getStatusCode());
        }

        // 4. 解析并返回内容
        String content = extractContent(response.getBody());
        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI 未返回有效内容");
        }
        return content;
    }

    /** 构造 generateContent 的请求体（无历史消息，只有 system prompt + 用户prompt） */
    private Map<String, Object> buildGenerateRequestBody(PersonaConfig persona, String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System 消息：角色设定
        StringBuilder systemPrompt = new StringBuilder();
        if (persona.getSystemPrompt() != null && !persona.getSystemPrompt().isBlank()) {
            systemPrompt.append(persona.getSystemPrompt());
        }
        if (persona.getPersonality() != null && !persona.getPersonality().isBlank()) {
            if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
            systemPrompt.append("你的性格特点：").append(persona.getPersonality());
        }
        systemPrompt.append("\n\n请用中文回复，保持角色一致性，内容详实有条理。");

        messages.add(Map.of("role", "system", "content", systemPrompt.toString()));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", persona.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 2048);
        body.put("stream", false);

        return body;
    }

    /** 从 OpenAI 格式响应中提取内容 */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> first = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            if (message == null) return null;
            Object content = message.get("content");
            return content != null ? content.toString() : null;
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", responseBody, e);
            return null;
        }
    }
}
