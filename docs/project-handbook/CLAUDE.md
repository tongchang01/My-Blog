# AI 工作入口（CLAUDE.md / AGENTS.md）

> 本文件是任意 AI 编码助手开始工作前的第一份必读文档。
> 文件名沿用 Claude Code 社区约定（`CLAUDE.md`），但内容对所有 AI 工具通用。

## 一、当前工作目标

本仓库正在进行 **MyBlog 全量重构（V2）**，三端 + 数据库 + 业务逻辑全部重设计。

- **V1**：`MyBlog-springboot/` + `MyBlog-vue/MyBlog-blog/` + `MyBlog-vue/MyBlog-admin/` — 在线运行中，**仅作业务参考，不要修改**
- **V2 后端**：`MyBlog-springboot-v2/` — 六个业务模块已完成第一版，当前进入审查问题修复与前端联调支持
- **V2 前台 / 后台**：进入 M4 前端工程骨架与接口联调阶段；前台规格在 `frontend-user/`，后台规格与计划在 `frontend/apps/admin/docs/`
- **数据**：V2 不兼容 V1 schema（ADR-0013），一次性导入（见 `migration/`）

当前阶段：**M4 前端骨架与后端联调准备**。M3 六个后端模块第一版已经完成并通过 H2/MySQL 回归；当前先处理发布前审查确认的联调阻塞项，再进入前台与后台工程骨架，详见 `status.md` / `roadmap.md` / `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`。

## 二、开始任何任务前必读

按顺序：

1. `overview.md` — 项目概览、技术栈、构建命令
2. `status.md` — 当前进度（每次开工先看）
3. `arch/module-map.md` — 模块边界
4. `pitfalls.md` — 红线与已知坑（动手前整体扫一遍）
5. `rules/` 下与当前任务相关的规则文件

## 三、规则速查表

| 主题 | 文档 |
|------|------|
| 包结构与模块边界 | `rules/package-layout.md` |
| SQL 摆放（BaseMapper / @Select / XML） | `rules/sql-placement.md` |
| 中文注释规范 | `rules/comment-style.md` |
| 异常与错误响应 | `rules/error-handling.md` |
| API 响应与契约 | `rules/api-response.md` |
| 安全基线（JWT / 白名单 / 密码） | `rules/security-baseline.md` |
| 测试策略 | `rules/testing-policy.md` |

## 四、SOP 速查表

| 任务 | SOP |
|------|-----|
| 新增业务模块 | `workflows/add-new-module.md` |
| 新增 API 接口 | `workflows/add-new-api.md` |
| 新增数据库表 | `workflows/add-new-table.md` |
| 构建与测试 | `workflows/build-and-test.md` |
| 初始化本地 MySQL 联调环境 | `workflows/local-mysql-development.md` |
| 写 ADR | `workflows/write-adr.md` |

## 五、架构现状速查

| 视角 | 文档 |
|------|------|
| 模块与依赖 | `arch/module-map.md` |
| 持久化策略 | `arch/persistence-strategy.md` |
| 认证流程 | `arch/auth-flow.md` |
| 请求处理链路 | `arch/request-flow.md` |

## 六、为什么这么定（ADR）

历史决策见 `decisions/0001` ~ `decisions/0018`，覆盖：模块化单体、包名、四层架构、六大模块、MyBatis-Plus、Spring Boot 3、JWT（含 R6 C1 双 token 补充）、Hutool、Knife4j 4.x（基于 springdoc-openapi）、SQL 分层、中文注释、ArchUnit、**V2 不兼容 V1 数据结构（0013）、schema 重设计原则（0014，部分被 0015 / 0018 超越）、审计列 + 软删三件套（0015）、URL id-led + slug（0016）、不使用 DB FOREIGN KEY（0017）、时区统一 Asia/Tokyo 五层（0018）**。

## 七、红线（永远不要做）

详见 `pitfalls.md`，关键速记：

- 🔴 不硬编码任何密钥（JWT、DB、第三方 token）
- 🔴 不修改 V1（`MyBlog-springboot/`）
- 🔴 不引入未经 ADR 授权的中间件与依赖（Redis、MQ 等）
- 🔴 业务模块之间不跨过 application 层互调
- 🔴 不引入 `hutool-all`
- 🔴 Controller 不自己 try-catch 后返错（抛 ApiException）
- 🔴 不用 HTTP 200 返业务失败
- 🔴 异常消息不暴露内部细节（SQL / 堆栈 / 表名）
- 🔴 不跳过 ArchUnit 测试
- 🔴 不改已 apply 的 Flyway 脚本

## 八、工作风格约定

- **小步快走**：任务先拆成可独立实现、验证和回滚的小任务，一次只改一件事
- **小批次提交**：每个 Git 提交只完成一个明确目的，不把清理、基础设施、模块重建和文档整理等不同阶段混在一起
- **控制提交范围**：提交前检查 `git diff --stat` 和 `git status --short`；如果扩散到大量文件，先继续拆分。确实不可拆的批量机械变更必须提前说明
- **中文提交信息**：Git 提交信息必须使用中文，并准确描述本次提交的单一目的
- **先看现状再动手**：修改前用 grep/glob 确认实现，不靠记忆
- **遵循已有模式**：新代码风格须与同模块同层的现有代码一致
- **不擅自重构**：发现需要大改，先和用户对齐
- **写代码同步更新文档**：违反某条 rule 必须同时改 rule + 写理由

## 九、构建与验证

进入 `MyBlog-springboot-v2/` 后：

```bash
mvn clean test                              # 跑所有测试（含 ArchUnit）
mvn test -Dtest=ArchitectureRulesTest       # 单跑架构守护
mvn spring-boot:run -Dspring-boot.run.profiles=local   # 本地启动
```

启动 `local` / `prod` 前必须提供数据库账号、密码和 `MYBLOG_JWT_SECRET`（≥32 字节）；完整清单与 profile 差异见 `workflows/build-and-test.md`。空库初始化、固定开发种子和真实前后台联调见 `workflows/local-mysql-development.md`。

## 十、当前焦点

- **进行中**：修复后端发布前审查确认的联调阻塞与测试隔离问题
- **下一步**：启动 M4 前端工程骨架，优先联调登录、公开站点配置和非 PASSWORD 文章链路
- **后续裁决**：PASSWORD 解锁、DEMO 敏感字段和 Web → Domain 边界单独设计，不在联调修复中顺带改变

## 十一、文档地图

| 想知道 | 看哪里 |
|--------|--------|
| AI 工作入口 | 本文件 |
| 项目概览 | `overview.md` |
| 当前进度 | `status.md` |
| V1 vs V2 对比 | `v1-vs-v2.md` |
| 未来路线 | `roadmap.md` |
| 规则 | `rules/` |
| 架构现状 | `arch/` |
| 决策理由 | `decisions/` |
| 操作 SOP | `workflows/` |
| 红线 / 历史坑 | `pitfalls.md` |
| **业务规格** | `product/` |
| **接口契约** | `api-contract/` |
| **前台规格** | `frontend-user/` |
| **后台规格** | `frontend/apps/admin/docs/` |
| **V1→V2 数据迁移** | `migration/` |
| 文档结构总览 | `INDEX.md` |
