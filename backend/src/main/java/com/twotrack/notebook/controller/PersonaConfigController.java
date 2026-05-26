package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.entity.PersonaConfig;
import com.twotrack.notebook.service.PersonaConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/persona-configs")
@SaCheckLogin
@RequiredArgsConstructor
public class PersonaConfigController {

    private final PersonaConfigService personaConfigService;

    /** 创建角色配置 */
    @PostMapping
    public Result<PersonaConfig> create(@RequestBody @Valid PersonaConfig persona) {
        return Result.success(personaConfigService.create(persona));
    }

    /** 更新角色配置 */
    @PutMapping("/{id}")
    public Result<PersonaConfig> update(@PathVariable Long id, @RequestBody @Valid PersonaConfig persona) {
        return Result.success(personaConfigService.update(id, persona));
    }

    /** 删除角色配置（软删除）*/
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        personaConfigService.delete(id);
        return Result.success(null);
    }

    /** 查询我的角色列表 */
    @GetMapping("/mine")
    public Result<List<PersonaConfig>> listMine() {
        return Result.success(personaConfigService.listMine());
    }

    /** 根据ID查询 */
    @GetMapping("/{id}")
    public Result<PersonaConfig> getById(@PathVariable Long id) {
        return Result.success(personaConfigService.getById(id));
    }
}
