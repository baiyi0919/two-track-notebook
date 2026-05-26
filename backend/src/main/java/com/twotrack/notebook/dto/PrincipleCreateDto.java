package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrincipleCreateDto {

    @NotBlank(message = "原则内容不能为空")
    @Size(max = 500, message = "原则内容不超过500字")
    private String content;

    /** 来源议题ID（可选）*/
    private Long sourceThreadId;

    /** 标签（逗号分隔，可选）*/
    private String tags;
}
