# 后端 V2 文档入口

**适用范围：** `MyBlog-springboot-v2`  
**状态：** 当前文档入口  

---

## 1. 使用规则

后端 V2 文档已经进入分阶段沉淀状态。后续推进重构时，先看本入口，再进入具体计划或规范。

如果早期文档与当前权威文档冲突，以本入口列出的当前权威文档为准。早期文档保留为过程记录，不再作为最新执行依据。

---

## 2. 当前权威规范

| 范围 | 当前权威文档 | 作用 |
| --- | --- | --- |
| 项目结构 | `docs/superpowers/specs/2026-05-31-backend-v2-package-structure-decisions.zh-CN.md` | 固定基础包名、顶层包、业务模块内部结构 |
| 技术选型 | `docs/superpowers/specs/2026-05-31-backend-v2-technology-decisions.zh-CN.md` | 固定 Java、Spring Boot、ORM、缓存、MQ、接口文档等技术方向 |
| 注释规范 | `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md` | 规定中文 Javadoc、字段注释、业务注释和后续新增代码注释要求 |
| SQL 放置 | `docs/superpowers/specs/2026-06-01-backend-v2-persistence-sql-placement-rules.zh-CN.md` | 规定 MyBatis-Plus、Mapper 注解、XML、动态 SQL 的放置边界 |
| 风险收口 | `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md` | 当前优先执行计划，用于先修正工程规则、安全边界和迁移规则 |
| MyBatis-Plus 迁移 | `docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md` | 风险收口完成后继续执行的分模块持久层迁移计划 |

---

## 3. 当前已确认结构

后端 V2 基础包名固定为：

```text
com.tyb.myblog.v2
```

顶层结构固定为：

```text
com.tyb.myblog.v2
├── common
├── infrastructure
├── identity
├── content
├── comment
└── system
```

业务模块内部统一采用：

```text
web
application
domain
infrastructure
```

后续新增代码必须放入上述结构，不再引入新的顶层业务容器名。

---

## 4. 当前执行顺序

当前阶段先执行：

```text
docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
```

该计划完成后，再回到：

```text
docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md
```

继续按模块迁移 `JdbcTemplate` 到 MyBatis-Plus。

---

## 5. 后续新增文档规则

- 架构决策、编码规范、技术规则放入 `docs/superpowers/specs/`。
- 可执行任务计划放入 `docs/superpowers/plans/`。
- Review、复盘、风险分析放入 `docs/superpowers/reviews/`。
- 中文文档文件名优先使用 `.zh-CN.md`。
- 后续文档必须使用中文。
- 后续文档如果改变当前权威结论，必须同步更新本入口。

