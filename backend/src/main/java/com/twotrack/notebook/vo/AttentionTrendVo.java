package com.twotrack.notebook.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttentionTrendVo {

    /** 趋势天数 */
    private Integer days;

    /** 每日趋势数据 */
    private List<DailyItem> daily;

    /** 期间总预算（小时） */
    private Double periodTotalBudget;

    /** 期间总实际（小时） */
    private Double periodTotalActual;

    /** 期间平均消耗率(%) */
    private Double avgEfficiency;

    /** 期间完成任务数 */
    private Integer periodCompletedCount;

    @Data
    public static class DailyItem {
        private String date;
        /** 预算（小时） */
        private Double budget;
        /** 实际用时（小时） */
        private Double actualTime;
        private Integer completedCount;
        private Integer pendingCount;
        private Integer abandonedCount;
        /** 消耗率(%) */
        private Double efficiency;
    }
}
