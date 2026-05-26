USE two_track_notebook;

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
