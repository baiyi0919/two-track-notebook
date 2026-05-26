package com.twotrack.notebook.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttentionReportVo {

    /** 日期 */
    private String date;

    /** 总预算（小时）*/
    private Double totalBudget;

    /** 总实际用时（小时）*/
    private Double totalActual;

    /** 已完成任务数 */
    private Integer completedCount;

    /** 未完成任务数 */
    private Integer pendingCount;

    /** 已放弃任务数 */
    private Integer abandonedCount;

    /** 消耗率(%) */
    private Double efficiency;

    /** 预算偏差（小时）：实际-预算（正=超支） */
    private Double budgetVariance;

    /** 当日预算（小时，来自日志） */
    private Double dayBudget;

    /** 当日实际用时（小时，来自日志） */
    private Double dayActual;

    /** 各任务明细 */
    private List<TaskItem> tasks;

    /** 按状态分组的预算分布 */
    private StatusBudget statusBudget;

    @Data
    public static class TaskItem {
        private Long taskId;
        private String title;
        private String anchorText;
        /** 预算（小时） */
        private Double budget;
        /** 实际用时（小时） */
        private Double actualTime;
        /** 0=待完成 1=已完成 2=已放弃 */
        private Integer status;
        /** 截止日期 */
        private String dueDate;
        /** 关联原则ID */
        private Long principleId;
        /** 消耗率(%) */
        private Double efficiency;
        /** 是否超支 */
        private Boolean overBudget;
    }

    @Data
    public static class StatusBudget {
        /** 进行中预算（小时） */
        private Double pendingBudget;
        /** 进行中实际（小时） */
        private Double pendingActual;
        /** 已完成预算（小时） */
        private Double completedBudget;
        /** 已完成实际（小时） */
        private Double completedActual;
        /** 已放弃预算（小时） */
        private Double abandonedBudget;
        /** 已放弃实际（小时） */
        private Double abandonedActual;
    }
}
