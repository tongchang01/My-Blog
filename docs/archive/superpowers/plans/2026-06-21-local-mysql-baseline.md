# V2 本地 MySQL 基线实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 V2 后端可以从空的 `myblog_v2_dev` MySQL 数据库完成 Flyway 初始化、导入可重复开发种子并支撑前后台真实联调，同时保留 H2 回归。

**Architecture:** local profile 负责连接 MySQL并启用 Flyway；独立 PowerShell 脚本负责空库守护和种子导入，种子 SQL 不进入 Flyway。配置契约用自动化测试守护，真实 MySQL 验收只使用用户本机环境变量，不提交凭据。

**Tech Stack:** MySQL 8、Spring Boot 3、Flyway、PowerShell、JUnit、Maven、curl/Invoke-WebRequest、pnpm。

---

## 文件职责

- `application-local.yml`：local profile MySQL/Flyway 配置。
- `LocalProfileConfigTest.java`：锁定 local profile 使用 MySQL、Flyway 和 Asia/Tokyo。
- `scripts/dev/mysql/seed.sql`：无隐私、固定 ID 的最小开发数据。
- `scripts/dev/mysql/initialize.ps1`：空库检查、启动迁移和种子导入编排。
- `scripts/dev/mysql/verify.ps1`：关键表数量和数据库时区检查。
- `docs/project-handbook/workflows/local-mysql-development.md`：环境变量、命令、重建和故障处理。

### Task 1：修复 local profile 的 Flyway 基线

**Files:**
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/LocalProfileConfigTest.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`

- [ ] **Step 1: 写失败测试**

测试加载 `application-local.yml` 并断言：

```java
assertThat(properties.get("spring.datasource.driver-class-name"))
        .isEqualTo("com.mysql.cj.jdbc.Driver");
assertThat(properties.get("spring.flyway.enabled")).isEqualTo(true);
assertThat(properties.get("spring.datasource.url").toString())
        .contains("myblog_v2_dev", "Asia/Tokyo", "time_zone='%2B09:00'");
```

- [ ] **Step 2: 验证 RED**

Run: `cd MyBlog-springboot-v2; mvn test -Dtest=LocalProfileConfigTest`

Expected: FAIL，当前 `spring.flyway.enabled` 为 `false`。

- [ ] **Step 3: 最小实现**

将 `application-local.yml` 改为：

```yaml
spring:
  flyway:
    enabled: true
