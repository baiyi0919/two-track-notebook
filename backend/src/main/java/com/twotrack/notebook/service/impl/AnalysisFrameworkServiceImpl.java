package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.entity.AnalysisFramework;
import com.twotrack.notebook.entity.Message;
import com.twotrack.notebook.entity.PersonaConfig;
import com.twotrack.notebook.mapper.AnalysisFrameworkMapper;
import com.twotrack.notebook.mapper.MessageMapper;
import com.twotrack.notebook.mapper.PersonaConfigMapper;
import com.twotrack.notebook.service.AiProxyService;
import com.twotrack.notebook.service.AnalysisFrameworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisFrameworkServiceImpl implements AnalysisFrameworkService {

    private final AnalysisFrameworkMapper analysisFrameworkMapper;
    private final PersonaConfigMapper personaConfigMapper;
    private final AiProxyService aiProxyService;
    private final MessageMapper messageMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public AnalysisFramework saveOrUpdate(Long personaId, String content) {
        List<AnalysisFramework> existing = analysisFrameworkMapper.selectList(
                new LambdaQueryWrapper<AnalysisFramework>()
                        .eq(AnalysisFramework::getPersonaId, personaId)
        );

        AnalysisFramework framework;
        if (existing != null && !existing.isEmpty()) {
            framework = existing.get(0);
            framework.setContent(content);
            framework.setLastUpdated(LocalDateTime.now());
            analysisFrameworkMapper.updateById(framework);
        } else {
            framework = new AnalysisFramework();
            framework.setPersonaId(personaId);
            framework.setContent(content);
            framework.setLastUpdated(LocalDateTime.now());
            framework.setNextUpdateTime(nextDay2AM());
            analysisFrameworkMapper.insert(framework);
        }
        return framework;
    }

    @Override
    public AnalysisFramework getByPersonaId(Long personaId) {
        List<AnalysisFramework> list = analysisFrameworkMapper.selectList(
                new LambdaQueryWrapper<AnalysisFramework>()
                        .eq(AnalysisFramework::getPersonaId, personaId)
        );
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    @Override
    public void updateNextUpdateTime(Long personaId, LocalDateTime nextTime) {
        List<AnalysisFramework> list = analysisFrameworkMapper.selectList(
                new LambdaQueryWrapper<AnalysisFramework>()
                        .eq(AnalysisFramework::getPersonaId, personaId)
        );
        if (list != null && !list.isEmpty()) {
            AnalysisFramework framework = list.get(0);
            framework.setNextUpdateTime(nextTime);
            analysisFrameworkMapper.updateById(framework);
        }
    }

    @Override
    public long getPendingUpdateCount(Long userId) {
        return analysisFrameworkMapper.countPendingByUserId(userId, LocalDateTime.now());
    }

    @Override
    public AnalysisFramework triggerUpdateForPersona(Long personaId) {
        PersonaConfig persona = personaConfigMapper.selectById(personaId);
        if (persona == null || persona.getIsDeleted() == 1) {
            throw new RuntimeException("角色不存在或已删除");
        }
        if (!persona.getUserId().equals(currentUserId())) {
            throw new RuntimeException("无权操作该角色");
        }

        String prompt = buildFrameworkPrompt(persona);
        log.info("开始为角色 {} 生成分析框架", persona.getName());
        String frameworkContent = aiProxyService.generateContent(personaId, prompt);

        AnalysisFramework framework = saveOrUpdate(personaId, frameworkContent);
        log.info("角色 {} 分析框架更新完成", persona.getName());

        return framework;
    }

    @Override
    public int updateAllPendingFrameworks() {
        Long userId = currentUserId();
        List<AnalysisFramework> pendingList = analysisFrameworkMapper.selectPendingByUserId(userId, LocalDateTime.now());

        int successCount = 0;
        for (AnalysisFramework framework : pendingList) {
            try {
                triggerUpdateForPersona(framework.getPersonaId());
                successCount++;
            } catch (Exception e) {
                log.error("更新角色 {} 的分析框架失败: {}", framework.getPersonaId(), e.getMessage(), e);
            }
        }
        log.info("用户框架更新完成：成功 {}/总数 {}", successCount, pendingList.size());
        return successCount;
    }

    @Override
    public int updateAllPendingFrameworksForScheduler() {
        List<AnalysisFramework> pendingAll = analysisFrameworkMapper.selectAllPending(LocalDateTime.now());

        int successCount = 0;
        for (AnalysisFramework framework : pendingAll) {
            try {
                triggerUpdateForPersona(framework.getPersonaId());
                successCount++;
            } catch (Exception e) {
                log.error("调度更新角色 {} 的分析框架失败: {}", framework.getPersonaId(), e.getMessage(), e);
            }
        }
        log.info("调度更新完成：成功 {}/总数 {}", successCount, pendingAll.size());
        return successCount;
    }

    @Override
    public int ensureAllFrameworksExist() {
        Long userId = currentUserId();
        // 查询当前用户的所有未删除角色
        List<PersonaConfig> personas = personaConfigMapper.selectList(
                new LambdaQueryWrapper<PersonaConfig>()
                        .eq(PersonaConfig::getUserId, userId)
                        .eq(PersonaConfig::getIsDeleted, 0)
        );
        
        int createdCount = 0;
        for (PersonaConfig p : personas) {
            if (p.getApiKey() != null && !p.getApiKey().isBlank()) {
                // 检查是否已有框架记录
                List<AnalysisFramework> existing = analysisFrameworkMapper.selectList(
                        new LambdaQueryWrapper<AnalysisFramework>()
                                .eq(AnalysisFramework::getPersonaId, p.getId())
                );
                if (existing == null || existing.isEmpty()) {
                    AnalysisFramework framework = new AnalysisFramework();
                    framework.setPersonaId(p.getId());
                    framework.setContent("");
                    framework.setLastUpdated(null);
                    framework.setNextUpdateTime(LocalDateTime.now());
                    analysisFrameworkMapper.insert(framework);
                    createdCount++;
                    log.info("为角色 {} 批量创建初始分析框架记录", p.getId());
                }
            }
        }
        log.info("批量补全框架记录完成：新建 {} 条", createdCount);
        return createdCount;
    }

    @Override
    public List<AnalysisFramework> getPendingUpdateList(Long userId) {
        return analysisFrameworkMapper.selectPendingByUserId(userId, LocalDateTime.now());
    }

    @Override
    public void ensureFrameworkExists(Long personaId) {
        List<AnalysisFramework> existing = analysisFrameworkMapper.selectList(
                new LambdaQueryWrapper<AnalysisFramework>()
                        .eq(AnalysisFramework::getPersonaId, personaId)
        );
        if (existing == null || existing.isEmpty()) {
            AnalysisFramework framework = new AnalysisFramework();
            framework.setPersonaId(personaId);
            framework.setContent(""); // 尚未生成，先设为空字符串避免 NOT NULL 报错
            framework.setLastUpdated(null);
            framework.setNextUpdateTime(LocalDateTime.now()); // 立即待更新
            analysisFrameworkMapper.insert(framework);
            log.info("为角色 {} 创建初始分析框架记录", personaId);
        }
    }

    private String buildFrameworkPrompt(PersonaConfig persona) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是角色【").append(persona.getName()).append("】。\n\n");
        sb.append("请根据你的设定，生成你的分析框架（思维模型）。\n");
        sb.append("分析框架是你思考问题时的固定思维路径和分析模式，它应该包括：\n");
        sb.append("1. 你通常从哪个角度切入问题？\n");
        sb.append("2. 你的核心思维模型是什么？（如：第一性原理、逆向思维、系统思维、类比思维等）\n");
        sb.append("3. 你在分析一个问题时，会优先考虑哪些因素？\n");
        sb.append("4. 你的决策框架是什么？（如何权衡利弊、做决定）\n");
        sb.append("5. 你有哪些典型的思维偏差或思维习惯？\n\n");
        sb.append("请用 markdown 格式输出，内容要具体、有操作性，不要泛泛而谈。\n");
        sb.append("直接输出框架内容，不要加任何解释或开场白。");

        if (persona.getPersonality() != null && !persona.getPersonality().isBlank()) {
            sb.append("\n\n你的性格特点：").append(persona.getPersonality());
        }
        return sb.toString();
    }

    @Override
    public void updateFrameworkFromConversation(Long personaId) {
        PersonaConfig persona = personaConfigMapper.selectById(personaId);
        if (persona == null || persona.getIsDeleted() == 1) {
            log.warn("角色 {} 不存在或已删除，跳过对话后框架更新", personaId);
            return;
        }
        if (persona.getApiKey() == null || persona.getApiKey().isBlank()) {
            log.warn("角色 {} 未配置 API Key，跳过对话后框架更新", persona.getName());
            return;
        }

        // 获取线程ID（当前逻辑：使用用户ID作为线程ID）
        Long threadId = persona.getUserId();
        
        // 统计该线程的消息数量
        Long messageCount = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getThreadId, threadId)
        );
        
        // 每5轮对话更新一次（5, 10, 15...）
        if (messageCount < 5) {
            log.debug("角色 {} 对话轮次 {} 不足5轮，暂不更新", persona.getName(), messageCount);
            return;
        }
        if (messageCount % 5 != 0) {
            log.debug("角色 {} 对话轮次 {} 不满足更新条件（每5轮更新一次）", persona.getName(), messageCount);
            return;
        }
        
        // 获取当前框架内容
        AnalysisFramework existing = getByPersonaId(personaId);
        String existingContent = (existing != null && existing.getContent() != null) ? existing.getContent() : "";
        
        // 获取最近10条对话消息
        List<Message> recentMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getThreadId, threadId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT 10")
        );
        Collections.reverse(recentMessages);
        
        String prompt = buildConversationFrameworkPrompt(persona, existingContent, recentMessages);
        log.info("对话后增量更新角色 {} 的分析框架（第 {} 轮）", persona.getName(), messageCount);
        
        try {
            String newContent = aiProxyService.generateContent(personaId, prompt);
            saveOrUpdate(personaId, newContent);
            log.info("角色 {} 对话后框架更新完成（第 {} 轮）", persona.getName(), messageCount);
        } catch (Exception e) {
            log.error("角色 {} 对话后框架更新失败: {}", persona.getName(), e.getMessage());
        }
    }

    private String buildConversationFrameworkPrompt(PersonaConfig persona, String existingFramework, List<Message> recentMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是角色【").append(persona.getName()).append("】。\n\n");
        sb.append("请根据以下最近对话内容，增量更新你的分析框架（思维模型）。\n\n");

        if (existingFramework != null && !existingFramework.isBlank()) {
            sb.append("【现有分析框架】\n");
            sb.append(existingFramework).append("\n\n");
        }

        sb.append("【最近对话记录】\n");
        for (Message msg : recentMessages) {
            String role = msg.getPersonaId() != null && msg.getPersonaId().equals(persona.getId()) ? "我(" + persona.getName() + ")" : "对方";
            sb.append(role).append("：").append(msg.getContent()).append("\n");
        }
        sb.append("\n");

        sb.append("请根据以上对话，增量更新你的分析框架。要求：\n");
        sb.append("1. 保留原有框架中仍然有效的部分\n");
        sb.append("2. 根据新对话补充或修正你的思维模式和决策偏好\n");
        sb.append("3. 如果对话中没有新的洞察，可以保持原框架不变\n");
        sb.append("4. 用 markdown 格式输出完整更新后的框架\n");
        sb.append("直接输出框架内容，不要加任何解释或开场白。");
        return sb.toString();
    }

    private LocalDateTime nextDay2AM() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow2AM = now.plusDays(1).with(LocalTime.of(2, 0));
        return tomorrow2AM;
    }
}
