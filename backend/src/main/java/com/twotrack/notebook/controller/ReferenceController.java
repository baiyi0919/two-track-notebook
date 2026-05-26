package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.ReferenceCreateDto;
import com.twotrack.notebook.entity.Reference;
import com.twotrack.notebook.service.ReferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/references")
@SaCheckLogin
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    /** 创建引用关系 */
    @PostMapping
    public Result<Reference> create(@RequestBody @Valid ReferenceCreateDto dto) {
        Reference ref = referenceService.createReference(
                dto.getSourceType(),
                dto.getSourceId(),
                dto.getTargetType(),
                dto.getTargetId()
        );
        return Result.success(ref);
    }

    /** 删除引用 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        referenceService.deleteReference(id);
        return Result.success(null);
    }

    /** 获取某实体的引用列表（source） */
    @GetMapping("/source")
    public Result<List<Reference>> getBySource(
            @RequestParam String sourceType,
            @RequestParam Long sourceId) {
        return Result.success(referenceService.getBySource(sourceType, sourceId));
    }

    /** 获取某实体的反向链接（target / backlinks） */
    @GetMapping("/target")
    public Result<List<Reference>> getByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return Result.success(referenceService.getByTarget(targetType, targetId));
    }
}
