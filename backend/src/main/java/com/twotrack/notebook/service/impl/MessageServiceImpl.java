package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.dto.MessageCreateDto;
import com.twotrack.notebook.entity.Message;
import com.twotrack.notebook.entity.Thread;
import com.twotrack.notebook.mapper.MessageMapper;
import com.twotrack.notebook.mapper.ThreadMapper;
import com.twotrack.notebook.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final ThreadMapper threadMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public Message send(Long threadId, MessageCreateDto dto) {
        // 校验议题属于当前用户且仍开放
        Thread thread = threadMapper.selectById(threadId);
        if (thread == null || !thread.getUserId().equals(currentUserId())) {
            throw new RuntimeException("议题不存在或无权访问");
        }
        if (thread.getStatus() == 1) {
            throw new RuntimeException("议题已关闭，无法继续发言");
        }

        Message message = new Message();
        message.setThreadId(threadId);
        message.setUserId(currentUserId());
        message.setPersonaId(dto.getPersonaId()); // 支持多人模式
        message.setRoleName(dto.getRoleName() != null ? dto.getRoleName() : "");
        message.setContent(dto.getContent());
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<Message> listByThread(Long threadId) {
        // 校验议题属于当前用户
        Thread thread = threadMapper.selectById(threadId);
        if (thread == null || !thread.getUserId().equals(currentUserId())) {
            throw new RuntimeException("议题不存在或无权访问");
        }

        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getThreadId, threadId)
                        .orderByAsc(Message::getCreatedAt)
        );
    }
}
