# 测试策略

> 状态：当前有效
> 适用范围：MyBlog V2 后端、前台、后台和文档整理
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：规则

## 本文档回答什么问题

本文档规定什么场景必须写测试、使用哪些测试工具、不同类型变更应该运行哪些验证命令，以及提交前如何记录验证结果。

## 1. 总原则

- 测试范围按风险和影响面选择。
- 后端架构规则必须随 `mvn test` 一起执行。
- 新增或修改业务规则必须有对应自动化测试。
- 只改文档时不需要跑业务测试，但必须检查链接、路径和 diff 范围。
- 不固化测试总数；测试数量只作为当次执行证据。
- Docker 不可用导致 Testcontainers 条件测试跳过时，必须在结果中说明。

## 2. 后端测试技术栈

| 工具 | 用途 |
|------|------|
| JUnit 5 | 单元测试和集成测试基础 |
| Spring Boot Test | 后端集成测试 |
| Spring Security Test | 认证授权测试 |
| H2 | 常规 test profile 内存数据库 |
| Flyway | 测试库迁移验证 |
| Testcontainers MySQL | MySQL 真实方言验证，Docker 不可用时可跳过 |
| ArchUnit | 包结构和依赖方向守护 |
| Maven Enforcer | Java、Maven 和依赖收敛基线 |

当前后端测试文件约 172 个，具体数量以实际执行为准。

## 3. 前端测试技术栈

| 应用 | 工具 |
|------|------|
| 前台 blog | Vitest、vue-tsc、ESLint、Vite build |
| 后台 admin | Vitest、Vue Test Utils、happy-dom、TypeScript、vue-tsc、ESLint、Prettier、Stylelint、Vite build |

前端测试数量随页面开发变化，文档中不长期固化总数。

## 4. 必须写测试的场景

后端必须覆盖：

- 登录、refresh、logout、改密、token 失效。
- ADMIN / DEMO / GUEST 权限边界。
- 公开接口和后台接口的白名单与鉴权。
- 软删除、恢复、引用保护、状态流转。
- 评论审核、隐藏、删除、恢复和评论计数。
- 匿名写入口限流、重复提交、内容清洗。
- 附件上传类型、大小、真实格式和非法图片。
- 统计打点、聚合、补算、清理和日期边界。
- 自定义 XML SQL 的查询条件、排序、分页和并发关键路径。
- Flyway 迁移脚本。
- ArchUnit 包结构和跨模块依赖规则。

前端必须覆盖：

- 认证会话：登录、恢复、refresh 单飞、失败清理、退出。
- 路由权限：未登录、ADMIN、DEMO。
- API 错误码到界面提示的映射。
- 列表页 loading、empty、error、retry、分页和 latest-request-wins。
- 表单校验、提交、取消、未保存修改提示。
- DEMO 只读交互。
- 文章编辑器、附件选择器、评论和统计等复杂交互。
- 前台公开页面的 loading、empty、404、locked、error、retry。

## 5. 可以不写测试的场景

一般可以不为以下内容单独写测试：

- 纯 getter / setter。
- 无分支的简单 DTO。
- 简单样板配置 Bean。
- 无业务语义的常量搬移。
- 仅调整 Markdown 文档。

但如果这些变更影响公开契约、权限、安全或运行配置，仍需写测试或执行对应验证。

## 6. 后端命令

在 `MyBlog-springboot-v2/` 下执行：

