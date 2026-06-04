# Schema 设计（V2 DDL 草案）

> 本文档回答："V2 的物理表 schema 长什么样？DDL 在哪里？"
> 状态：📝 **DDL 草案（R1–R8 决策合并版）**，已产出并烟测通过 `Flyway V1__init.sql`，待人工评审后冻结。
> 权威源：所有字段、索引、约束追溯到 `product/decisions-draft.md` R1–R8 + `decisions/` ADR-0014/0015/0017/0018。

## 一、横切约束（写 DDL 前必读）

| 约束 | 来源 |
|------|------|
| 时间类型用 `DATETIME`，禁用 `TIMESTAMP` | ADR-0014 / 0015 |
| 字符集统一 `utf8mb4` / 排序 `utf8mb4_0900_ai_ci`，引擎 `InnoDB` | ADR-0018 / overview.md |
| 审计 8 列基线：`id + created_at + created_by + updated_at + updated_by + deleted + deleted_at + deleted_by` | ADR-0015 / decisions-draft.md 审计列章节 |
| **禁建 DB FOREIGN KEY**，只建普通索引 `KEY idx_xxx` / `UNIQUE KEY uk_xxx` | ADR-0017 / pitfalls R-012 |
| 主键策略：业务表声明 `BIGINT NOT NULL`（**不带 AUTO_INCREMENT**），由 MyBatis-Plus `IdType.ASSIGN_ID` 雪花在应用层生成；日志型 / 聚合表例外可 `AUTO_INCREMENT` | ADR-0015 / R7 D4 |
| 软删三件套：`deleted TINYINT(0/1) + deleted_at DATETIME + deleted_by BIGINT` | ADR-0015 |
| 时区：JVM / MySQL session / 应用层统一 `Asia/Tokyo` | ADR-0018 / R7 D11 |
| 审计字段填充：`created_at/by` 与 `updated_at/by` 由 `AuditFieldHandler` 应用层填；DB 仅 `created_at` 给 `DEFAULT CURRENT_TIMESTAMP` 兜底，**不加 ON UPDATE** 避免与 handler 双写 | R7 D4 |
| 命名：表 `t_xxx`，索引 `idx_<table>_<cols>`，唯一键 `uk_<table>_<cols>` | ADR-0014 + H2/MySQL 兼容约束 |

### 审计列例外清单

| 表 | 例外原因 |
|---|---|
| `t_user_info` | 与 `t_user_auth` 1:1，主键即逻辑引用列 `user_id`，无独立 `id`；带 7 列审计（无 `id`） |
| `t_article_tag` | 关联表，靠主表审计，全部 8 列不带 |
| `t_refresh_token` | 不带 `deleted` 三件套；用 `revoked` 表示失效，过期/撤销由 cleanup job 物理删（ADR-0015 §6） |
| `t_page_view` | 高写入 append-only 日志，仅 `id + created_at` + 业务字段；`id` 用 `AUTO_INCREMENT` |
| `t_page_view_daily` | 聚合表，复合 PK `(article_id, lang, stat_date)`，无独立 `id`、无软删 |
| `t_mail_log` | append-only 日志，仅 `id + created_at` + 业务字段；`id` 用 `AUTO_INCREMENT`（ADR-0015 §6 例外） |

---

## 二、表清单（按模块）

| 模块 | 表 | 决策来源 |
|---|---|---|
| `identity` | t_user_auth | R1 #3/#4/#5' + R6 C1 + R8 E1 |
| `identity` | t_user_info | R5 A1 |
| `identity` | t_refresh_token | R6 C1 |
| `content` | t_article | R2 #7a + R3 #6/#8/#8' + R5 A3 + R4 |
| `content` | t_article_tag | R5 B1 |
| `content` | t_category | R2 #7b + R3 #9 |
| `content` | t_tag | R2 #7b + R3 #10 |
| `comment` | t_comment | R1 #1 + R4 #11/#12/#13/#12-P0 |
| `system` | t_site_config | R2 #7c |
| `system` | t_attachment | R5 A3 |
| `system` | t_friend_link | R8 E2 |
| `stats` | t_page_view | R6 C4 |
| `stats` | t_page_view_daily | R6 C4 |
| `common-infra` | t_mail_log | R7 D5 + R8 E3 |

合计 **14 张表**。

---

