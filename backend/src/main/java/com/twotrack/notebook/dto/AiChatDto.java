package com.twotrack.notebook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiChatDto {

    /** 议题ID */
    @NotNull
    private Long threadId;

    /** AI角色配置ID */
    @NotNull
    private Long personaId;
}