```powershell
mvn validate
mvn clean compile
mvn test
mvn test -Dtest=ArchitectureRulesTest
mvn test -Dtest=FlywayMigrationTest
mvn test -Dtest=MySqlFlywayMigrationTest
mvn package
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

说明：

- `mvn test` 是常规全量验证，包含 ArchUnit。
- `ArchitectureRulesTest` 可在包结构变更时单跑。
- `MySqlFlywayMigrationTest` 需要 Docker；Docker 不可用时可跳过，但发布前必须补真实 MySQL 方言验证。
- `mvn package -DskipTests` 不能作为提交前验证结果。

## 7. 前台 blog 命令

在 `frontend/apps/blog/` 下执行：

```powershell
corepack pnpm install --frozen-lockfile
corepack pnpm lint
corepack pnpm typecheck
corepack pnpm test
corepack pnpm exec vitest run
corepack pnpm build
```

说明：

- `pnpm test` 默认进入 Vitest 交互/监听模式，自动化验证优先用 `pnpm exec vitest run`。
- 修改页面、store、API 或路由时，至少跑相关测试、typecheck 和 build。

## 8. 后台 admin 命令

在 `frontend/apps/admin/` 下执行：

```powershell
corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
corepack pnpm lint
```

说明：

- `pnpm test` 当前是 `vitest run`。
- `pnpm lint` 会执行 ESLint、Prettier 和 Stylelint，并可能自动修复格式；提交前必须检查 diff。
- 只改单个功能时，可先跑相关测试文件，再跑 typecheck/build 作为收口验证。

## 9. 按变更类型选择验证

| 变更类型 | 最小验证 | 推荐收口验证 |
|----------|----------|--------------|
| 后端 domain 规则 | 相关单测 | `mvn test` |
| 后端 Controller/API | 对应 Controller 测试 | `mvn test` |
| 后端 Mapper/XML/Flyway | 对应持久层测试 / Flyway 测试 | `mvn test`，必要时 MySQL Testcontainers |
| 安全配置/JWT/权限 | 相关 Security/Auth 测试 | `mvn test` |
| 包结构/依赖方向 | `ArchitectureRulesTest` | `mvn test` |
| 前台页面/store/API | 相关 Vitest | typecheck + build |
| 后台页面/store/API | 相关 Vitest | typecheck + build |
| 只改文档 | 链接/路径/diff 检查 | `git diff --stat` + 抽样阅读 |

## 10. 测试数据库策略

| Profile | 数据库 | Flyway | 用途 |
|---------|--------|--------|------|
| `test` | H2 内存库，专项测试可用 Testcontainers MySQL | 启动时执行 | 常规自动化测试 |
| `local` | 本机 MySQL `myblog_v2_dev` | 当前配置启用 | 本地联调和手工验收 |
| `prod` | 生产 MySQL | 启动时执行 | 生产运行 |

注意：当前 `application.yml` 仍设置默认 profile 为 `local`。运行后端测试和服务时，应明确自己使用的 profile 和环境变量，避免误连数据库。

## 11. 命名约定

| 命名 | 用途 |
|------|------|
| `XxxTest` | 单元或集成测试 |
| `XxxControllerTest` | Web/API 测试 |
| `XxxOpenApiTest` | OpenAPI 契约测试 |
| `FlywayMigrationTest` | H2 迁移验证 |
| `MySqlFlywayMigrationTest` | MySQL 方言迁移验证 |
| `ArchitectureRulesTest` | 架构守护 |
| `*.test.ts` | 前端 Vitest 测试 |

测试方法命名应描述行为和条件，例如：

```java
@Test
void rejectsLoginWhenPasswordIsWrong() { }
```

```ts
it("clears session when refresh fails", () => {})
```

## 12. 验证结果记录

提交或阶段说明中应记录：

- 运行了哪些命令。
- 是否通过。
- 是否有 skipped。
- skipped 是否为已知条件，例如 Docker 不可用。
- 未运行某个应运行验证时，说明原因。

示例：

```text
验证：mvn test 通过；637 tests，0 failures，0 errors，4 skipped，跳过项为 Docker 不可用时的 Testcontainers MySQL 条件测试。
```

## 13. 禁止事项

- 禁止用 `mvn package -DskipTests` 冒充测试通过。
- 禁止提交前只跑编译而不跑相关测试。
- 禁止忽略 ArchUnit 失败。
- 禁止为了让测试通过而放宽权限或删除断言。
- 禁止把无关坏测试和当前变更混在同一提交里顺手大改。
- 禁止在没有说明的情况下跳过真实 MySQL 方言验证。

## 相关文档

- 包结构规则：`package-layout.md`
- API 响应规则：`api-response.md`
- 安全基线：`security-baseline.md`
- 构建与测试 SOP：`../ops/build-and-test.md`