## 三、各表 DDL

### 3.1 `t_user_auth`（identity）

来源：R1 #3/#4/#5' + R6 C1 + R8 E1

```sql
CREATE TABLE `t_user_auth` (
  `id`                BIGINT       NOT NULL                          COMMENT '主键（ASSIGN_ID 雪花）',
  `username`          VARCHAR(64)  NOT NULL                          COMMENT '用户名',
  `password_hash`     VARCHAR(72)  NOT NULL                          COMMENT 'BCrypt 哈希（60 字符+余量）',
  `type`              TINYINT      NOT NULL                          COMMENT '账号类型 1=ADMIN 2=DEMO 3=GUEST',
  `token_version`     INT          NOT NULL DEFAULT 0                COMMENT 'JWT 撤销版本号',
  `last_login_at`     DATETIME     NULL     DEFAULT NULL             COMMENT '最后登录时间',
  `last_login_ip`     VARCHAR(45)  NULL     DEFAULT NULL             COMMENT '最后登录 IP（IPv6 兼容）',
  `login_fail_count`  INT          NOT NULL DEFAULT 0                COMMENT '连续登录失败次数',
  `locked_until`      DATETIME     NULL     DEFAULT NULL             COMMENT '账号锁定截止时间（NULL=未锁）',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`        BIGINT       NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`        BIGINT       NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`           TINYINT      NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`        DATETIME     NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`        BIGINT       NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_auth_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户认证';
