package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspirationDto {

    @NotBlank(message = "灵感内容不能为空")
    @Size(max = 1000, message = "灵感内容不超过1000字")
    private String content;
}
