package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageCreateDto {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不超过5000字")
    private String content;

    /** 发言角色名（MVP阶段可选，默认空即"我"）*/
    private String roleName;

    /** 发言角色ID（NULL表示用户本人，多人模式使用）*/
    private Long personaId;

    /** 引用的灵感ID（可选）*/
    private Long refInspirationId;
}
