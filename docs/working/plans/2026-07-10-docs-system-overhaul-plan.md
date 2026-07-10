# MyBlog 文档体系彻底整理实施计划

> 执行方式：按任务顺序在本地 `main` 分支实施，每个任务独立验证、独立提交，不推送远程。

**目标：** 删除无长期价值的文档，建立唯一权威文档体系，将保留内容与当前代码校准，并记录第一版上线、遗留事项和扩展方向。

**架构：** `docs/` 最终只保留总入口、当前开发手册、仓库治理和项目展示四类内容。Git 历史承担过程追溯，不再维护 `archive/`、`working/`、旧入口或重复副本；代码和可执行测试是事实源，`handbook/` 是面向开发协作的人类可读权威说明。

**技术栈：** Markdown、PowerShell、Git、Maven、pnpm、Vue、Spring Boot。

## 全局约束

- 全部操作位于本地 `main` 分支，不推送远程。
- 文档正文默认中文；展示文档允许中文、英文、日文三个版本。
- 文档使用项目主体表达，避免对话式人称代词和具体 AI 工具名称。
- 当前代码、测试、配置和数据库迁移高于历史文档。
- 无长期价值的历史内容直接删除，追溯依赖 Git 历史。
- 每个提交只完成一个明确目的，提交信息使用中文。
- 每次提交前检查 `git diff --stat` 和 `git status --short`。

---

### 任务 1：清理历史、重复和过程文档

**删除范围：**

- `docs/archive/`
- `docs/working/`
- `docs/project-handbook/`
- `docs/repository-governance/`
- `docs/deep-research-report.md`
- `docs/refactor-plan.zh-CN.md`
- `docs/local-development.md`
- `docs/MyBlog-项目展示.md`
- `docs/MyBlog-Project-Showcase.en.md`
- `docs/MyBlog-プロジェクト紹介.ja.md`
- `docs/governance/2026-06-26-repository-reorganization-plan.md`
- `docs/governance/v2-main-prep-verification.md`

**保留范围：**

- `docs/README.md`
- `docs/handbook/`
- `docs/governance/README.md`
- `docs/governance/branch-policy.md`
- `docs/showcase/`

- [ ] 使用 `git rm -r` 删除无长期价值内容。
- [ ] 全仓搜索对删除路径的引用并登记到后续校准任务。
- [ ] 运行 `git diff --stat`，确认本任务以删除和去重为主。
- [ ] 提交：`文档：删除历史副本和过程材料`。

### 任务 2：重建文档规则和入口

**修改文件：**

- `docs/README.md`
- `docs/handbook/README.md`
- `docs/handbook/rules/documentation.md`
- `docs/handbook/rules/README.md`
- `docs/governance/README.md`
- `docs/governance/branch-policy.md`

**删除候选：**

- `docs/handbook/workflows/migrate-jdbc-to-mybatis-plus.md`

- [ ] 将目录模型固定为 `handbook / governance / showcase`，删除归档和临时过程层。
- [ ] 定义事实优先级、文档头、状态、更新触发器、重复内容限制和删除规则。
- [ ] 定义计划只在任务执行期存在，完成后删除，由 Git 保留历史。
- [ ] 校准分支策略与根 `AGENTS.md`，保留普通任务分支规则并说明当前直接在 `main` 整理的授权例外不进入长期规则。
- [ ] 删除仅服务于已完成 JDBC 迁移的工作流。
- [ ] 验证入口只指向实际存在的路径。
- [ ] 提交：`文档：重建文档结构和维护规则`。

### 任务 3：校准后端架构、ADR 和产品规格

**事实源：**

- `MyBlog-springboot-v2/pom.xml`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
- `MyBlog-springboot-v2/src/main/resources/`
- `MyBlog-springboot-v2/src/test/`

**修改范围：**

- `docs/handbook/architecture/`
- `docs/handbook/adr/`
- `docs/handbook/product/`
- `docs/handbook/rules/package-layout.md`
- `docs/handbook/rules/sql-placement.md`
- `docs/handbook/rules/comment-style.md`
- `docs/handbook/rules/error-handling.md`
- `docs/handbook/rules/api-response.md`
- `docs/handbook/rules/security-baseline.md`
- `docs/handbook/rules/testing-policy.md`
- `docs/handbook/workflows/add-new-module.md`
- `docs/handbook/workflows/add-new-table.md`
- `docs/handbook/workflows/write-adr.md`

- [ ] 核对五个业务模块和 `common` 基础设施边界。
- [ ] 核对四层包结构、ArchUnit 约束、认证端口和跨模块依赖。
- [ ] 核对四个 Flyway 迁移、14 张基础表、审计列、软删除和无外键策略。
- [ ] 核对认证、限流、附件、邮件、统计、文章访问和 DEMO 边界。
- [ ] 审核 18 份 ADR；保留仍约束当前代码的决策，删除或标记已替代的决策。
- [ ] 删除已由架构、Schema 或 API 文档完整覆盖的重复产品说明。
- [ ] 为所有保留文件补齐统一文档头和精确代码路径。
- [ ] 提交：`文档：校准后端架构和开发规则`。

### 任务 4：逐接口校准 API 契约

