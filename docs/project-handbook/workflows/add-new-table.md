# 新增数据库表（SOP）

> 目标：新增一张业务表，从迁移脚本到代码全链路落地。

## 1. 前置确认

- 表归属哪个模块？
- 表名风格遵循 `arch/schema-design.md` 中的命名规范（V2 全新设计，不再沿用 V1 习惯）
- 字段命名：snake_case
- 软删除字段：参考 `arch/schema-design.md` 软删除规范（V2 倾向布尔语义，非 0/1 整型）
- 时间字段：参考 `arch/schema-design.md` 时间字段规范

## 2. 步骤

### 步骤 1：写 Flyway 迁移脚本

位置：`src/main/resources/db/migration/V{version}__add_t_xxx.sql`

```sql
CREATE TABLE t_xxx_yyy (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    article_id  BIGINT       NOT NULL              COMMENT '所属文章 id',
    content     VARCHAR(500) NOT NULL              COMMENT '内容',
    is_delete   TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '软删除 0 否 1 是',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '业务表';
```

- 列必带中文 COMMENT
- 主键、索引、外键策略明确
- H2 兼容性：注意 H2 不支持 MySQL 全部语法，必要时写 H2 专属版本

### 步骤 2：跑 `FlywayMigrationTest`

`mvn test -Dtest=FlywayMigrationTest` 确认 H2 能跑通迁移脚本。

### 步骤 3：建领域模型

- domain/model/Xxx.java
- 字段中文 Javadoc + V1 数据导入映射说明（仅在确有导入需求时）

### 步骤 4：建 PO + Mapper

- `infrastructure.persistence.po.XxxPO`（如与 domain 分离）
- `infrastructure.persistence.mapper.XxxMapper extends BaseMapper<XxxPO>`
- 复杂查询：在 `src/main/resources/mapper/{module}/XxxMapper.xml` 写

### 步骤 5：建 Repository 实现 + 接口

- `domain/repository/XxxRepository.java`
- `infrastructure/persistence/XxxRepositoryImpl.java`

### 步骤 6：写持久层测试

- `DatabaseXxxWriterTest`：插入/更新/删除
- `DatabaseXxxReaderTest`：查询场景

### 步骤 7：跑 `mvn test`

## 3. Checklist

- [ ] Flyway 脚本带列 COMMENT
- [ ] H2 兼容验证
- [ ] domain Entity 字段中文 Javadoc
- [ ] PO ↔ Entity 映射类
- [ ] Repository 接口在 domain，实现在 infrastructure
- [ ] 持久层测试覆盖
- [ ] 索引、约束完整

## 4. 注意

- 🔴 V2 不沿用 V1 表结构（见 ADR-0013），新表按 `arch/schema-design.md` 规范设计
- 🔴 不要在 Flyway 里写业务数据（业务数据用 seed/管理后台维护）
- 🔴 不要修改已 apply 过的 Flyway 脚本，加 `V{n+1}__fix_xxx.sql` 修补
