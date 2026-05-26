package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.entity.PersonaConfig;
import com.twotrack.notebook.mapper.PersonaConfigMapper;
import com.twotrack.notebook.service.PersonaConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaConfigServiceImpl implements PersonaConfigService {

    private final PersonaConfigMapper personaConfigMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public PersonaConfig create(PersonaConfig persona) {
        persona.setUserId(currentUserId());
        persona.setIsDeleted(0);
        personaConfigMapper.insert(persona);
        return persona;
    }

    @Override
    public PersonaConfig update(Long id, PersonaConfig persona) {
        PersonaConfig existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("角色配置不存在");
        }
        persona.setId(id);
        persona.setUserId(currentUserId());
        personaConfigMapper.updateById(persona);
        return persona;
    }

    @Override
    public void delete(Long id) {
        PersonaConfig existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("角色配置不存在");
        }
        // 软删除
        existing.setIsDeleted(1);
        personaConfigMapper.updateById(existing);
    }

    @Override
    public List<PersonaConfig> listMine() {
        return personaConfigMapper.selectList(
                new LambdaQueryWrapper<PersonaConfig>()
                        .eq(PersonaConfig::getUserId, currentUserId())
                        .eq(PersonaConfig::getIsDeleted, 0)
                        .orderByAsc(PersonaConfig::getId)
        );
    }

    @Override
    public PersonaConfig getById(Long id) {
        PersonaConfig persona = personaConfigMapper.selectById(id);
        if (persona == null || persona.getIsDeleted() == 1) {
            return null;
        }
        // 校验归属
        if (!persona.getUserId().equals(currentUserId())) {
            throw new RuntimeException("无权访问该角色配置");
        }
        return persona;
    }
}
