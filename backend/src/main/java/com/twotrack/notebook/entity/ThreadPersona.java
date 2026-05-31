package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 议题-角色关联实体（多对多）
 */
@Data
@TableName("thread_persona")
public class ThreadPersona {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 议题ID */
    private Long threadId;

    /** 角色ID */
    private Long personaId;

    /** 在本议题中是否隐藏 0=显示 1=隐藏 */
    private Integer isDeleted;

    /** 在议题中的排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
