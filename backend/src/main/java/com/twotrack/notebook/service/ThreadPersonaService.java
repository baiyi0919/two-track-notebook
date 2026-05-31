package com.twotrack.notebook.service;

import com.twotrack.notebook.entity.ThreadPersona;
import java.util.List;

public interface ThreadPersonaService {

    /**
     * 获取某议题中激活的角色列表（用于前端显示）
     */
    List<ThreadPersona> getActivePersonas(Long threadId);

    /**
     * 为议题添加角色（如已存在则恢复显示）
     */
    ThreadPersona addPersonaToThread(Long threadId, Long personaId);

    /**
     * 从议题中隐藏角色（软删除，不影响其他议题）
     */
    void hidePersonaFromThread(Long threadId, Long personaId);

    /**
     * 恢复议题中已隐藏的角色
     */
    void showPersonaInThread(Long threadId, Long personaId);
}
