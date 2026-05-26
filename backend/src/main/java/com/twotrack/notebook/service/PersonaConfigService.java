package com.twotrack.notebook.service;

import com.twotrack.notebook.entity.PersonaConfig;

import java.util.List;

public interface PersonaConfigService {

    /** 创建角色配置 */
    PersonaConfig create(PersonaConfig persona);

    /** 更新角色配置 */
    PersonaConfig update(Long id, PersonaConfig persona);

    /** 删除角色配置（软删除）*/
    void delete(Long id);

    /** 查询当前用户的角色列表 */
    List<PersonaConfig> listMine();

    /** 根据ID查询 */
    PersonaConfig getById(Long id);
}
