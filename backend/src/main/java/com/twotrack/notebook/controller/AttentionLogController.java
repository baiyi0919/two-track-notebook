package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.AttentionLogCreateDto;
import com.twotrack.notebook.entity.AttentionLog;
import com.twotrack.notebook.service.AttentionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attention-logs")
@SaCheckLogin
@RequiredArgsConstructor
public class AttentionLogController {

    private final AttentionLogService attentionLogService;

    /** 写入/更新注意力日志 */
    @PostMapping
    public Result<AttentionLog> create(@RequestBody AttentionLogCreateDto dto) {
        return Result.success(attentionLogService.create(dto));
    }

    /** 查询某日的注意力日志 */
    @GetMapping("/by-date")
    public Result<List<AttentionLog>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(attentionLogService.listByDate(date));
    }

    /** 查询某任务的所有日志 */
    @GetMapping("/by-task/{taskId}")
    public Result<List<AttentionLog>> listByTask(@PathVariable Long taskId) {
        return Result.success(attentionLogService.listByTask(taskId));
    }

    /** 查询日期范围内的日志 */
    @GetMapping("/by-range")
    public Result<List<AttentionLog>> listByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.success(attentionLogService.listByDateRange(start, end));
    }
}
