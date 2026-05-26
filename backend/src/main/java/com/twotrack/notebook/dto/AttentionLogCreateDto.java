package com.twotrack.notebook.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttentionLogCreateDto {

    /** 关联任务ID */
    private Long taskId;

    /** 记录日期，默认今天 */
    private LocalDate logDate;

    /** 当日预算（小时）*/
    private Double budget;

    /** 当日实际用时（小时）*/
    private Double actualTime;
}
