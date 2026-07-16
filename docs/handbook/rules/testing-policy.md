# 测试与验证规则

> 状态：当前有效
> 适用范围：V2 后端、博客端、管理端和文档
> 最后校准：2026-07-16
> 对应代码：`MyBlog-springboot-v2/src/test/`、`frontend/apps/blog/src/`、`frontend/apps/admin/src/`
> 权威程度：规则

## 原则

- 每个小任务先运行与变更风险匹配的局部测试，阶段结束再运行完整验证。
- 业务规则、权限、安全、状态流转、持久化和复杂交互的变更必须有自动化测试。
- 跨端 HTTP 契约变更不能只以 MockMvc 或前端 mock 作为通过证据；至少对受影响关键流程执行一次运行中 API 验证。
- 不在长期文档中固化测试总数。
- Testcontainers 因 Docker 不可用而跳过时，结果中必须说明；发布前必须补真实 MySQL 验证。
- 仅修改文档也需检查链接、路径、元数据、过期引用、`git diff --check` 和变更范围。

## 后端

要求 JDK 17 与 Maven 3.9.x。在 `MyBlog-springboot-v2/` 执行：

```powershell
mvn validate
mvn test -Dtest=RelatedTest
mvn test
mvn package
```

专项命令：

```powershell
mvn test -Dtest=ArchitectureRulesTest
mvn test -Dtest=FlywayMigrationTest
mvn test -Dtest=MySqlFlywayMigrationTest
```

`mvn package -DskipTests` 不能作为验证通过的证据。测试 profile 使用 H2 和 Flyway；MySQL Testcontainers 负责真实方言差异。

## 博客端与管理端

两个应用均要求 Node `^24.0.0` 与 pnpm 9。在各应用目录执行：

```powershell
corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm typecheck
corepack pnpm lint
corepack pnpm build
```

两端的 `pnpm test` 当前均为一次性 `vitest run`。管理端 `pnpm lint` 会自动修复格式，执行后必须检查 diff。

## 风险映射

| 变更 | 局部验证 | 阶段验证 |
| --- | --- | --- |
| domain/application | 对应单元测试 | `mvn test` |
| Controller/安全 | Web 与 Security 测试 | `mvn test` |
| Mapper/Flyway | 持久化与迁移测试 | H2 全量，必要时 MySQL |
| 包结构 | `ArchitectureRulesTest` | `mvn test` |
| 前端 API/store/component | 对应 Vitest | test + typecheck + build |
| 跨端 HTTP 契约 | 运行中 API 的针对性请求 | 关键契约集 + 三端各自完整验证 |
| 文档 | 链接、路径、措辞、diff | 全仓文档扫描 |

提交或阶段说明记录命令、结果、skipped 和未执行项原因。
