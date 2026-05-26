package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attention_log")
public class AttentionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long taskId;

    private LocalDate logDate;

    /** 当日预算（小时）*/
    private Double budget;

    /** 当日实际用时（小时）*/
    private Double actualTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
