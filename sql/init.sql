-- ============================================================
-- Two-Track Notebook 双轨笔记本
-- 数据库初始化脚本 v1.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS two_track_notebook
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE two_track_notebook;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(50)     NOT NULL COMMENT '用户名',
    `password`      VARCHAR(255)    NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`      VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    `avatar_url`    VARCHAR(512)    DEFAULT NULL COMMENT '头像URL',
    `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删除 0=正常 1=已删除',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- ------------------------------------------------------------
-- 2. 任务表（现实轨核心）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `task` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `user_id`       BIGINT          NOT NULL COMMENT '所属用户ID',
    `title`         VARCHAR(200)    NOT NULL COMMENT '任务标题',
    `anchor_text`   VARCHAR(500)    DEFAULT NULL COMMENT '现实锚点：一句话意义声明',
    `description`   TEXT            DEFAULT NULL COMMENT '任务描述',
    `budget`        DOUBLE          DEFAULT NULL COMMENT '注意力预算（小时）',
    `actual_time`   DOUBLE          DEFAULT NULL COMMENT '实际用时（小时）',
    `due_date`      DATE            DEFAULT NULL COMMENT '截止日期',
    `status`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '状态 0=待完成 1=已完成 2=已放弃',
    `principle_id`  BIGINT          DEFAULT NULL COMMENT '关联的个人原则ID（锚点强化）',
    `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删除',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_due_date` (`due_date`),
    CONSTRAINT `fk_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表（现实轨）';


-- ------------------------------------------------------------
-- 3. 议题/线程表（探索轨）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `thread` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '议题ID',
    `user_id`       BIGINT          NOT NULL COMMENT '所属用户ID',
    `topic`         VARCHAR(200)    NOT NULL COMMENT '议题标题',
    `description`   TEXT            DEFAULT NULL COMMENT '议题背景描述',
    `status`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '状态 0=开放 1=已总结',
    `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删除',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    CONSTRAINT `fk_thread_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='思想议题表（探索轨）';


-- ------------------------------------------------------------
-- 3.5 角色配置表（多人模式）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `persona_config` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `user_id`       BIGINT          NOT NULL COMMENT '所属用户ID',
    `name`          VARCHAR(100)    NOT NULL COMMENT '角色名称',
    `avatar`        VARCHAR(10)     DEFAULT NULL COMMENT '头像Emoji',
    `model`         VARCHAR(100)    DEFAULT NULL COMMENT 'AI模型名称',
    `api_key`       VARCHAR(500)    DEFAULT NULL COMMENT 'API密钥（加密存储）',
    `api_url`       VARCHAR(500)    DEFAULT NULL COMMENT 'API地址',
    `personality`   TEXT            DEFAULT NULL COMMENT '人格设定',
    `system_prompt` TEXT            DEFAULT NULL COMMENT '系统提示词',
    `is_deleted`    TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '软删除',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_persona_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色配置表（多人模式）';


-- ------------------------------------------------------------
-- 3.6 分析框架表（多人模式）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `analysis_framework` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '框架ID',
    `persona_id`        BIGINT          NOT NULL COMMENT '角色ID',
    `content`           TEXT            NOT NULL COMMENT '分析框架内容',
    `last_updated`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `next_update_time`  DATETIME        DEFAULT NULL COMMENT '下次更新时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_persona_id` (`persona_id`),
    CONSTRAINT `fk_framework_persona` FOREIGN KEY (`persona_id`) REFERENCES `persona_config` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析框架表（多人模式）';


-- ------------------------------------------------------------
-- 4. 消息表（思想沙盒对话）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `message` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `thread_id`     BIGINT          NOT NULL COMMENT '所属议题ID',
    `user_id`       BIGINT          NOT NULL COMMENT '发言用户ID',
    `persona_id`    BIGINT          DEFAULT NULL COMMENT '发言角色ID（NULL表示用户本人）',
    `role_name`     VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '发言角色名（兼容旧数据）',
    `content`       TEXT            NOT NULL COMMENT '消息内容',
    `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删除',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_thread_id` (`thread_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_persona_id` (`persona_id`),
    CONSTRAINT `fk_message_thread` FOREIGN KEY (`thread_id`) REFERENCES `thread` (`id`),
    CONSTRAINT `fk_message_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_message_persona` FOREIGN KEY (`persona_id`) REFERENCES `persona_config` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='思想沙盒消息表';


-- ------------------------------------------------------------
-- 5. 原则表（个人宪法）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `principle` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '原则ID',
    `user_id`           BIGINT          NOT NULL COMMENT '所属用户ID',
    `content`           VARCHAR(500)    NOT NULL COMMENT '原则内容（一句话行动准则）',
    `source_thread_id`  BIGINT          DEFAULT NULL COMMENT '来源议题ID（从哪个沙盒讨论提取）',
    `tags`              VARCHAR(200)    DEFAULT NULL COMMENT '标签（逗号分隔，便于检索）',
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '软删除',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_source_thread` (`source_thread_id`),
    CONSTRAINT `fk_principle_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_principle_thread` FOREIGN KEY (`source_thread_id`) REFERENCES `thread` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人原则表（宪法库）';


-- ------------------------------------------------------------
-- 6. 补全 task 表外键（principle_id 依赖 principle 表，需在其后添加）
-- ------------------------------------------------------------
ALTER TABLE `task`
    ADD CONSTRAINT `fk_task_principle`
    FOREIGN KEY (`principle_id`) REFERENCES `principle` (`id`);


-- ------------------------------------------------------------
-- 7. 注意力预算日志表（支持日末审计报告）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attention_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `task_id`       BIGINT          NOT NULL COMMENT '关联任务ID',
    `log_date`      DATE            NOT NULL COMMENT '记录日期',
    `budget`        DOUBLE          DEFAULT NULL COMMENT '当日预算（小时）',
    `actual_time`   DOUBLE          DEFAULT NULL COMMENT '实际用时（小时）',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `log_date`),
    KEY `idx_task_id` (`task_id`),
    CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_log_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='注意力预算日志表';


-- ------------------------------------------------------------
-- 8. 知识引用表（支持任意实体间的引用关系）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reference` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '引用ID',
    `user_id`       BIGINT          NOT NULL COMMENT '所属用户ID',
    `source_type`   VARCHAR(20)     NOT NULL COMMENT '来源类型: TASK/THREAD/MESSAGE/PRINCIPLE',
    `source_id`     BIGINT          NOT NULL COMMENT '来源ID',
    `target_type`   VARCHAR(20)     NOT NULL COMMENT '目标类型: TASK/THREAD/MESSAGE/PRINCIPLE',
    `target_id`     BIGINT          NOT NULL COMMENT '目标ID',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_source` (`source_type`, `source_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    CONSTRAINT `fk_reference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识引用表';


-- ============================================================
-- 验证建表结果
-- ============================================================
SELECT
    TABLE_NAME AS '表名',
    TABLE_COMMENT AS '说明',
    CREATE_TIME AS '创建时间'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'two_track_notebook'
ORDER BY CREATE_TIME;
