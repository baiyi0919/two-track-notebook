package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.dto.ThreadCreateDto;
import com.twotrack.notebook.dto.ThreadUpdateDto;
import com.twotrack.notebook.entity.Message;
import com.twotrack.notebook.entity.Thread;
import com.twotrack.notebook.mapper.MessageMapper;
import com.twotrack.notebook.mapper.ThreadMapper;
import com.twotrack.notebook.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThreadServiceImpl implements ThreadService {

    private final ThreadMapper threadMapper;
    private final MessageMapper messageMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public Thread create(ThreadCreateDto dto) {
        Thread thread = new Thread();
        thread.setUserId(currentUserId());
        thread.setTopic(dto.getTopic());
        thread.setDescription(dto.getDescription());
        thread.setStatus(0); // 开放
        threadMapper.insert(thread);
        return thread;
    }

    @Override
    public List<Thread> list(Integer status) {
        LambdaQueryWrapper<Thread> wrapper = new LambdaQueryWrapper<Thread>()
                .eq(Thread::getUserId, currentUserId())
                .eq(status != null, Thread::getStatus, status)
                .orderByDesc(Thread::getCreatedAt);
        return threadMapper.selectList(wrapper);
    }

    @Override
    public Thread getById(Long id) {
        Thread thread = threadMapper.selectById(id);
        if (thread == null || !thread.getUserId().equals(currentUserId())) {
            throw new RuntimeException("议题不存在或无权访问");
        }
        return thread;
    }

    @Override
    public Thread update(Long id, ThreadUpdateDto dto) {
        Thread thread = getById(id);
        thread.setTopic(dto.getTopic());
        if (dto.getDescription() != null) {
            thread.setDescription(dto.getDescription());
        }
        threadMapper.updateById(thread);
        return thread;
    }

    @Override
    public void delete(Long id) {
        // 校验权限
        getById(id);
        // 先删除关联消息
        messageMapper.delete(
                new LambdaQueryWrapper<Message>().eq(Message::getThreadId, id)
        );
        // 再删除议题
        threadMapper.deleteById(id);
    }

    @Override
    public Thread close(Long id) {
        Thread thread = getById(id);
        thread.setStatus(1); // 已总结
        threadMapper.updateById(thread);
        return thread;
    }
}
