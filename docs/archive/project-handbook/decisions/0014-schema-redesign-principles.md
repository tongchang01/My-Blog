# ADR-0014: V2 数据库 schema 重设计原则

- 状态：accepted（**部分被 ADR-0015 / ADR-0018 超越**）
- 日期：2026-06
- 决策者：项目负责人
- 依赖：ADR-0013（V2 不再兼容 V1 数据结构）

> ⚠️ **2026-06 更新**：以下章节已被后续 ADR 超越，请优先参考新 ADR：
> - §2 主键（`BIGINT UNSIGNED AUTO_INCREMENT`）→ **ADR-0015 改为 `BIGINT NOT NULL`（不带 AUTO_INCREMENT）+ MyBatis-Plus `IdType.ASSIGN_ID`（雪花）在应用层生成**；日志型例外表（t_page_view / t_page_view_daily）可保留 AUTO_INCREMENT
> - §3 字段类型规范（时间类型 TIMESTAMP）→ **ADR-0015 / ADR-0018 改为 DATETIME**
> - §4 时间字段统一（`created_at` / `updated_at` only）→ **ADR-0015 改为 8 列审计基线（含 `created_by` / `updated_by` / `deleted_*`）**
> - §5 软删除统一（单列 `deleted_at TIMESTAMP NULL`）→ **ADR-0015 改为三件套 `deleted TINYINT + deleted_at DATETIME + deleted_by BIGINT`，配合 MyBatis-Plus `@TableLogic`**
> - §6 字段命名（`is_xxx` 布尔）→ 仍有效，但状态枚举字段命名见 R3 / R4
> - "例：标准业务表模板"中的 `t_article` 示例 → **已过时**，三语字段 / slug / cover_attachment_id / status 5 态 / 8 列审计 / DATETIME / ASSIGN_ID 等详见 `product/decisions-draft.md` R2-R3 与 `arch/schema-design.md`
>
> 本 ADR 的"表命名 / 索引规范 / 字符集 / COMMENT / 关联表"原则仍有效。

## 背景

ADR-0013 确定 V2 重新设计 schema。具体怎么设计需要一套统一原则，避免新表又陷入 V1 的混乱。

## 决定

V2 全部新表遵循以下原则：

### 1. 表命名

- 前缀 `t_`，下划线分隔，全小写
- 表名用业务单数名词：`t_article`、`t_comment`、`t_tag`（不要 `t_articles` 复数）
- 关联表用 `t_{a}_{b}` 顺序按依赖关系：`t_article_tag`
- **不**沿用 V1 原作者的命名遗留（如 `t_aurora_*` 之类前缀）

### 2. 主键

- 默认 `BIGINT NOT NULL`（**不带** `AUTO_INCREMENT`，**已被 ADR-0015 超越**）
- id 由 MyBatis-Plus `IdType.ASSIGN_ID`（雪花算法）在应用层生成
- 字段名统一 `id`
- 例外：日志 / 高写入 append-only 表（如 `t_page_view` / `t_page_view_daily`）可保留 DB AUTO_INCREMENT

### 3. 字段类型规范

| 业务含义 | 类型 |
|----------|------|
| 布尔（是否） | `BOOLEAN`（MySQL 实际是 TINYINT(1)，但语义清晰）|
| 状态枚举 | `TINYINT UNSIGNED`，配合 `COMMENT` 列出全部取值 |
| 短字符串 | `VARCHAR(N)`，N 按业务上限给 |
| 长文本 | `TEXT` / `MEDIUMTEXT`（文章正文用 MEDIUMTEXT） |
| 创建时间 | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| 更新时间 | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
| 金额 | `DECIMAL(precision, scale)`（不用 FLOAT/DOUBLE） |
| IP 地址 | `VARCHAR(45)`（兼容 IPv6） |
| URL | `VARCHAR(512)` 起 |

### 4. 时间字段统一

每张业务表必带：
```sql
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
```

