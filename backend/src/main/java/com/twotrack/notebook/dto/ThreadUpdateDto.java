package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThreadUpdateDto {
    @NotBlank(message = "议题名称不能为空")
    private String topic;

    private String description;
}
