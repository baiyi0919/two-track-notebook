package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("thread")
public class Thread {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String topic;

    private String description;

    /** 0=开放 1=已总结 */
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
