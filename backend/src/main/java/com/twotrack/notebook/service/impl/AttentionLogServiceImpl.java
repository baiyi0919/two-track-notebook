package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.dto.AttentionLogCreateDto;
import com.twotrack.notebook.entity.AttentionLog;
import com.twotrack.notebook.entity.Task;
import com.twotrack.notebook.mapper.AttentionLogMapper;
import com.twotrack.notebook.mapper.TaskMapper;
import com.twotrack.notebook.service.AttentionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttentionLogServiceImpl implements AttentionLogService {

    private final AttentionLogMapper attentionLogMapper;
    private final TaskMapper taskMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    @Transactional
    public AttentionLog create(AttentionLogCreateDto dto) {
        // 校验任务归属
        Task task = taskMapper.selectById(dto.getTaskId());
        if (task == null || !task.getUserId().equals(currentUserId())) {
            throw new RuntimeException("任务不存在或无权访问");
        }

        LocalDate logDate = dto.getLogDate() != null ? dto.getLogDate() : LocalDate.now();

        // 查是否已有该天该任务的日志，有则更新
        AttentionLog existing = attentionLogMapper.selectOne(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getUserId, currentUserId())
                        .eq(AttentionLog::getTaskId, dto.getTaskId())
                        .eq(AttentionLog::getLogDate, logDate)
        );

        if (existing != null) {
            // 更新已有日志
            if (dto.getBudget() != null) existing.setBudget(dto.getBudget());
            if (dto.getActualTime() != null) existing.setActualTime(dto.getActualTime());
            attentionLogMapper.updateById(existing);
        } else {
            // 新建日志
            existing = new AttentionLog();
            existing.setUserId(currentUserId());
            existing.setTaskId(dto.getTaskId());
            existing.setLogDate(logDate);
            existing.setBudget(dto.getBudget());
            existing.setActualTime(dto.getActualTime());
            attentionLogMapper.insert(existing);
        }

        // 重新计算 task.actualTime = sum(所有日志的 actualTime)
        recalcTaskActualTime(dto.getTaskId());

        return existing;
    }

    @Override
    public List<AttentionLog> listByDate(LocalDate date) {
        return attentionLogMapper.selectList(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getUserId, currentUserId())
                        .eq(AttentionLog::getLogDate, date)
                        .orderByDesc(AttentionLog::getCreatedAt)
        );
    }

    @Override
    public List<AttentionLog> listByTask(Long taskId) {
        return attentionLogMapper.selectList(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getUserId, currentUserId())
                        .eq(AttentionLog::getTaskId, taskId)
                        .orderByDesc(AttentionLog::getLogDate)
        );
    }

    @Override
    public List<AttentionLog> listByDateRange(LocalDate start, LocalDate end) {
        return attentionLogMapper.selectList(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getUserId, currentUserId())
                        .ge(AttentionLog::getLogDate, start)
                        .le(AttentionLog::getLogDate, end)
                        .orderByAsc(AttentionLog::getLogDate)
        );
    }

    /** 重新计算任务的累计实际用时 */
    private void recalcTaskActualTime(Long taskId) {
        List<AttentionLog> logs = attentionLogMapper.selectList(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getTaskId, taskId)
        );
        double total = logs.stream()
                .mapToDouble(log -> log.getActualTime() != null ? log.getActualTime() : 0.0)
                .sum();
        Task task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setActualTime(total);
            taskMapper.updateById(task);
        }
    }
}
