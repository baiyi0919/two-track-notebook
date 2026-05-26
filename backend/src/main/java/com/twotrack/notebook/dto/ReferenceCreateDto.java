package com.twotrack.notebook.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ReferenceCreateDto {

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;  // TASK / THREAD / MESSAGE / PRINCIPLE

    @NotNull(message = "来源ID不能为空")
    private Long sourceId;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;  // TASK / THREAD / MESSAGE / PRINCIPLE

    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}