**事实源：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/web/`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/application/`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/`

**修改文件：**

- `docs/handbook/api/README.md`
- `docs/handbook/api/auth.md`
- `docs/handbook/api/article.md`
- `docs/handbook/api/attachment.md`
- `docs/handbook/api/category-tag.md`
- `docs/handbook/api/comment.md`
- `docs/handbook/api/friend-link.md`
- `docs/handbook/api/site-config.md`
- `docs/handbook/api/stats.md`
- `docs/handbook/workflows/add-new-api.md`

- [ ] 核对路径、HTTP 方法、鉴权角色和公开白名单。
- [ ] 核对请求字段、响应字段、分页模型、ID 字符串边界、枚举和错误码。
- [ ] 删除与代码模型重复且容易漂移的逐字段背景说明，只保留联调所需契约。
- [ ] 确认测试专用 `SecurityProbeController` 不进入生产 API 清单。
- [ ] 使用 Controller 测试和 OpenAPI 测试验证关键契约。
- [ ] 提交：`文档：校准后端接口契约`。

### 任务 5：校准前台和后台文档

**事实源：**

- `frontend/apps/blog/package.json`
- `frontend/apps/blog/src/`
- `frontend/apps/blog/tests/`
- `frontend/apps/admin/package.json`
- `frontend/apps/admin/src/`
- `frontend/apps/admin/tests/`

**修改文件：**

- `docs/handbook/frontend/README.md`
- `docs/handbook/frontend/blog/README.md`
- `docs/handbook/frontend/blog/integration-status.md`
- `docs/handbook/frontend/admin/README.md`
- `docs/handbook/frontend/admin/integration-status.md`

- [ ] 核对 blog 页面、路由、V2 API、状态管理、旧数据源清理和后置能力。
- [ ] 核对 admin 登录、会话、权限、业务页面、DEMO 边界和测试命令。
- [ ] 删除阶段描述和已完成待办，只保留当前行为、边界和已知限制。
- [ ] 更新前端依赖和版本要求，不复制完整 `package.json`。
- [ ] 提交：`文档：校准前台和后台现状`。

### 任务 6：校准本地开发、测试、CI 和部署文档

**事实源：**

- `MyBlog-springboot-v2/src/main/resources/application*.yml`
- `MyBlog-springboot-v2/src/test/resources/`
- `frontend/apps/blog/.env*`
- `frontend/apps/admin/.env*`
- `.github/workflows/ci.yml`
- 三端 `package.json` 与 `pom.xml`

**修改范围：**

- `docs/handbook/ops/`

- [ ] 核对 Java、Maven、Node、pnpm、MySQL 版本和三端启动命令。
- [ ] 核对 local、test、prod 配置和全部生产环境变量。
- [ ] 核对 CI 实际执行的后端、MySQL、blog、admin 检查。
- [ ] 将部署方向写成可执行顺序：服务器准备、数据库、后端、前端、反向代理、HTTPS、S3、备份、冒烟和回滚。
- [ ] 保留第一版上线检查清单，未确认的服务器参数使用明确的“部署前确认项”，不编造值。
- [ ] 删除已完成 CI 设计过程或重复本地开发说明。
- [ ] 提交：`文档：校准开发测试和部署说明`。

### 任务 7：重写项目状态、上线路线和遗留事项

**修改文件：**

- `docs/handbook/start-here/project-overview.md`
- `docs/handbook/start-here/current-status.md`
- `docs/handbook/start-here/roadmap.md`
- `docs/handbook/start-here/open-issues.md`
- `docs/handbook/start-here/glossary.md`
- `docs/handbook/start-here/pitfalls.md`
- `README.md`
- `README.en.md`
- `README.ja.md`

- [ ] 将当前状态压缩为已完成能力、当前发布阻塞项和验证状态。
- [ ] 从 `open-issues.md` 删除所有已关闭项目，Git 历史承担关闭记录。
- [ ] 保留 PASSWORD 解锁、留言板、部署硬项、token 存储、多实例限流等真实遗留项。
- [ ] 将路线图分为第一版上线、上线后修正、内容能力扩展、架构扩展四段。
- [ ] 更新根 README 三语入口和项目定位，消除 V1 功能误写到 V2 展示的情况。
- [ ] 提交：`文档：更新当前状态和后续路线`。

### 任务 8：校准项目展示文档

**修改文件：**

- `docs/showcase/README.md`
- `docs/showcase/myblog-showcase.zh-CN.md`
- `docs/showcase/myblog-showcase.en.md`
- `docs/showcase/myblog-showcase.ja.md`

- [ ] 以 V2 已实现能力重写项目展示，删除相册、说说、旧音乐播放器等未进入 V2 的功能描述。
- [ ] 三语版本保持相同事实范围，不复制开发手册细节。
- [ ] 检查项目主体表达和外部链接。
- [ ] 提交：`文档：校准项目展示内容`。

### 任务 9：全量验证和最终收口

**验证范围：**

- `docs/`
- `README.md`
- `README.en.md`
- `README.ja.md`
- `AGENTS.md`

- [ ] 确认 `docs/` 只保留 `README.md`、`handbook/`、`governance/`、`showcase/`。
- [ ] 检查 Markdown 相对链接全部可解析。
- [ ] 检查 handbook 文档头完整、校准日期有效、代码路径存在。
- [ ] 搜索已删除目录、旧绝对路径、旧分支、历史阶段和失效文件引用。
- [ ] 搜索对话式人称代词和具体 AI 工具名称，排除不符合项目主体表达的措辞。
- [ ] 检查重复文件哈希和重复权威说明。
- [ ] 运行 `mvn clean test`。
- [ ] 运行 blog 的 `pnpm test`、`pnpm typecheck`、`pnpm build`。
- [ ] 运行 admin 的 `pnpm test`、`pnpm typecheck`、`pnpm build`。
- [ ] 删除本实施计划，确认 Git 历史已经保留执行过程。
- [ ] 检查 `git diff --stat`、`git status --short` 和最近本地提交。
- [ ] 提交：`文档：完成文档体系最终校验`。
