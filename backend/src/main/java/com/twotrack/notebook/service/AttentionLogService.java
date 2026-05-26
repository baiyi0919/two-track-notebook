package com.twotrack.notebook.service;

import com.twotrack.notebook.dto.AttentionLogCreateDto;
import com.twotrack.notebook.entity.AttentionLog;

import java.time.LocalDate;
import java.util.List;

public interface AttentionLogService {

    /** 写入一条注意力日志，同时更新 task.actualTime */
    AttentionLog create(AttentionLogCreateDto dto);

    /** 查询某日的注意力日志 */
    List<AttentionLog> listByDate(LocalDate date);

    /** 查询某任务的所有日志 */
    List<AttentionLog> listByTask(Long taskId);

    /** 查询日期范围内的日志 */
    List<AttentionLog> listByDateRange(LocalDate start, LocalDate end);
}