```

### 3.2 `t_user_info`（identity）

来源：R5 A1 — 例外表，无独立 `id`，`user_id` 同时作 PK + 逻辑引用 `t_user_auth.id`

```sql
CREATE TABLE `t_user_info` (
  `user_id`       BIGINT       NOT NULL                          COMMENT 'PK + 逻辑引用 t_user_auth.id',
  `nickname`      VARCHAR(64)  NOT NULL                          COMMENT '展示昵称',
  `avatar_url`    VARCHAR(255) NULL     DEFAULT NULL             COMMENT '头像 URL',
  `bio_zh`        TEXT         NULL                              COMMENT '个人简介（中）',
  `bio_ja`        TEXT         NULL                              COMMENT '个人简介（日）',
  `bio_en`        TEXT         NULL                              COMMENT '个人简介（英）',
  `location`      VARCHAR(64)  NULL     DEFAULT NULL             COMMENT '所在地（如 Tokyo）',
  `website`       VARCHAR(255) NULL     DEFAULT NULL             COMMENT '个人主页',
  `email_public`  VARCHAR(128) NULL     DEFAULT NULL             COMMENT '公开邮箱（可选）',
  `github_url`    VARCHAR(255) NULL     DEFAULT NULL             COMMENT 'GitHub 主页 URL',
  `twitter_url`   VARCHAR(255) NULL     DEFAULT NULL             COMMENT 'Twitter / X 主页 URL',
  `linkedin_url`  VARCHAR(255) NULL     DEFAULT NULL             COMMENT 'LinkedIn 主页 URL',
  `zhihu_url`     VARCHAR(255) NULL     DEFAULT NULL             COMMENT '知乎主页 URL',
  `qiita_url`     VARCHAR(255) NULL     DEFAULT NULL             COMMENT 'Qiita 主页 URL',
  `juejin_url`    VARCHAR(255) NULL     DEFAULT NULL             COMMENT '掘金主页 URL',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`    BIGINT       NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`    BIGINT       NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`       TINYINT      NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`    DATETIME     NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`    BIGINT       NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料（1:1 t_user_auth）';
```

### 3.3 `t_refresh_token`（identity）

来源：R6 C1 — 例外表（不带软删三件套），失效靠 `revoked` 字段，过期/撤销由 `RefreshTokenCleanupJob` 物理删（ADR-0015 §6）

```sql
CREATE TABLE `t_refresh_token` (
  `id`          BIGINT       NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `user_id`     BIGINT       NOT NULL                          COMMENT '逻辑引用 t_user_auth.id',
  `token_hash`  VARCHAR(64)  NOT NULL                          COMMENT 'SHA-256(refresh token)，不存明文',
  `expires_at`  DATETIME     NOT NULL                          COMMENT '过期时间',
  `revoked`     TINYINT      NOT NULL DEFAULT 0                COMMENT '已撤销 0=未 1=是（取代软删）',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_token_hash` (`token_hash`),
  KEY `idx_refresh_token_user_expires` (`user_id`, `expires_at`),
  KEY `idx_refresh_token_expires` (`expires_at`),
  KEY `idx_refresh_token_revoked_created` (`revoked`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Refresh Token（物理删，无软删）';
```

### 3.4 `t_article`（content）

来源：R2 #7a（三语标题/摘要）+ R3 #6/#8/#8'（slug / 5 态 / 字段约束）+ R5 A3（封面）+ R4（评论计数）

```sql
CREATE TABLE `t_article` (
  `id`                    BIGINT        NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `title_zh`              VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '标题（中）',
  `title_ja`              VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '标题（日）',
  `title_en`              VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '标题（英）',
  `summary_zh`            VARCHAR(500)  NULL     DEFAULT NULL             COMMENT '摘要（中）',
  `summary_ja`            VARCHAR(500)  NULL     DEFAULT NULL             COMMENT '摘要（日）',
  `summary_en`            VARCHAR(500)  NULL     DEFAULT NULL             COMMENT '摘要（英）',
  `body`                  MEDIUMTEXT    NULL                              COMMENT '正文 Markdown（单中文）',
  `category_id`           BIGINT        NULL     DEFAULT NULL             COMMENT '逻辑引用 t_category.id（DRAFT 可空，其他状态必填）',
  `author_id`             BIGINT        NOT NULL                          COMMENT '逻辑引用 t_user_auth.id',
  `slug`                  VARCHAR(160)  NULL     DEFAULT NULL             COMMENT 'URL 别名 a-z 0-9 - ；不强制唯一',
  `status`                TINYINT       NOT NULL                          COMMENT '1=DRAFT 2=PUBLISHED 3=PRIVATE 4=PASSWORD 5=SCHEDULED',
  `access_password`       VARCHAR(255)  NULL     DEFAULT NULL             COMMENT 'PASSWORD 状态用，BCrypt 哈希',
  `publish_at`            DATETIME      NULL     DEFAULT NULL             COMMENT 'SCHEDULED 状态用',
  `cover_attachment_id`   BIGINT        NULL     DEFAULT NULL             COMMENT '逻辑引用 t_attachment.id',
  `comment_count`         INT           NOT NULL DEFAULT 0                COMMENT '冗余：target_type=ARTICLE AND target_id=本文 AND deleted=0 AND audit_status=PASS 的评论数',
  `created_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`            BIGINT        NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`            BIGINT        NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`               TINYINT       NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`            DATETIME      NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`            BIGINT        NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  KEY `idx_article_status_deleted_publish` (`status`, `deleted`, `publish_at`),
  KEY `idx_article_category_deleted` (`category_id`, `deleted`),
  KEY `idx_article_author` (`author_id`),
  KEY `idx_article_slug` (`slug`),
  KEY `idx_article_cover` (`cover_attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章';
```

### 3.5 `t_article_tag`（content）

来源：R5 B1 — 关联表，复合 PK，不带审计

```sql
CREATE TABLE `t_article_tag` (
  `article_id`  BIGINT  NOT NULL  COMMENT '逻辑引用 t_article.id',
  `tag_id`      BIGINT  NOT NULL  COMMENT '逻辑引用 t_tag.id',
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_article_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章-标签关联';
```

### 3.6 `t_category`（content）

来源：R2 #7b + R3 #9

```sql
CREATE TABLE `t_category` (
  `id`          BIGINT       NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `name_zh`     VARCHAR(64)  NOT NULL                          COMMENT '名称（中）',
  `name_ja`     VARCHAR(64)  NULL     DEFAULT NULL             COMMENT '名称（日，fallback 到 zh）',
  `name_en`     VARCHAR(64)  NULL     DEFAULT NULL             COMMENT '名称（英，fallback 到 zh）',
  `slug`        VARCHAR(64)  NOT NULL                          COMMENT 'URL 别名（UNIQUE）',
  `sort_order`  INT          NOT NULL DEFAULT 0                COMMENT '排序',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`     TINYINT      NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`  DATETIME     NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`),
  KEY `idx_category_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类（平铺）';
```

### 3.7 `t_tag`（content）

来源：R2 #7b + R3 #10

```sql
CREATE TABLE `t_tag` (
  `id`          BIGINT       NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `name_zh`     VARCHAR(64)  NOT NULL                          COMMENT '名称（中）',
  `name_ja`     VARCHAR(64)  NULL     DEFAULT NULL             COMMENT '名称（日，fallback 到 zh）',
  `name_en`     VARCHAR(64)  NULL     DEFAULT NULL             COMMENT '名称（英，fallback 到 zh）',
  `slug`        VARCHAR(64)  NOT NULL                          COMMENT 'URL 别名（UNIQUE）',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`     TINYINT      NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`  DATETIME     NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`  BIGINT       NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签（白名单）';
```

### 3.8 `t_comment`（comment）

来源：R1 #1 + R4 #11/#12/#13/#12-P0 + feature-inventory ④ 留言板复用

> **多用途承载**：评论实体同时承载"文章评论"与"留言板"（未来可扩展"关于页评论"等）。用 `target_type + target_id` 二元定位，避免 `article_id=0` 这类 magic value。

```sql
CREATE TABLE `t_comment` (
  `id`                    BIGINT       NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `target_type`           TINYINT      NOT NULL                          COMMENT '1=ARTICLE 2=GUESTBOOK（未来可扩展 3=ABOUT 等）',
  `target_id`             BIGINT       NOT NULL                          COMMENT 'ARTICLE 时是 t_article.id；GUESTBOOK 时固定 0',
  `parent_id`             BIGINT       NULL     DEFAULT NULL             COMMENT '顶层评论 id（挂楼用，顶层评论自身为 NULL）',
  `reply_to_comment_id`   BIGINT       NULL     DEFAULT NULL             COMMENT '本次实际回复的目标评论 id（通知用）',
  `reply_to_user_id`      BIGINT       NULL     DEFAULT NULL             COMMENT '被回复者为系统用户时的 user_id',
  `reply_to_nickname`     VARCHAR(64)  NULL     DEFAULT NULL             COMMENT '被回复者昵称快照',
  `author_user_id`        BIGINT       NULL     DEFAULT NULL             COMMENT '管理员评论时挂 t_user_auth.id；游客 NULL',
  `author_nickname`       VARCHAR(64)  NOT NULL                          COMMENT '昵称',
  `author_email`          VARCHAR(128) NOT NULL                          COMMENT '邮箱（不公开展示）',
  `author_site`           VARCHAR(255) NULL     DEFAULT NULL             COMMENT '个人站（http/https 白名单）',
  `author_ip`             VARCHAR(45)  NULL     DEFAULT NULL             COMMENT '审计：IP',
  `author_user_agent`     VARCHAR(512) NULL     DEFAULT NULL             COMMENT '审计：UA',
  `content_md`            TEXT         NOT NULL                          COMMENT '用户提交原文（Markdown，≤5000）',
  `content_html`          TEXT         NOT NULL                          COMMENT '清洗后安全 HTML（前台只渲染这个）',
  `audit_status`          TINYINT      NOT NULL                          COMMENT '1=PASS 2=PENDING 3=HIDDEN',
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`            BIGINT       NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`            BIGINT       NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`               TINYINT      NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`            DATETIME     NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`            BIGINT       NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  KEY `idx_comment_target_deleted_audit_created` (`target_type`, `target_id`, `deleted`, `audit_status`, `created_at`),
  KEY `idx_comment_parent` (`parent_id`),
  KEY `idx_comment_reply_to` (`reply_to_comment_id`),
  KEY `idx_comment_author_user` (`author_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论 / 留言（多用途）';
```

**application 层约束补充**：
- 跨目标回复禁止：`reply_to_comment_id` 所属 `(target_type, target_id)` 必须等于当前评论的 `(target_type, target_id)`
- ARTICLE 类型：`target_id` 必须命中 `t_article.id` 且文章状态允许评论
- GUESTBOOK 类型：`target_id` 固定 `0`，application 层强制写入

### 3.9 `t_site_config`（system）

来源：R2 #7c — 宽表，单行 `id=1`

```sql
CREATE TABLE `t_site_config` (
  `id`                    BIGINT        NOT NULL                          COMMENT '固定为 1',
  `site_title_zh`         VARCHAR(128)  NOT NULL                          COMMENT '站点标题（中）',
  `site_title_ja`         VARCHAR(128)  NULL     DEFAULT NULL             COMMENT '站点标题（日）',
  `site_title_en`         VARCHAR(128)  NULL     DEFAULT NULL             COMMENT '站点标题（英）',
  `site_subtitle_zh`      VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '副标题（中）',
  `site_subtitle_ja`      VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '副标题（日）',
  `site_subtitle_en`      VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '副标题（英）',
  `about_md_zh`           MEDIUMTEXT    NULL                              COMMENT '关于我（中）',
  `about_md_ja`           MEDIUMTEXT    NULL                              COMMENT '关于我（日）',
  `about_md_en`           MEDIUMTEXT    NULL                              COMMENT '关于我（英）',
  `logo_url`              VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '站点 Logo URL',
  `favicon_url`           VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '站点 favicon URL',
  `icp_no`                VARCHAR(64)   NULL     DEFAULT NULL             COMMENT 'ICP 备案号（中国大陆部署用）',
  `spotify_playlist_id`   VARCHAR(64)   NULL     DEFAULT NULL             COMMENT 'Spotify 播放列表 ID（替代 V1 音乐播放器）',
  `created_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`            BIGINT        NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`            BIGINT        NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`               TINYINT       NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`            DATETIME      NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`            BIGINT        NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站点配置（宽表，单行）';
```

### 3.10 `t_attachment`（system）

来源：R5 A3

```sql
CREATE TABLE `t_attachment` (
  `id`                  BIGINT        NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `storage_type`        VARCHAR(16)   NOT NULL                          COMMENT 'LOCAL / S3 / OSS',
  `bucket`              VARCHAR(64)   NULL     DEFAULT NULL             COMMENT 'LOCAL 时为本地根目录别名',
  `object_key`          VARCHAR(255)  NOT NULL                          COMMENT '存储后端的对象键 / 相对路径',
  `public_url`          VARCHAR(512)  NOT NULL                          COMMENT '对外可访问 URL',
  `content_type`        VARCHAR(64)   NOT NULL                          COMMENT 'MIME 类型',
  `file_size`           BIGINT        NOT NULL                          COMMENT '字节',
  `width`               INT           NULL     DEFAULT NULL             COMMENT '仅图片',
  `height`              INT           NULL     DEFAULT NULL             COMMENT '仅图片',
  `original_filename`   VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '原始文件名',
  `hash_sha256`         VARCHAR(64)   NULL     DEFAULT NULL             COMMENT 'SHA-256，用于去重',
  `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`          BIGINT        NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`          BIGINT        NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`             TINYINT       NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`          DATETIME      NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`          BIGINT        NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attachment_hash` (`hash_sha256`),
  KEY `idx_attachment_storage_key` (`storage_type`, `object_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件';
```

> 上传去重按 `hash_sha256` 全局查找，不按 `deleted` 过滤；若命中软删记录则恢复该行，避免软删保留期内重复上传触发 `uk_hash` 冲突。

### 3.11 `t_friend_link`（system）

来源：R8 E2

```sql
CREATE TABLE `t_friend_link` (
  `id`           BIGINT        NOT NULL                          COMMENT '主键（ASSIGN_ID）',
  `name`         VARCHAR(64)   NOT NULL                          COMMENT '友链名称',
  `url`          VARCHAR(255)  NOT NULL                          COMMENT '站点地址',
  `avatar_url`   VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '头像 / logo',
  `description`  VARCHAR(255)  NULL     DEFAULT NULL             COMMENT '一句话介绍（单中文）',
  `sort_order`   INT           NOT NULL DEFAULT 0                COMMENT '排序',
  `status`       TINYINT       NOT NULL DEFAULT 1                COMMENT '1=显示 2=隐藏',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by`   BIGINT        NULL     DEFAULT NULL             COMMENT '创建者 user_id',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by`   BIGINT        NULL     DEFAULT NULL             COMMENT '最后修改者 user_id',
  `deleted`      TINYINT       NOT NULL DEFAULT 0                COMMENT '软删标记 0=正常 1=已删',
  `deleted_at`   DATETIME      NULL     DEFAULT NULL             COMMENT '删除时间',
  `deleted_by`   BIGINT        NULL     DEFAULT NULL             COMMENT '删除者 user_id',
  PRIMARY KEY (`id`),
  KEY `idx_friend_link_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='友链';
```

### 3.12 `t_page_view`（stats）

来源：R6 C4 — 例外表（append-only 日志），`AUTO_INCREMENT` 自增，不带审计

```sql
CREATE TABLE `t_page_view` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT          COMMENT '自增 PK',
  `article_id`     BIGINT        NULL     DEFAULT NULL             COMMENT '逻辑引用 t_article.id（NULL=非文章页）',
  `lang`           VARCHAR(8)    NOT NULL                          COMMENT 'zh / ja / en',
  `visitor_hash`   CHAR(64)      NOT NULL                          COMMENT 'SHA-256(ip + ua)，用于 UV 去重',
  `referrer`       VARCHAR(512)  NULL     DEFAULT NULL             COMMENT '来源 URL',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_page_view_article_created` (`article_id`, `created_at`),
  KEY `idx_page_view_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面访问明细（90 天清理）';
```

### 3.13 `t_page_view_daily`（stats）

来源：R6 C4 — 聚合表，复合 PK，无软删

```sql
CREATE TABLE `t_page_view_daily` (
  `article_id`  BIGINT       NOT NULL                          COMMENT '逻辑引用 t_article.id（0=首页/非文章页汇总）',
  `lang`        VARCHAR(8)   NOT NULL                          COMMENT 'zh / ja / en',
  `stat_date`   DATE         NOT NULL                          COMMENT '统计日期（JST）',
  `pv`          INT          NOT NULL DEFAULT 0                COMMENT '页面浏览量',
  `uv`          INT          NOT NULL DEFAULT 0                COMMENT '独立访客数',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`article_id`, `lang`, `stat_date`),
  KEY `idx_page_view_daily_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面访问日聚合';
```

### 3.14 `t_mail_log`（common-infra）

来源：R7 D5 + R8 E3 — 例外表（append-only 日志），只写失败

```sql
CREATE TABLE `t_mail_log` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT          COMMENT '自增 PK',
  `to_email`             VARCHAR(128)  NOT NULL                          COMMENT '收件邮箱',
  `template`             VARCHAR(64)   NOT NULL                          COMMENT '模板名 comment_reply / comment_passed 等',
  `subject`              VARCHAR(255)  NOT NULL                          COMMENT '邮件标题',
  `status`               TINYINT       NOT NULL                          COMMENT '1=SUCCESS 2=FAILED（V2 起点只写 FAILED）',
  `retry_count`          INT           NOT NULL DEFAULT 0                COMMENT '重试次数',
  `error_message`        VARCHAR(512)  NULL     DEFAULT NULL             COMMENT '失败原因（脱敏）',
  `provider_message_id`  VARCHAR(64)   NULL     DEFAULT NULL             COMMENT 'Resend message id',
  `params_json`          VARCHAR(512)  NULL     DEFAULT NULL             COMMENT '模板渲染关键参数',
  `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mail_log_status_created` (`status`, `created_at`),
  KEY `idx_mail_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮件发送日志（90 天清理，只写失败）';
```

---

## 四、初始化数据（V1__init.sql 同时插入）

```sql
-- 站点配置单行
INSERT INTO `t_site_config` (`id`, `site_title_zh`) VALUES (1, 'MyBlog');

-- 管理员账号（密码占位，部署时通过运维 SQL 改 BCrypt 哈希）
-- type=1 ADMIN
-- token_version=0
-- 注意：实际部署前必须改 password_hash
```

> 管理员账号建议**不在 Flyway 中写入**，改由部署文档要求"首次部署后通过运维 SQL 手工插入"，避免初始密码哈希提交进版本库。

---

## 五、变更流程

1. 本文档定稿 → 已生成 `MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql`
2. **DDL 冻结里程碑达成后**：本文档与 `V1__init.sql` 锁定，之后任何 schema 变更走 `V2__xxx.sql` / `V3__xxx.sql`，不改 V1
3. 进入业务代码阶段：清理 V2 现有业务层 → 按本 schema 重建 Entity / Mapper / Controller

---

## 六、相关文档

- 决策草案：`product/decisions-draft.md` R1–R8
- ADR：`decisions/0014` / `0015` / `0017` / `0018`
- 持久化策略：`arch/persistence-strategy.md`
- 路线：`roadmap.md` S3 / M1
- 红线：`pitfalls.md` R-011 / R-012
