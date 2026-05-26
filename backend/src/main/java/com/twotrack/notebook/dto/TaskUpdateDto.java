package com.twotrack.notebook.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateDto {

    private String title;

    private String anchorText;

    private String description;

    /** 注意力预算（小时）*/
    private Double budget;

    /** 实际用时（小时）*/
    private Double actualTime;

    private LocalDate dueDate;

    /** 0=待完成 1=已完成 2=已放弃 */
    private Integer status;

    private Long principleId;
}
