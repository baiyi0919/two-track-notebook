package com.twotrack.notebook.service;

import com.twotrack.notebook.entity.AnalysisFramework;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisFrameworkService {

    /** 创建或更新分析框架 */
    AnalysisFramework saveOrUpdate(Long personaId, String content);

    /** 根据角色ID查询分析框架 */
    AnalysisFramework getByPersonaId(Long personaId);

    /** 更新下次更新时间 */
    void updateNextUpdateTime(Long personaId, LocalDateTime nextTime);

    /**
     * 查询当前用户待更新的框架数量
     * （next_update_time <= 当前时间，且角色属于当前用户）
     */
    long getPendingUpdateCount(Long userId);

    /**
     * 立即触发某个角色的框架更新（调用 AI 生成）
     */
    AnalysisFramework triggerUpdateForPersona(Long personaId);

    /**
     * 批量更新所有到期的框架（供定时任务调用，不限制用户）
     * @return 更新成功的框架数量
     */
    int updateAllPendingFrameworksForScheduler();

    /**
     * 批量更新当前用户所有到期的框架
     * @return 更新成功的框架数量
     */
    int updateAllPendingFrameworks();

    /**
     * 查询当前用户待更新的框架列表
     */
    List<AnalysisFramework> getPendingUpdateList(Long userId);

    /**
     * 确保角色有分析框架记录（不存在则创建，next_update_time=NOW()立即待更新）
     */
    void ensureFrameworkExists(Long personaId);

    /**
     * 为当前用户的所有AI角色（有apiKey的）批量创建缺失的分析框架记录
     * @return 新创建的记录数量
     */
    int ensureAllFrameworksExist();

    /**
     * AI 对话后自动根据最近对话增量更新分析框架
     * 由 AiProxyService 在每次 AI 回复后异步调用
     */
    void updateFrameworkFromConversation(Long personaId);
}
