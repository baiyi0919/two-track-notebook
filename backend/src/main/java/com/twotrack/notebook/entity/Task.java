package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    /** 现实锚点：一句话意义声明 */
    private String anchorText;

    /** 任务描述 */
    private String description;

    /** 注意力预算（小时）*/
    private Double budget;

    /** 实际用时（小时）*/
    private Double actualTime;

    private LocalDate dueDate;

    /** 0=待完成 1=已完成 2=已放弃 */
    private Integer status;

    /** 关联的个人原则ID */
    private Long principleId;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
