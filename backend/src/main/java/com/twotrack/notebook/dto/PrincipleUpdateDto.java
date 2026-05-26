package com.twotrack.notebook.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrincipleUpdateDto {

    @Size(max = 500, message = "原则内容不超过500字")
    private String content;

    /** 标签（逗号分隔，可选）*/
    private String tags;
}
