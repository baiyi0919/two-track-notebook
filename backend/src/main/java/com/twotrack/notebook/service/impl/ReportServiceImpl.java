package com.twotrack.notebook.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twotrack.notebook.entity.AttentionLog;
import com.twotrack.notebook.entity.Task;
import com.twotrack.notebook.mapper.AttentionLogMapper;
import com.twotrack.notebook.mapper.TaskMapper;
import com.twotrack.notebook.service.ReportService;
import com.twotrack.notebook.vo.AttentionReportVo;
import com.twotrack.notebook.vo.AttentionTrendVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TaskMapper taskMapper;
    private final AttentionLogMapper attentionLogMapper;

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public AttentionReportVo getAttentionReport(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        // 查询用户所有未删除任务
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, currentUserId())
                        .orderByAsc(Task::getStatus)
                        .orderByDesc(Task::getCreatedAt)
        );

        // 查询当日的注意力日志
        List<AttentionLog> dayLogs = attentionLogMapper.selectList(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getUserId, currentUserId())
                        .eq(AttentionLog::getLogDate, date)
        );

        // 按taskId索引日志
        Map<Long, AttentionLog> logMap = dayLogs.stream()
                .collect(Collectors.toMap(AttentionLog::getTaskId, log -> log, (a, b) -> b));

        AttentionReportVo report = new AttentionReportVo();
        report.setDate(date.toString());

        double totalBudget = 0;
        double totalActual = 0;
        int completed = 0;
        int pending = 0;
        int abandoned = 0;
        double pendingBudget = 0, pendingActual = 0;
        double completedBudget = 0, completedActual = 0;
        double abandonedBudget = 0, abandonedActual = 0;
        double dayBudget = 0;
        double dayActual = 0;

        List<AttentionReportVo.TaskItem> items = new ArrayList<>();
        for (Task task : tasks) {
            AttentionReportVo.TaskItem item = new AttentionReportVo.TaskItem();
            item.setTaskId(task.getId());
            item.setTitle(task.getTitle());
            item.setAnchorText(task.getAnchorText());
            item.setBudget(task.getBudget() != null ? task.getBudget() : 0.0);
            item.setActualTime(task.getActualTime() != null ? task.getActualTime() : 0.0);
            item.setStatus(task.getStatus());
            item.setDueDate(task.getDueDate() != null ? task.getDueDate().toString() : null);
            item.setPrincipleId(task.getPrincipleId());

            // 计算每个任务的消耗率和是否超支
            if (item.getBudget() != null && item.getBudget() > 0) {
                item.setEfficiency(item.getActualTime() / item.getBudget() * 100);
                item.setOverBudget(item.getActualTime() > item.getBudget());
            } else {
                item.setEfficiency(item.getActualTime() > 0 ? 999.0 : 0.0);
                item.setOverBudget(false);
            }

            items.add(item);

            totalBudget += item.getBudget();
            totalActual += item.getActualTime();

            // 统计当天的日志数据
            AttentionLog dayLog = logMap.get(task.getId());
            if (dayLog != null) {
                dayBudget += dayLog.getBudget() != null ? dayLog.getBudget() : 0.0;
                dayActual += dayLog.getActualTime() != null ? dayLog.getActualTime() : 0.0;
            }

            switch (task.getStatus()) {
                case 0 -> {
                    pending++;
                    pendingBudget += item.getBudget();
                    pendingActual += item.getActualTime();
                }
                case 1 -> {
                    completed++;
                    completedBudget += item.getBudget();
                    completedActual += item.getActualTime();
                }
                case 2 -> {
                    abandoned++;
                    abandonedBudget += item.getBudget();
                    abandonedActual += item.getActualTime();
                }
            }
        }

        report.setTotalBudget(totalBudget);
        report.setTotalActual(totalActual);
        report.setCompletedCount(completed);
        report.setPendingCount(pending);
        report.setAbandonedCount(abandoned);
        report.setBudgetVariance(totalActual - totalBudget);
        report.setEfficiency(totalBudget > 0 ? (double) totalActual / totalBudget * 100 : 0.0);
        report.setTasks(items);

        // 按状态分组预算分布
        AttentionReportVo.StatusBudget statusBudget = new AttentionReportVo.StatusBudget();
        statusBudget.setPendingBudget(pendingBudget);
        statusBudget.setPendingActual(pendingActual);
        statusBudget.setCompletedBudget(completedBudget);
        statusBudget.setCompletedActual(completedActual);
        statusBudget.setAbandonedBudget(abandonedBudget);
        statusBudget.setAbandonedActual(abandonedActual);
        report.setStatusBudget(statusBudget);

        // 当日数据（来自日志）
        report.setDayBudget(dayBudget);
        report.setDayActual(dayActual);

        return report;
    }

    @Override
    public AttentionTrendVo getAttentionTrend(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        if (days > 30) {
            days = 30;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 从 attention_log 查询时间范围内的日志（更准确的每日数据）
        List<AttentionLog> logs = attentionLogMapper.selectList(
                new LambdaQueryWrapper<AttentionLog>()
                        .eq(AttentionLog::getUserId, currentUserId())
                        .ge(AttentionLog::getLogDate, startDate)
                        .le(AttentionLog::getLogDate, endDate)
        );

        // 按日期分组
        Map<LocalDate, List<AttentionLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(AttentionLog::getLogDate));

        AttentionTrendVo trend = new AttentionTrendVo();
        trend.setDays(days);

        double periodTotalBudget = 0;
        double periodTotalActual = 0;
        int validDays = 0;

        List<AttentionTrendVo.DailyItem> dailyItems = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            AttentionTrendVo.DailyItem daily = new AttentionTrendVo.DailyItem();
            daily.setDate(d.toString());

            List<AttentionLog> dayLogs = logsByDate.getOrDefault(d, List.of());
            double dayBudgetVal = dayLogs.stream()
                    .mapToDouble(log -> log.getBudget() != null ? log.getBudget() : 0.0)
                    .sum();
            double dayActualVal = dayLogs.stream()
                    .mapToDouble(log -> log.getActualTime() != null ? log.getActualTime() : 0.0)
                    .sum();
            long dayTaskCount = dayLogs.size();

            daily.setBudget(dayBudgetVal);
            daily.setActualTime(dayActualVal);
            daily.setEfficiency(dayBudgetVal > 0 ? dayActualVal / dayBudgetVal * 100 : 0.0);

            dailyItems.add(daily);

            periodTotalBudget += dayBudgetVal;
            periodTotalActual += dayActualVal;
            if (dayBudgetVal > 0 || dayActualVal > 0) validDays++;
        }

        trend.setDaily(dailyItems);
        trend.setPeriodTotalBudget(periodTotalBudget);
        trend.setPeriodTotalActual(periodTotalActual);
        trend.setPeriodCompletedCount(validDays);
        trend.setAvgEfficiency(periodTotalBudget > 0 ? (double) periodTotalActual / periodTotalBudget * 100 : 0.0);

        return trend;
    }
}
