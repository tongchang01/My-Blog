CREATE TABLE `t_user_auth` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID 雪花）',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password_hash` VARCHAR(72) NOT NULL COMMENT 'BCrypt 哈希（60 字符+余量）',
  `type` TINYINT NOT NULL COMMENT '账号类型 1=ADMIN 2=DEMO 3=GUEST',
  `token_version` INT NOT NULL DEFAULT 0 COMMENT 'JWT 撤销版本号',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(45) NULL DEFAULT NULL COMMENT '最后登录 IP（IPv6 兼容）',
  `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `locked_until` DATETIME NULL DEFAULT NULL COMMENT '账号锁定截止时间（NULL=未锁）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_auth_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户认证';

CREATE TABLE `t_user_info` (
  `user_id` BIGINT NOT NULL COMMENT 'PK + 逻辑引用 t_user_auth.id',
  `nickname` VARCHAR(64) NOT NULL COMMENT '展示昵称',
  `avatar_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像 URL',
  `bio_zh` TEXT NULL COMMENT '个人简介（中）',
  `bio_ja` TEXT NULL COMMENT '个人简介（日）',
  `bio_en` TEXT NULL COMMENT '个人简介（英）',
  `location` VARCHAR(64) NULL DEFAULT NULL COMMENT '所在地（如 Tokyo）',
  `website` VARCHAR(255) NULL DEFAULT NULL COMMENT '个人主页',
  `email_public` VARCHAR(128) NULL DEFAULT NULL COMMENT '公开邮箱（可选）',
  `github_url` VARCHAR(255) NULL DEFAULT NULL COMMENT 'GitHub 主页 URL',
  `twitter_url` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Twitter / X 主页 URL',
  `linkedin_url` VARCHAR(255) NULL DEFAULT NULL COMMENT 'LinkedIn 主页 URL',
  `zhihu_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '知乎主页 URL',
  `qiita_url` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Qiita 主页 URL',
  `juejin_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '掘金主页 URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料（1:1 t_user_auth）';