```

不得修改 `V1__init.sql` 或 `V2__backfill_user_info.sql`。

- [ ] **Step 4: 验证并提交**

Run: `mvn test -Dtest=LocalProfileConfigTest,FlywayMigrationTest`

Run: `git diff --stat; git status --short; git diff --check`

Commit: `启用本地MySQL的Flyway迁移`

### Task 2：建立最小开发种子 SQL

**Files:**
- Create: `MyBlog-springboot-v2/scripts/dev/mysql/seed.sql`
- Create: `MyBlog-springboot-v2/scripts/dev/mysql/verify-seed.sql`

- [ ] **Step 1: 写种子验收查询**

`verify-seed.sql` 必须断言或返回以下期望：

```text
t_user_auth active = 2
t_user_info active = 2
t_category active >= 2
t_tag active >= 3
t_article active >= 5
t_article_tag >= 5
t_comment active >= 2
t_site_config active = 1
```

- [ ] **Step 2: 编写事务种子**

`seed.sql` 使用固定开发 ID，并按依赖顺序插入：

```text
t_user_auth → t_user_info → t_category → t_tag →
t_article → t_article_tag → t_comment → t_site_config
```

要求：

- `START TRANSACTION` / `COMMIT` 包裹全部写入。
- 使用固定主键；若绕过初始化脚本直接重复执行，重复主键必须让事务失败并整体回滚。
- ADMIN/DEMO 只保存文档明确标注的开发 BCrypt hash，不保存真实 V1 密码。
- 覆盖 DRAFT、PUBLISHED、PRIVATE、PASSWORD、SCHEDULED 五种文章状态。
- 时间使用固定 JST 文本，保证截图和分页可复现。
- 评论数与 PASS 且未删除的文章评论数量一致。

- [ ] **Step 3: 静态校验**

Run:

```powershell
rg -n "START TRANSACTION|COMMIT" MyBlog-springboot-v2/scripts/dev/mysql/seed.sql
rg -n "password|secret|token" MyBlog-springboot-v2/scripts/dev/mysql/seed.sql
```

Expected: 仅命中说明性开发 hash，不出现明文生产凭据或 token。

- [ ] **Step 4: 检查并提交**

Commit: `建立V2本地开发种子数据`

### Task 3：实现安全初始化脚本

**Files:**
- Create: `MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1`
- Create: `MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1`
- Create: `MyBlog-springboot-v2/scripts/dev/mysql/verify.ps1`

- [ ] **Step 1: 写失败的脚本契约测试**

Create: `MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1`

使用纯 PowerShell 启动子进程并检查退出码和输出，覆盖：缺少 `MYBLOG_DATASOURCE_USERNAME`/`PASSWORD` 时退出非 0；数据库名不是 `myblog_v2_dev` 时拒绝执行；未显式传 `-Reset` 时不得执行 `DROP DATABASE`；目标表已有 active 数据时拒绝导入种子。

- [ ] **Step 2: 验证 RED**

Run: `pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1`

Expected: FAIL，初始化脚本尚不存在。

- [ ] **Step 3: 实现初始化流程**

脚本参数：

```powershell
param(
  [switch]$Reset,
  [switch]$SkipSeed
)
```

流程：检查 `mysql`、校验目标库名、可选重建库、使用 local profile 启动后端完成 Flyway、等待端口健康、停止后端、查询 `t_user_auth`、`t_article` 等目标表是否已有 active 数据、导入 `seed.sql`、执行 `verify.ps1`。未传 `-Reset` 且目标表非空时必须立即退出，不得调用 `seed.sql`。启动后台进程必须隐藏窗口，并在 `finally` 中终止本脚本创建的进程。

- [ ] **Step 4: 验证脚本契约并提交**

Run: `pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1`

Expected: PASS，且测试不需要连接真实 MySQL。

Commit: `编排V2本地MySQL初始化流程`

### Task 4：编写本地 MySQL 工作流文档

**Files:**
- Create: `docs/project-handbook/workflows/local-mysql-development.md`
- Modify: `docs/project-handbook/CLAUDE.md`
- Modify: `docs/project-handbook/INDEX.md`

- [ ] **Step 1: 写完整操作手册**

必须包含：MySQL 8 前置条件、创建最小权限本地账号、以下环境变量、初始化、重建、只验证、启动后端和前端命令：

```text
MYBLOG_DATASOURCE_URL
MYBLOG_DATASOURCE_USERNAME
MYBLOG_DATASOURCE_PASSWORD
MYBLOG_JWT_SECRET
MYBLOG_STATS_HASH_SECRET
```

文档不得给出真实密码；示例 secret 使用明确的占位字符串。

- [ ] **Step 2: 增加文档入口**

在 CLAUDE SOP 表和 INDEX 中链接该工作流。

- [ ] **Step 3: 检查并提交**

Commit: `记录本地MySQL联调工作流`

### Task 5：真实 MySQL 初始化与接口验收

**Files:**
- Modify: `docs/project-handbook/status.md`

- [ ] **Step 1: 请求或读取本机环境变量**

不得打印密码和 secrets。若变量缺失，停止真实 MySQL 阶段，但仍可保留已完成的配置、脚本和文档提交。

- [ ] **Step 2: 初始化空库**

Run: `./scripts/dev/mysql/initialize.ps1 -Reset`

Expected: Flyway schema version 2，种子校验全部满足。

- [ ] **Step 3: 启动后端并验收接口**

验证：ADMIN/DEMO login、`/api/auth/me`、refresh、公开站点配置、公开文章列表/详情、后台文章筛选/分页。

- [ ] **Step 4: 启动前台和后台联调**

前台不得读取旧静态 JSON；后台文章列表不得使用 QA mock 或测试假数据。

- [ ] **Step 5: 完整回归**

Run:

```powershell
cd MyBlog-springboot-v2; mvn clean test
cd ../frontend/apps/blog; corepack pnpm test; corepack pnpm typecheck; corepack pnpm build
cd ../admin; corepack pnpm test; corepack pnpm typecheck; corepack pnpm build
```

- [ ] **Step 6: 更新状态并提交**

记录 MySQL 版本、Flyway 版本、表数量、接口验收和自动化测试结果，不记录凭据。

Commit: `记录V2本地MySQL基线验收结果`

## 自审结果

- local profile 当前禁用 Flyway 的真实差距已进入首个 TDD 任务。
- 种子数据与生产 Flyway 分离，不会污染部署 schema 历史。
- 初始化脚本默认不删除数据库，重建必须显式 `-Reset`。
- H2 完整回归仍是每阶段质量门禁，MySQL只补充真实方言与联调风险。
- 正式 V1 数据、附件和隐私数据不进入本计划。
