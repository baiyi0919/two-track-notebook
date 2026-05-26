package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reference")
public class Reference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 来源类型: TASK / THREAD / MESSAGE / PRINCIPLE */
    private String sourceType;

    /** 来源ID */
    private Long sourceId;

    /** 目标类型: TASK / THREAD / MESSAGE / PRINCIPLE */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
