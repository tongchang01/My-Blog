# 后端 V2 历史执行文档入口

**适用范围：** `MyBlog-springboot-v2`  
**状态：** 历史执行计划与阶段性规范入口

> 长期权威文档入口已迁移到 `docs/project-handbook/INDEX.md`。
> 本目录主要保留已执行计划、阶段性 specs、reviews 和过程记录。

---

## 1. 使用规则

后端 V2 文档已经进入长期 handbook + 阶段计划并行状态。后续推进重构时：

1. 长期规则、架构、ADR、产品、接口契约，先看 `docs/project-handbook/`。
2. 具体阶段任务和历史执行记录，再看本目录下的 `plans/`、`specs/`、`reviews/`。

如果本目录与 `docs/project-handbook/` 冲突，以 `docs/project-handbook/` 为准。

---

## 2. 已迁入长期入口

长期入口：

```text
docs/project-handbook/INDEX.md
```

其中已经覆盖：

- 包结构与模块边界
- 技术选型与 ADR
- SQL 放置规则
- 中文注释规范
- 安全基线
- 测试策略
- V1/V2 关系
- 产品规格、接口契约、前后台规格和数据迁移目录

---

## 3. 历史阶段规范

| 范围 | 当前权威文档 | 作用 |
| --- | --- | --- |
| 项目结构 | `docs/superpowers/specs/2026-05-31-backend-v2-package-structure-decisions.zh-CN.md` | 固定基础包名、顶层包、业务模块内部结构 |
| 技术选型 | `docs/superpowers/specs/2026-05-31-backend-v2-technology-decisions.zh-CN.md` | 固定 Java、Spring Boot、ORM、缓存、MQ、接口文档等技术方向 |
| 注释规范 | `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md` | 规定中文 Javadoc、字段注释、业务注释和后续新增代码注释要求 |
| SQL 放置 | `docs/superpowers/specs/2026-06-01-backend-v2-persistence-sql-placement-rules.zh-CN.md` | 规定 MyBatis-Plus、Mapper 注解、XML、动态 SQL 的放置边界 |
| 风险收口 | `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md` | 已完成，保留为工程基盘收口记录 |
| MyBatis-Plus 迁移 | `docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md` | 持久层迁移计划，后续是否继续执行需结合 `docs/project-handbook/product/` 与新 schema 决策 |

---

## 4. 当前已确认结构

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

## 5. 当前执行顺序

风险收口计划已经完成：

```text
docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
```

后续是否继续执行 MyBatis-Plus 分模块迁移，需要先结合产品清单、新数据模型和新 schema 判断：

```text
docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md
```

---

## 6. 后续新增文档规则

- 长期规则、架构决策、产品规格、接口契约优先放入 `docs/project-handbook/`。
- 只用于一次性执行的任务计划放入 `docs/superpowers/plans/`。
- Review、复盘、风险分析放入 `docs/superpowers/reviews/`。
- 中文文档文件名优先使用 `.zh-CN.md`。
- 后续文档必须使用中文。
- 后续文档如果改变长期权威结论，必须同步更新 `docs/project-handbook/` 对应文件。
