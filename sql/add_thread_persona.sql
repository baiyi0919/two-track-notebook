-- ============================================================
-- 新增 thread_persona 表（议题-角色关联表）
-- ============================================================

CREATE TABLE IF NOT EXISTS `thread_persona` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `thread_id`   BIGINT        NOT NULL COMMENT '议题ID',
    `persona_id`  BIGINT        NOT NULL COMMENT '角色ID',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '在本议题中是否隐藏 0=显示 1=隐藏',
    `sort_order`  INT           NOT NULL DEFAULT 0 COMMENT '在议题中的排序',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_thread_persona` (`thread_id`, `persona_id`),
    CONSTRAINT `fk_tp_thread`  FOREIGN KEY (`thread_id`)  REFERENCES `thread` (`id`),
    CONSTRAINT `fk_tp_persona` FOREIGN KEY (`persona_id`) REFERENCES `persona_config` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='议题-角色关联表（多对多）';
