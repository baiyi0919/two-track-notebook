package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ThreadCreateDto {

    @NotBlank(message = "议题标题不能为空")
    @Size(max = 200, message = "议题标题不超过200字")
    private String topic;

    @Size(max = 1000, message = "议题描述不超过1000字")
    private String description;
}
