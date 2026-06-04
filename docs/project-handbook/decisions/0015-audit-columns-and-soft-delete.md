# ADR-0015: 审计列规范与软删三件套

- 状态：accepted
- 日期：2026-06
- 决策者：项目负责人
- 依赖：ADR-0014（schema 重设计原则）
- supersede：ADR-0014 §2（主键 AUTO_INCREMENT）/ §3（时间类型）/ §4（时间字段统一）/ §5（软删除统一）/ §6（字段命名 deleted 部分）

## 背景

ADR-0014 给出了 schema 重设计的总原则，但在以下几点早期决定不够精细，随着 R5-R7 业务/框架决定推进暴露出冲突：

1. 时间列用 `TIMESTAMP` 还是 `DATETIME`：`TIMESTAMP` 会被 session 时区影响存储值，与 R7 D11 "全站 Asia/Tokyo 统一" 配合不稳；`DATETIME` 不携带时区语义，存什么读什么，与"应用层强制 Asia/Tokyo"模型更契合。
2. 软删除单列 `deleted_at TIMESTAMP NULL`：信息够但**与 MyBatis-Plus `@TableLogic` 不直接兼容**（`@TableLogic` 默认匹配 `value/delval` 两个具体值的字段）。R7 D4 明确选 `@TableLogic(value="0", delval="1")` + `deleted TINYINT`。
3. 缺少 `created_by / updated_by / deleted_by`：审计场景下"谁干的"和"何时干的"同样重要；R5 #5 决定补齐。

## 决定

### 1. 时间类型：DATETIME

所有时间相关列统一用 `DATETIME`，**不用** `TIMESTAMP`。

理由：
- `DATETIME` 存什么读什么，不参与 session 时区转换
- 配合 JVM `-Duser.timezone=Asia/Tokyo` + MySQL `serverTimezone=Asia/Tokyo`（详见 ADR-0018），全链路一致
- `TIMESTAMP` 的 2038 上限对长生命周期数据是隐患（虽对当前博客无实际影响，但 DATETIME 无此问题）

### 2. 审计列 8 列基线

> **主键策略（supersede ADR-0014 §2）**：DB 列声明 `BIGINT NOT NULL`，**不带** `AUTO_INCREMENT`；id 由 MyBatis-Plus `IdType.ASSIGN_ID`（雪花）在应用层生成。日志型例外（`t_page_view` / `t_mail_log`）可保留 DB `AUTO_INCREMENT`，`t_page_view_daily` 是复合 PK 例外，见 §6。

所有"业务实体表"必带以下 8 列：

| 列名 | 类型 | NULL | 默认 | 含义 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | （无默认，MyBatis-Plus `IdType.ASSIGN_ID` 雪花生成；日志型例外见 §6） | 主键 |
| `created_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| `created_by` | BIGINT | NULL | NULL | 创建者 `t_user_auth.id`（游客 NULL） |
| `updated_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 最后修改时间 |
| `updated_by` | BIGINT | NULL | NULL | 最后修改者 |
| `deleted` | TINYINT | NOT NULL | 0 | 软删标记（0=正常 1=已删） |
| `deleted_at` | DATETIME | NULL | NULL | 删除时间 |
| `deleted_by` | BIGINT | NULL | NULL | 删除者 |

### 3. 软删三件套

- `deleted TINYINT NOT NULL DEFAULT 0` —— 与 `@TableLogic(value="0", delval="1")` 配对
- `deleted_at DATETIME NULL` —— 删除时刻
- `deleted_by BIGINT NULL` —— 删除人

软删时三列**一起更新**（service 层显式设置 `deleted_at / deleted_by`，不靠 `MetaObjectHandler` 自动填，避免误触发）。

### 4. 落地

- `BaseEntity` 抽象基类承载 8 列；业务实体继承
- `AuditFieldHandler implements MetaObjectHandler` 自动填 `created_at/by` + `updated_at/by`
- `current_user_id` 从 SecurityContext 取（无登录态时填 NULL）
- 高频查询表（`t_article`、`t_comment`）的 `deleted` 列建索引

### 5. 语义说明

- `created_by` / `updated_by` **只记录系统用户**（`t_user_auth.id`），不记录游客身份
- 游客评论时 `created_by = NULL` 是正常情况，不是审计缺失
- 游客身份信息存在评论表自己的 `author_nickname / author_email / author_ip / author_user_agent` 字段里
- 系统任务（如 SCHEDULED 发布、清理任务）触发的更新统一 `updated_by = NULL`

### 6. 例外表（不带 8 列或只带部分）

> **读法说明**：下表"不带"列描述"标准 8 列中**哪些被省略**"。"全部 8 列中除 X 外都不带" = 仅保留 X，其他 7 列均省略。

| 表 | 不带 | 理由 |
|---|---|---|
| `t_user_info` | 独立 `id`（用 `user_id` 同时作 PK + 逻辑引用 t_user_auth.id） | 与 `t_user_auth` 1:1 强绑定（不建 DB FOREIGN KEY，ADR-0017）。继承 `AuditOnlyBase`（7 列：去掉 id） |
| `t_article_tag`（关联表） | 全部 8 列均不带 | 中间表，靠主表审计；仅 `(article_id, tag_id)` 复合 PK |
| `t_refresh_token` | `deleted` 三件套（`deleted` / `deleted_at` / `deleted_by`） | 用 `revoked` 字段表示失效，过期+撤销由清理任务物理删 |
| `t_page_view`（明细 append-only） | 8 列中除 `id` 与 `created_at` 外均不带；**`id` 用 DB `AUTO_INCREMENT`**（非雪花） | 高写入量日志，仅 `created_at` + 业务字段 |
| `t_page_view_daily`（聚合） | 独立 `id` + `updated_at` + `updated_by` + 软删三件套均不带；仅 `created_at` + 业务字段 | 复合 PK `(article_id, lang, stat_date)`，无独立 `id`、无软删 |
| `t_mail_log`（append-only 日志） | 8 列中除 `id` 与 `created_at` 外均不带；**`id` 用 DB `AUTO_INCREMENT`**（非雪花） | 邮件发送失败日志，仅 `created_at` + 业务字段；雪花 id 对永不被引用的日志表是过度设计（R8 E3） |

例外表手动定义实体，不继承 `BaseEntity`。

## 理由

- 时间类型选 DATETIME 与 R7 D11 "全站 Asia/Tokyo" 五层时区模型一致
- 软删三件套支持 MyBatis-Plus `@TableLogic` + 保留"何时删 / 谁删"信息
- 8 列基线一次性写够，避免后续加列改全部业务表
- 例外清单显式枚举，新人不会误踩

## 后果

正面：
- 审计信息完整，回溯无障碍
- 与 MyBatis-Plus 自动填充 + `@TableLogic` 无缝配合
- 时区行为可预期

负面：
- 比单列 `deleted_at` 占多两个 BIGINT；个人博客量级可忽略
- DATETIME 不参与时区转换，意味着多时区场景需应用层手动处理；V2 单时区无此需求

## 相关

- 依赖：ADR-0014（被本 ADR 部分超越）
- 依赖：ADR-0018（时区统一 Asia/Tokyo）
- 关联 rules：`rules/security-baseline.md`、`product/decisions-draft.md` 审计列规范
- 关联 pitfalls：R-011（不得直接 `LocalDateTime.now()`）
