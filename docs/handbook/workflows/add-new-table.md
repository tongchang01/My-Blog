# 新增数据库表（SOP）

> 目标：新增一张业务表，从 Flyway 脚本到代码全链路落地。
> 适用范围：V2 全量重设计后的新表。
> 相关 ADR：0014（schema 重设计原则）/ 0015（审计列 + 软删三件套，supersede 0014 §2-§5）/ 0017（不建 DB FOREIGN KEY）/ 0018（Asia/Tokyo 时区）

## 0. 前置门禁（重要）

按 `status.md` 当前主线，**DDL 冻结（roadmap S3）之前不允许新增业务表**。本 SOP 是 DDL 冻结后、按 R5 模块清单重建业务层时的操作手册。

## 1. 前置确认

- 表归属哪个模块？（identity / content / comment / system / stats / common-infra，见 `../architecture/module-map.md`）
- 表是否落在已锁定的表清单内？若是新增项，先登记 `../start-here/open-issues.md`，再回写 `../architecture/schema-design.md`
- 该表是 8 列基线业务表，还是 ADR-0015 §6 的例外表？

## 2. 步骤

### 步骤 1：写 Flyway 迁移脚本

位置：`src/main/resources/db/migration/V{version}__add_t_xxx.sql`

**标准业务表模板（8 列审计基线）**：

```sql
CREATE TABLE t_xxx_yyy (
    id              BIGINT      NOT NULL                COMMENT '主键（应用层 MyBatis-Plus ASSIGN_ID 雪花生成）',
    article_id      BIGINT      NOT NULL                COMMENT '所属文章 id（逻辑引用 t_article.id，不建 DB FOREIGN KEY）',
    content         VARCHAR(500) NOT NULL               COMMENT '内容',
    -- 审计 8 列
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    created_by      BIGINT      NULL                                                     COMMENT '创建者 t_user_auth.id（游客 NULL）',
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '最后修改时间',
    updated_by      BIGINT      NULL                                                     COMMENT '最后修改者',
    deleted         TINYINT     NOT NULL DEFAULT 0                                       COMMENT '软删标记（0=正常 1=已删，配 @TableLogic）',
    deleted_at      DATETIME    NULL                                                     COMMENT '删除时间',
    deleted_by      BIGINT      NULL                                                     COMMENT '删除者',
    PRIMARY KEY (id),
    KEY idx_xxx_yyy_article_id (article_id),
    KEY idx_xxx_yyy_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='示例业务表';
```

**硬性约束**：

- 🔴 **不**写 `AUTO_INCREMENT`（id 由应用层 ASSIGN_ID 生成）；唯一例外：日志型 append-only 表 `t_page_view` / `t_mail_log`（ADR-0015 §6）。`t_page_view_daily` 是复合 PK 例外，无独立 `id`
- 🔴 时间列**只**用 `DATETIME`，不用 `TIMESTAMP`（ADR-0015 §1 / ADR-0018）
- 🔴 不写 `ON UPDATE`，`updated_at` 由 `AuditFieldHandler` 在应用层填充，避免 DB 自动更新和应用层审计双写
- 🔴 软删用三件套 `deleted` + `deleted_at` + `deleted_by`，不用 V1 的 `is_delete` 单列
- 🔴 审计列用 `created_at` / `updated_at` / `deleted_at`（后缀 `_at`），不用 V1 的 `create_time` / `update_time`
- 🔴 不写 `FOREIGN KEY`，仅建普通索引 `KEY idx_xxx`（ADR-0017 / R-012）
- 🔴 字符集统一 `utf8mb4` + `utf8mb4_0900_ai_ci`
- 🔴 每列必带中文 COMMENT，状态字段列出全部取值含义
- 🔴 索引命名 `idx_{table}_{cols}`，唯一索引 `uk_{table}_{cols}`
- ⚠️ 例外表（如 `t_user_info` / `t_article_tag` / `t_refresh_token` / `t_page_view` / `t_page_view_daily` / `t_mail_log`）按 ADR-0015 §6 单独裁剪审计列

### 步骤 2：跑 `FlywayMigrationTest`

`mvn test -Dtest=FlywayMigrationTest` 确认迁移脚本能跑通。

### 步骤 3：建领域模型

- `domain/model/Xxx.java`
- 字段中文 Javadoc（业务语义，**不写**"旧库兼容"——V2 已全量重设计，ADR-0013）
- 领域内**不直接**调 `LocalDateTime.now()`，时间通过注入的 `Clock`（ADR-0018 / R-011）

### 步骤 4：建 Entity (PO) + Mapper

- `infrastructure.persistence.entity.XxxEntity`，继承 `BaseEntity`（自动获得 8 列审计）；例外表继承 `AuditOnlyBase`
- `@TableId(type = IdType.ASSIGN_ID)`（**不**用 `AUTO`）
- `@TableLogic(value = "0", delval = "1")` 配软删 `deleted` 列
- `infrastructure.persistence.mapper.XxxMapper extends BaseMapper<XxxEntity>`
- 复杂查询：在 `src/main/resources/mapper/{module}/XxxMapper.xml` 写（见 `../rules/sql-placement.md`）

### 步骤 5：建 Repository 接口 + 实现

- `domain/repository/XxxRepository.java`（接口，业务语义）
- `infrastructure/persistence/XxxRepositoryImpl.java`（用 Mapper 实现）
- application 只调 Repository 接口，不直接接触 Mapper（ArchUnit 规则 #3）

### 步骤 6：写持久层测试

- `DatabaseXxxWriterTest`：插入 / 更新 / 软删除
- `DatabaseXxxReaderTest`：查询场景
- 验证 `@TableLogic` 软删后查询自动过滤
- 验证 `AuditFieldHandler` 自动填充 `created_at/by` + `updated_at/by`

### 步骤 7：跑 `mvn test`

确保 ArchUnit 守护 + Flyway 迁移 + 持久层测试全绿。

## 3. Checklist

- [ ] 表在已锁定的 schema 清单里（或已先回写 schema-design.md）
- [ ] Flyway 脚本每列中文 COMMENT
- [ ] **无** `AUTO_INCREMENT`（或确认是 §6 例外表）
- [ ] **无** `TIMESTAMP`（统一 `DATETIME`）
- [ ] **无** `FOREIGN KEY`，仅建索引
- [ ] 8 列审计齐全（业务表）或按例外表清单裁剪
- [ ] 软删三件套 `deleted / deleted_at / deleted_by`
- [ ] 字符集 `utf8mb4_0900_ai_ci`
- [ ] Entity 用 `@TableId(IdType.ASSIGN_ID)` + `@TableLogic`
- [ ] 继承 `BaseEntity` / `AuditOnlyBase`
- [ ] Repository 接口在 domain，实现在 infrastructure
- [ ] 持久层测试覆盖

## 4. 注意

- 🔴 V2 不沿用 V1 表结构（ADR-0013），新表按 `../architecture/schema-design.md` 规范设计
- 🔴 不要在 Flyway 里写业务数据（业务数据用 seed/管理后台维护）
- 🔴 不要修改已 apply 过的 Flyway 脚本，加 `V{n+1}__fix_xxx.sql` 修补
- 🔴 新增模块表后同步更新 `../architecture/module-map.md` + `ArchitectureRulesTest`
