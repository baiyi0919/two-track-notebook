package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_framework")
public class AnalysisFramework {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID */
    private Long personaId;

    /** 分析框架内容 */
    private String content;

    /** 最后更新时间 */
    private LocalDateTime lastUpdated;

    /** 下次更新时间 */
    private LocalDateTime nextUpdateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
