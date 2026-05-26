package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.TaskCreateDto;
import com.twotrack.notebook.dto.TaskUpdateDto;
import com.twotrack.notebook.entity.Task;
import com.twotrack.notebook.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@SaCheckLogin
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 创建任务（含现实锚点）*/
    @PostMapping
    public Result<Task> create(@Valid @RequestBody TaskCreateDto dto) {
        return Result.success(taskService.create(dto));
    }

    /** 查询任务列表，可按状态筛选、排序 */
    @GetMapping
    public Result<List<Task>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return Result.success(taskService.list(status, sortBy, sortOrder));
    }

    /** 查询单个任务详情 */
    @GetMapping("/{id}")
    public Result<Task> detail(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    /** 更新任务 */
    @PutMapping("/{id}")
    public Result<Task> update(@PathVariable Long id, @RequestBody TaskUpdateDto dto) {
        return Result.success(taskService.update(id, dto));
    }

    /** 删除任务 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success();
    }

    /** 切换任务状态 */
    @PatchMapping("/{id}/status")
    public Result<Task> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(taskService.toggleStatus(id, status));
    }
}
