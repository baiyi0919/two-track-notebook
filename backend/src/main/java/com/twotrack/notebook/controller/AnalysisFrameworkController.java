package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.entity.AnalysisFramework;
import com.twotrack.notebook.service.AnalysisFrameworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis-frameworks")
@SaCheckLogin
@RequiredArgsConstructor
public class AnalysisFrameworkController {

    private final AnalysisFrameworkService analysisFrameworkService;

    /** 保存或更新分析框架 */
    @PostMapping
    public Result<AnalysisFramework> saveOrUpdate(@RequestParam Long personaId, @RequestBody String content) {
        return Result.success(analysisFrameworkService.saveOrUpdate(personaId, content));
    }

    /** 根据角色ID查询分析框架 */
    @GetMapping
    public Result<AnalysisFramework> getByPersonaId(@RequestParam Long personaId) {
        return Result.success(analysisFrameworkService.getByPersonaId(personaId));
    }

    /** 更新下次更新时间 */
    @PutMapping("/next-update-time")
    public Result<Void> updateNextUpdateTime(@RequestParam Long personaId, @RequestParam String nextTime) {
        java.time.LocalDateTime nextDateTime = java.time.LocalDateTime.parse(nextTime);
        analysisFrameworkService.updateNextUpdateTime(personaId, nextDateTime);
        return Result.success(null);
    }
}
