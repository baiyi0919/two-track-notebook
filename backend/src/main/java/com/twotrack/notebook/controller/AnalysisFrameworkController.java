package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.entity.AnalysisFramework;
import com.twotrack.notebook.service.AnalysisFrameworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    /** 更新下次更新时间（接收 ISO 格式字符串） */
    @PutMapping("/next-update-time")
    public Result<Void> updateNextUpdateTime(@RequestParam Long personaId, @RequestParam String nextTime) {
        LocalDateTime nextDateTime = LocalDateTime.parse(nextTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        analysisFrameworkService.updateNextUpdateTime(personaId, nextDateTime);
        return Result.success(null);
    }

    /** 查询当前用户待更新的框架数量 */
    @GetMapping("/pending-count")
    public Result<Long> getPendingUpdateCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = analysisFrameworkService.getPendingUpdateCount(userId);
        return Result.success(count);
    }

    /** 获取当前用户待更新的框架列表 */
    @GetMapping("/pending-list")
    public Result<List<AnalysisFramework>> getPendingUpdateList() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AnalysisFramework> list = analysisFrameworkService.getPendingUpdateList(userId);
        return Result.success(list);
    }

    /** 立即触发某个角色的框架更新 */
    @PostMapping("/trigger-update")
    public Result<AnalysisFramework> triggerUpdate(@RequestParam Long personaId) {
        AnalysisFramework framework = analysisFrameworkService.triggerUpdateForPersona(personaId);
        return Result.success(framework);
    }

    /** 批量补全当前用户所有AI角色的框架记录（修复历史数据） */
    @PostMapping("/ensure-all")
    public Result<Integer> ensureAll() {
        int count = analysisFrameworkService.ensureAllFrameworksExist();
        return Result.success(count);
    }
}