字段名 `created_at` / `updated_at`（snake_case + 后缀 _at），不用 V1 的 `create_time` / `update_time`。

### 5. 软删除统一

- 需要软删除的表：加 `deleted_at TIMESTAMP NULL DEFAULT NULL`
- 非 null 表示已删除
- **不**用 V1 的 `is_delete TINYINT(1)`（信息量小，无法知道删除时间）
- 不需要软删除的表（如关联表、日志表）不加该字段

### 6. 字段命名

- snake_case，全小写
- 布尔字段语义化：`is_pinned`、`is_featured`（不要 `top` `featured`）
- 时间字段后缀 `_at`：`published_at`、`reviewed_at`
- 外键 `{table_singular}_id`：`article_id`、`user_id`
- 不缩写：`description` 不要 `desc`（且 desc 是 SQL 保留字）

### 7. 索引规范

- 单表索引数控制在合理范围（≤5）
- 联合索引按"高选择度在前"
- 外键字段必加索引
- 命名：`idx_{table}_{cols}`，唯一索引 `uk_{table}_{cols}`

### 8. 字符集

- 统一 `utf8mb4` + `utf8mb4_0900_ai_ci`（MySQL 8）
- 不用 `utf8`（MySQL 旧的 utf8 是 3 字节，存不下 emoji）

### 9. 必带列 COMMENT

每张表、每个字段都必须有中文 COMMENT 说明业务用途；状态字段必须列出所有取值含义。

### 10. 关联表设计

- 用复合主键或独立 id + 唯一索引
- 业务模块归属：与"主"实体同模块（如 `t_article_tag` 归 content 模块）

## 例：标准业务表模板（**已过时，仅作历史参考**）

> 此例保留 V1 习惯的 AUTO_INCREMENT / TIMESTAMP / 单列软删，已被 ADR-0015 / ADR-0018 全面替换。新表请按 `arch/schema-design.md` + ADR-0015 的 8 列基线撰写。

```sql
-- 已过时示例（请勿照抄）
CREATE TABLE t_article (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT UNSIGNED NOT NULL                COMMENT '作者 id',
    category_id     BIGINT UNSIGNED NULL                    COMMENT '分类 id',
    title           VARCHAR(100)    NOT NULL                COMMENT '标题',
    cover_url       VARCHAR(512)    NULL                    COMMENT '封面 URL',
    summary         VARCHAR(500)    NULL                    COMMENT '摘要',
    content         MEDIUMTEXT      NOT NULL                COMMENT '正文（Markdown）',
    status          TINYINT UNSIGNED NOT NULL DEFAULT 1     COMMENT '状态 1 公开 2 私密 3 草稿',
    is_pinned       BOOLEAN         NOT NULL DEFAULT FALSE  COMMENT '是否置顶',
    is_featured     BOOLEAN         NOT NULL DEFAULT FALSE  COMMENT '是否推荐',
    access_password VARCHAR(255)    NULL                    COMMENT '访问密码（BCrypt）',
    published_at    TIMESTAMP       NULL                    COMMENT '发布时间',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP       NULL DEFAULT NULL       COMMENT '软删除时间，非空即已删除',
    PRIMARY KEY (id),
    KEY idx_article_user_id (user_id),
    KEY idx_article_category_id (category_id),
    KEY idx_article_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章表';
```

## 后果

正面：
- 整库风格统一
- 字段类型不再有歧义（BOOLEAN 表布尔、TIMESTAMP 表时间）
- 软删除带时间戳，可追溯
- 字符集对 emoji / 多语言友好

负面：
- Flyway 脚本需要重写 V1 表（V2 旧 Entity 也要同步重写）
- 与 V1 行为对照时需要心算字段映射（一次性成本）

## 相关

- 依赖：ADR-0013
- 后续 arch：`arch/schema-design.md`（按本原则设计的全部新表）
- 修改 workflows：`workflows/add-new-table.md` 已更新引用本 ADR
- 修改 rules：`rules/comment-style.md` 已弱化旧库兼容要求
