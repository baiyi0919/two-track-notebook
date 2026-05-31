package com.twotrack.notebook.service.impl;

import com.twotrack.notebook.entity.ThreadPersona;
import com.twotrack.notebook.mapper.ThreadPersonaMapper;
import com.twotrack.notebook.service.ThreadPersonaService;
import com.twotrack.notebook.service.PersonaConfigService;
import com.twotrack.notebook.entity.PersonaConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThreadPersonaServiceImpl implements ThreadPersonaService {

    private final ThreadPersonaMapper threadPersonaMapper;
    private final PersonaConfigService personaConfigService;

    @Override
    public List<ThreadPersona> getActivePersonas(Long threadId) {
        return threadPersonaMapper.selectActiveByThreadId(threadId);
    }

    @Override
    public ThreadPersona addPersonaToThread(Long threadId, Long personaId) {
        // 检查 persona 是否存在且未全局删除
        PersonaConfig persona = personaConfigService.getById(personaId);
        if (persona == null || (persona.getIsDeleted() != null && persona.getIsDeleted() == 1)) {
            throw new RuntimeException("角色不存在或已被全局删除");
        }

        // 查询是否已有关联记录
        ThreadPersona existing = threadPersonaMapper.selectByThreadIdAndPersonaId(threadId, personaId);

        if (existing != null) {
            // 已存在：恢复显示（is_deleted = 0）
            threadPersonaMapper.showPersona(threadId, personaId);
            existing.setIsDeleted(0);
            return existing;
        } else {
            // 不存在：新建关联
            ThreadPersona tp = new ThreadPersona();
            tp.setThreadId(threadId);
            tp.setPersonaId(personaId);
            tp.setIsDeleted(0);
            tp.setSortOrder(0);
            threadPersonaMapper.insert(tp);
            return tp;
        }
    }

    @Override
    public void hidePersonaFromThread(Long threadId, Long personaId) {
        threadPersonaMapper.hidePersona(threadId, personaId);
    }

    @Override
    public void showPersonaInThread(Long threadId, Long personaId) {
        threadPersonaMapper.showPersona(threadId, personaId);
    }
}
