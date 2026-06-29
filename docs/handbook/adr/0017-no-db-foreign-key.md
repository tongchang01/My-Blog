# ADR-0017: 不使用 DB FOREIGN KEY 约束

- 状态：accepted
- 日期：2026-06
- 决策者：项目负责人

## 背景

业务表之间存在大量引用关系：`t_article.author_id → t_user_auth.id`、`t_comment(target_type,target_id) → t_article.id`（ARTICLE 场景）、`t_article.category_id → t_category.id` 等。是否在 DB 层用 `FOREIGN KEY` 约束维护引用完整性，是一个早期难改的决定（加了再去要锁表 + 数据迁移）。

## 备选方案

- 方案 A：DB 层加 `FOREIGN KEY`，由数据库强制
- 方案 B：DB 只建普通索引，引用完整性由 application 层维护

## 决定

选 **方案 B**：所有业务表**不使用 DB FOREIGN KEY 约束**。

## 规则

- 所有 `*_id` 列（`category_id` / `parent_id` / `reply_to_comment_id` / `cover_attachment_id` / `user_id` 等）只建普通索引：

```sql
KEY idx_article_category (category_id)
```

- **禁止**：

```sql
FOREIGN KEY (category_id) REFERENCES t_category(id)
```

- 引用完整性由 application 层（service）维护：
  - 写入前校验被引用方存在
  - 软删父表不级联：删父表只 `deleted=1`，子表引用保留，application 层查询时按需过滤 / fallback
  - 物理删除走运维 SQL，执行前手动验关联（详见 R4 #17）

## 理由

- **阿里 Java 开发规范明确推荐**："不得使用外键与级联，一切外键概念必须在应用层解决"
- **改表 / 模块迁移友好**：拆库 / 改表 / 调字段类型不被 FK 阻塞
- **软删语义灵活**：DB FK 不理解"软删"语义；级联删与软删冲突
- **性能**：大表写入时 FK 校验是额外开销
- **测试简单**：测试库 truncate 不被 FK 顺序约束
- **微服务/拆库铺路**：未来拆库时 FK 跨库无意义

代价已被接受：
- application 层必须严谨校验引用完整性（service 层是 PR review 重点）
- 测试需覆盖"引用方不存在"的边界

## 守护

- **`pitfalls.md` R-012 红线**：DDL 出现 `FOREIGN KEY` 即违规
- ArchUnit 不直接管 DDL，但 Flyway 脚本 review 时必查

## 后果

正面：
- schema 演进灵活
- 软删 / 级联语义在 application 层显式表达
- 拆库零障碍

负面：
- 引用完整性依赖代码 + 测试覆盖；bug 可能写出"悬空引用"
- 需要在团队约定中强调"FK 概念在 service 层"

## 相关

- 关联决定：`product/decisions-draft.md` R7 D2
- 关联 pitfalls：R-012（不得加 DB FOREIGN KEY）
