# 数据库 Schema

> 状态：当前有效
> 适用范围：MyBlog V2 MySQL Schema
> 最后校准：2026-07-16
> 对应代码：`MyBlog-springboot-v2/src/main/resources/db/migration/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：Schema 权威说明

## 权威来源

Flyway 迁移是物理 Schema 的唯一事实源。本文档只记录表职责、关键关系和不可破坏的不变量，不复制完整 DDL。

| 迁移 | 内容 |
| --- | --- |
| `V1__init.sql` | 14 张基础表和 `t_site_config` 初始行 |
| `V2__backfill_user_info.sql` | 为已有活动账号补齐 `t_user_info` |
| `V3__add_article_homepage_slot.sql` | 新增 `t_article.homepage_slot` 与组合索引 |
| `V4__add_site_config_started_date.sql` | 新增 `t_site_config.started_date` |
| `V5__add_homepage_slot_guard.sql` | 新增首页槽位并发 guard 表及 PINNED/FEATURED 固定行 |

## 表目录

| 模块 | 表 | 职责 |
| --- | --- | --- |
| identity | `t_user_auth` | 登录凭据、账号类型、失败锁定、token version |
| identity | `t_user_info` | 1:1 资料、三语简介和社交链接 |
| identity | `t_refresh_token` | refresh token hash、过期时间和撤销状态 |
| content | `t_article` | 三语标题摘要、中文正文、五态状态、首页槽位、封面和评论计数 |
| content | `t_category` | 平铺分类、三语名称、唯一 slug 和排序 |
| content | `t_tag` | 标签、三语名称和唯一 slug |
| content | `t_article_tag` | 文章与标签的复合主键关联 |
| content | `t_homepage_slot_guard` | PINNED/FEATURED 写入串行锁，不保存业务内容 |
| comment | `t_comment` | 文章评论、留言板、两层回复、审核和作者快照 |
| system | `t_site_config` | 固定 `id=1` 的站点配置、三语 About 和建站日期 |
| system | `t_attachment` | LOCAL/S3 对象、公开 URL、图片尺寸和 SHA-256 去重 |
| system | `t_friend_link` | 友链、显示状态和排序 |
| stats | `t_page_view` | 页面访问明细 |
| stats | `t_page_view_daily` | 按文章、语言、JST 日期聚合的 PV/UV |
| common | `t_mail_log` | 邮件发送失败日志 |

## 通用约束

1. 数据库使用 MySQL 8、`utf8mb4`、`utf8mb4_0900_ai_ci`。
2. 业务表以 `t_` 开头，列和索引使用 snake_case。
3. 标准业务主键为 `BIGINT`，由 MyBatis-Plus `ASSIGN_ID` 生成。
4. 业务时间使用 `DATETIME`，应用按 Asia/Tokyo 解释。
5. 数据库不使用 `FOREIGN KEY`；应用层校验引用完整性，引用列建立普通索引。
6. 已应用迁移不可改写；变更新增 `V{n}__description.sql`。

## 审计与软删除

标准业务实体包含：`id`、`created_at/by`、`updated_at/by`、`deleted`、`deleted_at/by`。软删除操作必须同时维护删除标记、时间和操作者。

显式例外：

- `t_user_info`：`user_id` 同时作为主键和逻辑引用，不含独立 `id`。
- `t_article_tag`：只保留复合主键和标签反向索引。
- `t_refresh_token`：使用 `revoked`，不使用软删除三件套。
- `t_page_view`、`t_mail_log`：append-only，自增主键和创建时间。
- `t_page_view_daily`：复合主键，无独立 `id` 和软删除。

## 关键不变量

- `t_user_auth.username` 与 `t_refresh_token.token_hash` 全局唯一。
- `t_category.slug` 与 `t_tag.slug` 各自唯一；文章 slug 不唯一。
- 文章状态为 `DRAFT / PUBLISHED / PRIVATE / PASSWORD / SCHEDULED`，首页槽位为 `NONE / PINNED / FEATURED`。
- `t_homepage_slot_guard` 固定保留 `PINNED / FEATURED` 两行；槽位计数校验前必须锁定对应行。
- `t_comment.target_type + target_id` 区分文章评论与留言板；留言板 `target_id=0`。
- `t_site_config` 只有 `id=1` 一行有效配置。
- `t_attachment.hash_sha256` 全局唯一；重复上传命中软删除记录时恢复原记录。
- `t_page_view_daily` 主键为 `(article_id, lang, stat_date)`；非文章页聚合使用 `article_id=0`。
- `t_mail_log` 当前只写失败记录。

## 变更检查

新增迁移时必须同步检查 Entity、Mapper/XML、Repository、H2 迁移测试、MySQL 迁移测试、本文档和相关 ADR。新增表流程见 `../workflows/add-new-table.md`。
