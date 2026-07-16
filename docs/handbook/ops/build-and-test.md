# 构建与测试

> 状态：当前有效
> 适用范围：V2 本地验证与发布前构建
> 最后校准：2026-07-16
> 对应代码：`MyBlog-springboot-v2/pom.xml`、`frontend/apps/blog/package.json`、`frontend/apps/admin/package.json`
> 权威程度：运行手册

## 后端

要求 Java 17、Maven 3.9.x，并以 `Asia/Tokyo` JVM 时区运行。

```powershell
cd MyBlog-springboot-v2
mvn validate
mvn test -Dtest=RelatedTest
mvn clean test
mvn package
```

专项验证：

```powershell
mvn test -Dtest=ArchitectureRulesTest
mvn test -Dtest=FlywayMigrationTest
mvn test -Dtest=MySqlFlywayMigrationTest
mvn test -Dtest=RunningApiContractTest
```

常规 test profile 使用 H2 并执行 Flyway；`RunningApiContractTest` 额外启动随机端口 Tomcat，验证关键真实 HTTP 请求；MySQL 专项使用 Testcontainers。Docker 不可用时条件测试可以跳过，但发布前必须在真实 MySQL 方言上补跑。`mvn package -DskipTests` 只用于临时产物，不能作为完成证据。

## 博客端与管理端

在每个应用目录执行：

```powershell
corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm typecheck
corepack pnpm lint
corepack pnpm build
```

两端 test 均为一次性 Vitest run。管理端 lint 会自动修改文件，执行后必须检查 diff。

## 提交与阶段收口

- 小任务先运行相关测试，再检查 `git diff --stat`、`git status --short` 和 `git diff --check`。
- 阶段结束运行后端 `mvn clean test`，以及两个前端的 test、typecheck、build。
- 发布前再执行真实 MySQL、目标 CORS、可信代理、存储和冒烟验证。
- 结果记录命令、通过情况、skipped 及未执行原因。

测试覆盖选择规则见 `../rules/testing-policy.md`，部署门槛见 `release-checklist.md`。
