package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.dto.PrincipleCreateDto;
import com.twotrack.notebook.dto.PrincipleUpdateDto;
import com.twotrack.notebook.entity.Principle;
import com.twotrack.notebook.entity.Task;
import com.twotrack.notebook.mapper.PrincipleMapper;
import com.twotrack.notebook.mapper.TaskMapper;
import com.twotrack.notebook.service.PrincipleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrincipleServiceImpl implements PrincipleService {

    private final PrincipleMapper principleMapper;
    private final TaskMapper taskMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public Principle create(PrincipleCreateDto dto) {
        Principle principle = new Principle();
        principle.setUserId(currentUserId());
        principle.setContent(dto.getContent());
        principle.setSourceThreadId(dto.getSourceThreadId());
        principle.setTags(dto.getTags());
        principleMapper.insert(principle);
        return principle;
    }

    @Override
    public List<Principle> list(String keyword) {
        LambdaQueryWrapper<Principle> wrapper = new LambdaQueryWrapper<Principle>()
                .eq(Principle::getUserId, currentUserId())
                .like(keyword != null && !keyword.isEmpty(), Principle::getContent, keyword)
                .orderByDesc(Principle::getCreatedAt);
        return principleMapper.selectList(wrapper);
    }

    @Override
    public Principle getById(Long id) {
        Principle principle = principleMapper.selectById(id);
        if (principle == null || !principle.getUserId().equals(currentUserId())) {
            throw new RuntimeException("原则不存在或无权访问");
        }
        return principle;
    }

    @Override
    public Principle update(Long id, PrincipleUpdateDto dto) {
        Principle principle = getById(id);
        if (dto.getContent() != null) {
            principle.setContent(dto.getContent());
        }
        if (dto.getTags() != null) {
            principle.setTags(dto.getTags());
        }
        principleMapper.updateById(principle);
        return principle;
    }

    @Override
    public void delete(Long id) {
        Principle principle = getById(id);
        principleMapper.deleteById(principle.getId());
    }

    @Override
    public List<Task> getRelatedTasks(Long principleId) {
        return taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getPrincipleId, principleId)
                .orderByDesc(Task::getCreatedAt)
        );
    }
}
