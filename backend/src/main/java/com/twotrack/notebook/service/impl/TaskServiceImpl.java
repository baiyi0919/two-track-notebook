package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.dto.TaskCreateDto;
import com.twotrack.notebook.dto.TaskUpdateDto;
import com.twotrack.notebook.entity.Task;
import com.twotrack.notebook.mapper.TaskMapper;
import com.twotrack.notebook.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public Task create(TaskCreateDto dto) {
        Task task = new Task();
        task.setUserId(currentUserId());
        task.setTitle(dto.getTitle());
        task.setAnchorText(dto.getAnchorText());
        task.setDescription(dto.getDescription());
        task.setBudget(dto.getBudget());
        task.setDueDate(dto.getDueDate());
        task.setPrincipleId(dto.getPrincipleId());
        task.setStatus(0); // 待完成
        task.setActualTime(0.0);
        taskMapper.insert(task);
        return task;
    }

    @Override
    public List<Task> list(Integer status, String sortBy, String sortOrder) {
        // 排序字段白名单，防止SQL注入
        String sortColumn = switch (sortBy) {
            case "dueDate" -> "due_date";
            case "budget" -> "budget";
            case "actualTime" -> "actual_time";
            case "status" -> "status";
            default -> "created_at";
        };

        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, currentUserId())
                .eq(status != null, Task::getStatus, status);

        // 使用 orderByDesc/orderByAsc + last 来实现动态排序
        if ("due_date".equals(sortColumn)) {
            wrapper.orderBy(true, isAsc, Task::getDueDate);
        } else if ("budget".equals(sortColumn)) {
            wrapper.orderBy(true, isAsc, Task::getBudget);
        } else if ("actual_time".equals(sortColumn)) {
            wrapper.orderBy(true, isAsc, Task::getActualTime);
        } else if ("status".equals(sortColumn)) {
            wrapper.orderBy(true, isAsc, Task::getStatus);
        } else {
            wrapper.orderByDesc(Task::getCreatedAt);
        }

        return taskMapper.selectList(wrapper);
    }

    @Override
    public Task getById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null || !task.getUserId().equals(currentUserId())) {
            throw new RuntimeException("任务不存在或无权访问");
        }
        return task;
    }

    @Override
    public Task update(Long id, TaskUpdateDto dto) {
        Task task = getById(id); // 复用权限校验
        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getAnchorText() != null) task.setAnchorText(dto.getAnchorText());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getBudget() != null) task.setBudget(dto.getBudget());
        if (dto.getActualTime() != null) task.setActualTime(dto.getActualTime());
        if (dto.getDueDate() != null) task.setDueDate(dto.getDueDate());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());
        // principleId: null=不修改, 0=清除关联, >0=设置关联
        if (dto.getPrincipleId() != null) {
            task.setPrincipleId(dto.getPrincipleId() == 0 ? null : dto.getPrincipleId());
        }
        taskMapper.updateById(task);
        return task;
    }

    @Override
    public void delete(Long id) {
        Task task = getById(id);
        taskMapper.deleteById(task.getId()); // 逻辑删除
    }

    @Override
    public Task toggleStatus(Long id, Integer status) {
        Task task = getById(id);
        task.setStatus(status);
        taskMapper.updateById(task);
        return task;
    }
}
