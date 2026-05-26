package com.twotrack.notebook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("persona_config")
public class PersonaConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 角色名称 */
    private String name;

    /** 头像Emoji */
    private String avatar;

    /** AI模型名称 */
    private String model;

    /** API密钥（加密存储）*/
    private String apiKey;

    /** API地址 */
    private String apiUrl;

    /** 人格设定 */
    private String personality;

    /** 系统提示词 */
    private String systemPrompt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
