CREATE TABLE `t_article_access_token` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `article_id` BIGINT NOT NULL COMMENT '逻辑引用 t_article.id',
  `token_hash` CHAR(64) NOT NULL COMMENT 'SHA-256(文章访问 token)，不存明文',
  `expires_at` DATETIME NOT NULL COMMENT '访问授权截止时间',
  `revoked` TINYINT NOT NULL DEFAULT 0 COMMENT '已撤销 0=否 1=是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '系统操作为空',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '系统操作为空',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_access_token_hash` (`token_hash`),
  KEY `idx_article_access_token_article_active` (`article_id`, `revoked`, `expires_at`),
  KEY `idx_article_access_token_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PASSWORD 文章短期访问授权（物理删留待后续维护）';
