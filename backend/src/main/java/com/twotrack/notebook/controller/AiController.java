package com.twotrack.notebook.controller;

import com.twotrack.notebook.common.Result;
import com.twotrack.notebook.dto.AiChatDto;
import com.twotrack.notebook.entity.Message;
import com.twotrack.notebook.service.AiProxyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProxyService aiProxyService;

    /**
     * 触发 AI 角色回复
     */
    @PostMapping("/chat")
    public Result<Message> chat(@Valid @RequestBody AiChatDto dto) {
        Message message = aiProxyService.chat(dto.getThreadId(), dto.getPersonaId());
        return Result.success(message);
    }
}
