package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("principle")
public class Principle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 原则内容（一句话行动准则）*/
    private String content;

    /** 来源议题ID */
    private Long sourceThreadId;

    /** 标签（逗号分隔）*/
    private String tags;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
