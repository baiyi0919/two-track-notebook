package com.twotrack.notebook.service;

import com.twotrack.notebook.entity.AnalysisFramework;

public interface AnalysisFrameworkService {

    /** 创建或更新分析框架 */
    AnalysisFramework saveOrUpdate(Long personaId, String content);

    /** 根据角色ID查询分析框架 */
    AnalysisFramework getByPersonaId(Long personaId);

    /** 更新下次更新时间 */
    void updateNextUpdateTime(Long personaId, java.time.LocalDateTime nextTime);
}
