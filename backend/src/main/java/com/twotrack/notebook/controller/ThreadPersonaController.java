package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.entity.ThreadPersona;
import com.twotrack.notebook.service.ThreadPersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@RestController
@RequestMapping("/thread-personas")
@RequiredArgsConstructor
public class ThreadPersonaController {

    private final ThreadPersonaService threadPersonaService;

    /**
     * 获取某议题中激活的角色列表
     * GET /api/thread-personas?threadId=1
     */
    @GetMapping
    public Result<List<ThreadPersona>> getByThread(@RequestParam Long threadId) {
        return Result.success(threadPersonaService.getActivePersonas(threadId));
    }

    /**
     * 为议题添加角色（如已隐藏则恢复显示）
     * POST /api/thread-personas?threadId=1&personaId=2
     */
    @PostMapping
    public Result<ThreadPersona> addPersona(
            @RequestParam Long threadId,
            @RequestParam Long personaId) {
        return Result.success(threadPersonaService.addPersonaToThread(threadId, personaId));
    }

    /**
     * 从议题中隐藏角色（软删除，不影响其他议题）
     * DELETE /api/thread-personas?threadId=1&personaId=2
     */
    @DeleteMapping
    public Result<Void> hidePersona(
            @RequestParam Long threadId,
            @RequestParam Long personaId) {
        threadPersonaService.hidePersonaFromThread(threadId, personaId);
        return Result.success(null);
    }
}
