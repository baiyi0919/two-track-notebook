package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskCreateDto {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题不超过100字")
    private String title;

    /** 现实锚点：强提示填写 */
    @Size(max = 500, message = "现实锚点不超过500字")
    private String anchorText;

    /** 任务描述 */
    private String description;

    /** 注意力预算（小时）*/
    private Double budget;

    private LocalDate dueDate;

    /** 关联的原则ID */
    private Long principleId;
}
