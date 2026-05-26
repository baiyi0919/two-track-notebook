package com.twotrack.notebook.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.InspirationDto;
import com.twotrack.notebook.dto.MessageCreateDto;
import com.twotrack.notebook.dto.ThreadCreateDto;
import com.twotrack.notebook.dto.ThreadUpdateDto;
import com.twotrack.notebook.entity.Message;
import com.twotrack.notebook.entity.Thread;
import com.twotrack.notebook.service.MessageService;
import com.twotrack.notebook.service.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/threads")
@SaCheckLogin
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;
    private final MessageService messageService;

    // ==================== 议题管理 ====================

    /** 创建议题 */
    @PostMapping
    public Result<Thread> create(@Valid @RequestBody ThreadCreateDto dto) {
        return Result.success(threadService.create(dto));
    }

    /** 查询议题列表 */
    @GetMapping
    public Result<List<Thread>> list(@RequestParam(required = false) Integer status) {
        return Result.success(threadService.list(status));
    }

    /** 查询议题详情 */
    @GetMapping("/{id}")
    public Result<Thread> detail(@PathVariable Long id) {
        return Result.success(threadService.getById(id));
    }

    /** 更新议题（编辑 topic/description） */
    @PutMapping("/{id}")
    public Result<Thread> update(@PathVariable Long id, @Valid @RequestBody ThreadUpdateDto dto) {
        return Result.success(threadService.update(id, dto));
    }

    /** 删除议题（同时删除关联消息） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        threadService.delete(id);
        return Result.success(null);
    }

    /** 关闭议题（标记为已总结）*/
    @PatchMapping("/{id}/close")
    public Result<Thread> close(@PathVariable Long id) {
        return Result.success(threadService.close(id));
    }

    // ==================== 沙盒消息 ====================

    /** 在议题下发言 */
    @PostMapping("/{id}/messages")
    public Result<Message> sendMessage(@PathVariable Long id, @Valid @RequestBody MessageCreateDto dto) {
        return Result.success(messageService.send(id, dto));
    }

    /** 查询议题下的消息列表 */
    @GetMapping("/{id}/messages")
    public Result<List<Message>> listMessages(@PathVariable Long id) {
        return Result.success(messageService.listByThread(id));
    }

    // ==================== 灵感快寄 ====================

    /** 心流模式下快速保存灵感（自动创建一个收件箱议题）*/
    @PostMapping("/inspiration")
    public Result<Thread> quickInspiration(@Valid @RequestBody InspirationDto dto) {
        ThreadCreateDto threadDto = new ThreadCreateDto();
        threadDto.setTopic("💡 灵感: " + dto.getContent().substring(0, Math.min(dto.getContent().length(), 50)));
        threadDto.setDescription(dto.getContent());
        Thread thread = threadService.create(threadDto);
        // 自动关闭，标记为收件箱灵感
        threadService.close(thread.getId());
        return Result.success(thread);
    }
}
