package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.PrincipleCreateDto;
import com.twotrack.notebook.dto.PrincipleUpdateDto;
import com.twotrack.notebook.entity.Principle;
import com.twotrack.notebook.entity.Task;
import com.twotrack.notebook.service.PrincipleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/principles")
@SaCheckLogin
@RequiredArgsConstructor
public class PrincipleController {

    private final PrincipleService principleService;

    /** 创建原则（从议题中手动提取）*/
    @PostMapping
    public Result<Principle> create(@Valid @RequestBody PrincipleCreateDto dto) {
        return Result.success(principleService.create(dto));
    }

    /** 查询个人宪法（原则列表），支持关键词检索 */
    @GetMapping
    public Result<List<Principle>> list(@RequestParam(required = false) String keyword) {
        return Result.success(principleService.list(keyword));
    }

    /** 查询单个原则 */
    @GetMapping("/{id}")
    public Result<Principle> detail(@PathVariable Long id) {
        return Result.success(principleService.getById(id));
    }

    /** 删除原则 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        principleService.delete(id);
        return Result.success();
    }

    /** 更新原则 */
    @PutMapping("/{id}")
    public Result<Principle> update(@PathVariable Long id, @Valid @RequestBody PrincipleUpdateDto dto) {
        return Result.success(principleService.update(id, dto));
    }

    /** 查询关联到此原则的任务列表（反向关联）*/
    @GetMapping("/{id}/related-tasks")
    public Result<List<Task>> getRelatedTasks(@PathVariable Long id) {
        // 先校验原则是否存在且属于当前用户
        principleService.getById(id);
        return Result.success(principleService.getRelatedTasks(id));
    }
}