CREATE TABLE `t_refresh_token` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `user_id` BIGINT NOT NULL COMMENT '逻辑引用 t_user_auth.id',
  `token_hash` VARCHAR(64) NOT NULL COMMENT 'SHA-256(refresh token)，不存明文',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  `revoked` TINYINT NOT NULL DEFAULT 0 COMMENT '已撤销 0=未 1=是（取代软删）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_token_hash` (`token_hash`),
  KEY `idx_refresh_token_user_expires` (`user_id`, `expires_at`),
  KEY `idx_refresh_token_expires` (`expires_at`),
  KEY `idx_refresh_token_revoked_created` (`revoked`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Refresh Token（物理删，无软删）';

CREATE TABLE `t_article` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `title_zh` VARCHAR(255) NULL DEFAULT NULL COMMENT '标题（中）',
  `title_ja` VARCHAR(255) NULL DEFAULT NULL COMMENT '标题（日）',
  `title_en` VARCHAR(255) NULL DEFAULT NULL COMMENT '标题（英）',
  `summary_zh` VARCHAR(500) NULL DEFAULT NULL COMMENT '摘要（中）',
  `summary_ja` VARCHAR(500) NULL DEFAULT NULL COMMENT '摘要（日）',
  `summary_en` VARCHAR(500) NULL DEFAULT NULL COMMENT '摘要（英）',
  `body` MEDIUMTEXT NULL COMMENT '正文 Markdown（单中文）',
  `category_id` BIGINT NULL DEFAULT NULL COMMENT '逻辑引用 t_category.id（DRAFT 可空，其他状态必填）',
  `author_id` BIGINT NOT NULL COMMENT '逻辑引用 t_user_auth.id',
  `slug` VARCHAR(160) NULL DEFAULT NULL COMMENT 'URL 别名 a-z 0-9 - ；不强制唯一',
  `status` TINYINT NOT NULL COMMENT '1=DRAFT 2=PUBLISHED 3=PRIVATE 4=PASSWORD 5=SCHEDULED',
  `access_password` VARCHAR(255) NULL DEFAULT NULL COMMENT 'PASSWORD 状态用，BCrypt 哈希',
  `publish_at` DATETIME NULL DEFAULT NULL COMMENT '公开发布时间；PUBLISHED/PASSWORD 为首次公开时间，SCHEDULED 为预定公开时间',
  `cover_attachment_id` BIGINT NULL DEFAULT NULL COMMENT '逻辑引用 t_attachment.id',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '冗余：target_type=ARTICLE AND target_id=本文 AND deleted=0 AND audit_status=PASS 的评论数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  KEY `idx_article_status_deleted_publish` (`status`, `deleted`, `publish_at`),
  KEY `idx_article_category_deleted` (`category_id`, `deleted`),
  KEY `idx_article_author` (`author_id`),
  KEY `idx_article_slug` (`slug`),
  KEY `idx_article_cover` (`cover_attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章';

CREATE TABLE `t_article_tag` (
  `article_id` BIGINT NOT NULL COMMENT '逻辑引用 t_article.id',
  `tag_id` BIGINT NOT NULL COMMENT '逻辑引用 t_tag.id',
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_article_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章-标签关联';

CREATE TABLE `t_category` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `name_zh` VARCHAR(64) NOT NULL COMMENT '名称（中）',
  `name_ja` VARCHAR(64) NULL DEFAULT NULL COMMENT '名称（日，fallback 到 zh）',
  `name_en` VARCHAR(64) NULL DEFAULT NULL COMMENT '名称（英，fallback 到 zh）',
  `slug` VARCHAR(64) NOT NULL COMMENT 'URL 别名（UNIQUE）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`),
  KEY `idx_category_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类（平铺）';

CREATE TABLE `t_tag` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `name_zh` VARCHAR(64) NOT NULL COMMENT '名称（中）',
  `name_ja` VARCHAR(64) NULL DEFAULT NULL COMMENT '名称（日，fallback 到 zh）',
  `name_en` VARCHAR(64) NULL DEFAULT NULL COMMENT '名称（英，fallback 到 zh）',
  `slug` VARCHAR(64) NOT NULL COMMENT 'URL 别名（UNIQUE）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签（白名单）';

CREATE TABLE `t_comment` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `target_type` TINYINT NOT NULL COMMENT '1=ARTICLE 2=GUESTBOOK（未来可扩展 3=ABOUT 等）',
  `target_id` BIGINT NOT NULL COMMENT 'ARTICLE 时是 t_article.id；GUESTBOOK 时固定 0',
  `parent_id` BIGINT NULL DEFAULT NULL COMMENT '顶层评论 id（挂楼用，顶层评论自身为 NULL）',
  `reply_to_comment_id` BIGINT NULL DEFAULT NULL COMMENT '本次实际回复的目标评论 id（通知用）',
  `reply_to_user_id` BIGINT NULL DEFAULT NULL COMMENT '被回复者为系统用户时的 user_id',
  `reply_to_nickname` VARCHAR(64) NULL DEFAULT NULL COMMENT '被回复者昵称快照',
  `author_user_id` BIGINT NULL DEFAULT NULL COMMENT '管理员评论时挂 t_user_auth.id；游客 NULL',
  `author_nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
  `author_email` VARCHAR(128) NOT NULL COMMENT '邮箱（不公开展示）',
  `author_site` VARCHAR(255) NULL DEFAULT NULL COMMENT '个人站（http/https 白名单）',
  `author_ip` VARCHAR(45) NULL DEFAULT NULL COMMENT '审计：IP',
  `author_user_agent` VARCHAR(512) NULL DEFAULT NULL COMMENT '审计：UA',
  `content_md` TEXT NOT NULL COMMENT '用户提交原文（Markdown，≤5000）',
  `content_html` TEXT NOT NULL COMMENT '清洗后安全 HTML（前台只渲染这个）',
  `audit_status` TINYINT NOT NULL COMMENT '1=PASS 2=PENDING 3=HIDDEN',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  KEY `idx_comment_target_deleted_audit_created` (`target_type`, `target_id`, `deleted`, `audit_status`, `created_at`),
  KEY `idx_comment_parent` (`parent_id`),
  KEY `idx_comment_reply_to` (`reply_to_comment_id`),
  KEY `idx_comment_author_user` (`author_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论 / 留言（多用途）';

CREATE TABLE `t_site_config` (
  `id` BIGINT NOT NULL COMMENT '固定为 1',
  `site_title_zh` VARCHAR(128) NOT NULL COMMENT '站点标题（中）',
  `site_title_ja` VARCHAR(128) NULL DEFAULT NULL COMMENT '站点标题（日）',
  `site_title_en` VARCHAR(128) NULL DEFAULT NULL COMMENT '站点标题（英）',
  `site_subtitle_zh` VARCHAR(255) NULL DEFAULT NULL COMMENT '副标题（中）',
  `site_subtitle_ja` VARCHAR(255) NULL DEFAULT NULL COMMENT '副标题（日）',
  `site_subtitle_en` VARCHAR(255) NULL DEFAULT NULL COMMENT '副标题（英）',
  `about_md_zh` MEDIUMTEXT NULL COMMENT '关于我（中）',
  `about_md_ja` MEDIUMTEXT NULL COMMENT '关于我（日）',
  `about_md_en` MEDIUMTEXT NULL COMMENT '关于我（英）',
  `logo_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '站点 Logo URL',
  `favicon_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '站点 favicon URL',
  `icp_no` VARCHAR(64) NULL DEFAULT NULL COMMENT 'ICP 备案号（中国大陆部署用）',
  `spotify_playlist_id` VARCHAR(64) NULL DEFAULT NULL COMMENT 'Spotify 播放列表 ID（替代 V1 音乐播放器）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站点配置（宽表，单行）';

CREATE TABLE `t_attachment` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `storage_type` VARCHAR(16) NOT NULL COMMENT 'LOCAL / S3 / OSS',
  `bucket` VARCHAR(64) NULL DEFAULT NULL COMMENT 'LOCAL 时为本地根目录别名',
  `object_key` VARCHAR(255) NOT NULL COMMENT '存储后端的对象键 / 相对路径',
  `public_url` VARCHAR(512) NOT NULL COMMENT '对外可访问 URL',
  `content_type` VARCHAR(64) NOT NULL COMMENT 'MIME 类型',
  `file_size` BIGINT NOT NULL COMMENT '字节',
  `width` INT NULL DEFAULT NULL COMMENT '仅图片',
  `height` INT NULL DEFAULT NULL COMMENT '仅图片',
  `original_filename` VARCHAR(255) NULL DEFAULT NULL COMMENT '原始文件名',
  `hash_sha256` VARCHAR(64) NOT NULL COMMENT 'SHA-256，用于去重',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attachment_hash` (`hash_sha256`),
  KEY `idx_attachment_storage_key` (`storage_type`, `object_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件';

CREATE TABLE `t_friend_link` (
  `id` BIGINT NOT NULL COMMENT '主键（ASSIGN_ID）',
  `name` VARCHAR(64) NOT NULL COMMENT '友链名称',
  `url` VARCHAR(255) NOT NULL COMMENT '站点地址',
  `avatar_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像 / logo',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT '一句话介绍（单中文）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=显示 2=隐藏',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建者 user_id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '最后修改者 user_id',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记 0=正常 1=已删',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  KEY `idx_friend_link_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='友链';

CREATE TABLE `t_page_view` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增 PK',
  `article_id` BIGINT NULL DEFAULT NULL COMMENT '逻辑引用 t_article.id（NULL=非文章页）',
  `lang` VARCHAR(8) NOT NULL COMMENT 'zh / ja / en',
  `visitor_hash` CHAR(64) NOT NULL COMMENT 'SHA-256(ip + ua)，用于 UV 去重',
  `referrer` VARCHAR(512) NULL DEFAULT NULL COMMENT '来源 URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_page_view_article_created` (`article_id`, `created_at`),
  KEY `idx_page_view_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面访问明细（90 天清理）';

CREATE TABLE `t_page_view_daily` (
  `article_id` BIGINT NOT NULL COMMENT '逻辑引用 t_article.id（0=首页/非文章页汇总）',
  `lang` VARCHAR(8) NOT NULL COMMENT 'zh / ja / en',
  `stat_date` DATE NOT NULL COMMENT '统计日期（JST）',
  `pv` INT NOT NULL DEFAULT 0 COMMENT '页面浏览量',
  `uv` INT NOT NULL DEFAULT 0 COMMENT '独立访客数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`article_id`, `lang`, `stat_date`),
  KEY `idx_page_view_daily_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面访问日聚合';

CREATE TABLE `t_mail_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增 PK',
  `to_email` VARCHAR(128) NOT NULL COMMENT '收件邮箱',
  `template` VARCHAR(64) NOT NULL COMMENT '模板名 comment_reply / comment_passed 等',
  `subject` VARCHAR(255) NOT NULL COMMENT '邮件标题',
  `status` TINYINT NOT NULL COMMENT '1=SUCCESS 2=FAILED（V2 起点只写 FAILED）',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `error_message` VARCHAR(512) NULL DEFAULT NULL COMMENT '失败原因（脱敏）',
  `provider_message_id` VARCHAR(64) NULL DEFAULT NULL COMMENT 'Resend message id',
  `params_json` VARCHAR(512) NULL DEFAULT NULL COMMENT '模板渲染关键参数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mail_log_status_created` (`status`, `created_at`),
  KEY `idx_mail_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件发送日志（90 天清理，只写失败）';

INSERT INTO `t_site_config` (`id`, `site_title_zh`) VALUES (1, 'MyBlog');
