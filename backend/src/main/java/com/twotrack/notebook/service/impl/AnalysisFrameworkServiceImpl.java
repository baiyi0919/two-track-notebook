package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.entity.AnalysisFramework;
import com.twotrack.notebook.mapper.AnalysisFrameworkMapper;
import com.twotrack.notebook.service.AnalysisFrameworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisFrameworkServiceImpl implements AnalysisFrameworkService {

    private final AnalysisFrameworkMapper analysisFrameworkMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public AnalysisFramework saveOrUpdate(Long personaId, String content) {
        // 校验角色属于当前用户
        // 注意：这里需要注入 PersonaConfigService 或 Mapper 来校验归属
        // 为简化，先直接保存/更新

        List<AnalysisFramework> existing = analysisFrameworkMapper.selectList(
                new LambdaQueryWrapper<AnalysisFramework>()
                        .eq(AnalysisFramework::getPersonaId, personaId)
        );

        AnalysisFramework framework;
        if (existing != null && !existing.isEmpty()) {
            // 更新
            framework = existing.get(0);
            framework.setContent(content);
            framework.setLastUpdated(LocalDateTime.now());
            analysisFrameworkMapper.updateById(framework);
        } else {
            // 创建
            framework = new AnalysisFramework();
            framework.setPersonaId(personaId);
            framework.setContent(content);
            framework.setLastUpdated(LocalDateTime.now());
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
}
