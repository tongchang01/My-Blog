# 首个管理员初始化与管理端改密实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 本地空库可使用默认管理员登录，生产可通过一次性非 Web 命令创建首个管理员，管理端可安全修改当前管理员密码。

**Architecture:** 使用专用 `AdminBootstrapRepository`、既有 `UserProfileRepository` 和 `PasswordHashService`：初始化仓储复用 MyBatis Mapper，但不改变单方法的登录 `UserAccountRepository`，由一个事务化的 bootstrap 应用服务编排。`ApplicationRunner` 只在显式开关开启时运行；本地 profile 开启已知开发默认值，生产以临时环境变量和非 Web 运行模式调用。管理端复用个人资料页、现有改密 API 和 `sessionService.expire()`。

**Tech Stack:** Java 17、Spring Boot 3.5、MyBatis-Plus、Flyway、H2/Testcontainers、Vue 3、TypeScript、Pinia、Element Plus、Vitest。

## Global Constraints

- 不修改 `V1__init.sql`，不在迁移、镜像或公开生产文档中写入生产密码。
- 本地 `admin / 12345678` 只由 `local` profile 提供；生产密码只来自临时 root-only `bootstrap.env`。
- 初始化只在没有有效 ADMIN 时创建账号和资料，绝不覆盖已有密码或资料。
- 改密只调用既有 `PUT /api/auth/me/password`；成功后直接清理会话，不再调用会失败的 logout API。
- 不实现账号管理、找回密码、邮件验证、强制改密字段或新的公开 HTTP 初始化接口。
- 每项任务完成后先运行局部测试，再进行单一目的的中文提交。

---

### Task 1：补齐管理员创建的持久化端口

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/bootstrap/AdminBootstrapRepository.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserAccountEntity.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisAdminBootstrapRepository.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseUserAccountRepositoryTest.java`

**Interfaces:**

- Consumes: `AccountType.ADMIN`、MyBatis-Plus `BaseMapper.insert`、`BaseEntity` 的雪花 ID 与审计自动填充。
- Produces: `AdminBootstrapRepository.existsActiveAdmin()` 和 `AdminBootstrapRepository.createAdmin(String username, String passwordHash)`，供 bootstrap 应用服务调用；既有 `UserAccountRepository` 不改动。

- [ ] **Step 1: 写入数据库仓储的失败测试**

在 `DatabaseUserAccountRepositoryTest` 新增两个测试：空表时 `existsActiveAdmin()` 为 false；调用 `createAdmin("admin", "$2a$10$test-password-hash")` 后，返回账号类型为 ADMIN，数据库同时存在未删除账号记录，且 `existsActiveAdmin()` 为 true。

```java
@Test
void createsAdminWithGeneratedIdAndDetectsIt() {
    assertTrue(userAccountRepository.existsActiveAdmin() == false);

    UserAccount created = userAccountRepository.createAdmin(
            "admin", "$2a$10$test-password-hash");

    assertTrue(created.id() > 0);
    assertEquals(AccountType.ADMIN, created.type());
    assertTrue(userAccountRepository.existsActiveAdmin());
    assertEquals("admin", jdbcTemplate.queryForObject(
            "SELECT username FROM t_user_auth WHERE id = ?", String.class, created.id()));
}
```

- [ ] **Step 2: 运行失败测试，确认端口尚不存在**

Run:

```powershell
Set-Location MyBlog-springboot-v2
mvn -Dtest=DatabaseUserAccountRepositoryTest test
```

Expected: 编译失败，提示 `existsActiveAdmin` 或 `createAdmin` 未定义。

- [ ] **Step 3: 最小实现端口、实体与 XML 查询**

让 `UserAccountEntity` 继承 `BaseEntity`，移除其 `IdType.INPUT` 的重复主键字段，使 MyBatis-Plus 在 `mapper.insert` 时生成雪花 ID。新增专用仓储端口与 MyBatis 适配器，并只向 Mapper 和 XML 添加以下查询能力：

```java
boolean existsActiveAdmin();

UserAccount createAdmin(String username, String passwordHash);
```

```xml
<select id="existsActiveAdmin" resultType="boolean">
    SELECT EXISTS(
        SELECT 1 FROM t_user_auth
        WHERE type = 1 AND deleted = 0
    )
</select>
```

`MyBatisAdminBootstrapRepository.createAdmin` 创建 `UserAccountEntity`，设置用户名、密码摘要、`AccountType.ADMIN.databaseValue()`、`tokenVersion = 0`、`loginFailCount = 0`，调用 `mapper.insert(entity)` 并验证影响行数为 1；以生成的 ID 返回 `UserAccount`。不得设置审计人或密码明文。

- [ ] **Step 4: 运行测试确认通过**

Run:

```powershell
Set-Location MyBlog-springboot-v2
mvn -Dtest=DatabaseUserAccountRepositoryTest test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交持久化能力**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseUserAccountRepositoryTest.java
git commit -m "功能：支持创建首个管理员账号"
```

### Task 2：实现本地与生产一次性管理员初始化

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/BootstrapAdminProperties.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/bootstrap/BootstrapAdminApplicationService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/bootstrap/BootstrapAdminRunner.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/MyBlogV2Application.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/bootstrap/BootstrapAdminApplicationServiceTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration/BootstrapAdminIntegrationTest.java`

**Interfaces:**

- Consumes: Task 1 的 `AdminBootstrapRepository`、既有 `UserProfileRepository.insert`、`PasswordHashService` 和 `UserProfile.create`。
- Produces: `boolean BootstrapAdminApplicationService.bootstrap(BootstrapAdminProperties properties)`；`BootstrapAdminRunner` 仅在 `myblog.bootstrap-admin.enabled=true` 时执行。

- [ ] **Step 1: 写入应用服务单测**

使用 Mockito 覆盖三种行为：已有管理员时不调用创建；空库时将 `admin` 和密码摘要交给账号仓储、创建昵称为 `admin` 的资料；空白用户名或长度小于 8 的密码抛出 `IllegalArgumentException`，且不写入仓储。

```java
when(accountRepository.existsActiveAdmin()).thenReturn(false);
when(accountRepository.createAdmin("admin", "hash"))
        .thenReturn(new UserAccount(1001L, "admin", "hash", AccountType.ADMIN, 0, 0, null));
when(passwordHashService.encode("12345678")).thenReturn("hash");

assertThat(service.bootstrap(new BootstrapAdminProperties(true, "admin", "12345678", false)))
        .isTrue();
verify(profileRepository).insert(UserProfile.create(1001L, "admin", null, null, null, null, null, null, null, null, null, null, null, null, null));
```

- [ ] **Step 2: 运行单测，确认失败**

Run:

```powershell
Set-Location MyBlog-springboot-v2
mvn -Dtest=BootstrapAdminApplicationServiceTest test
```

Expected: 测试类或 bootstrap 类型不存在而失败。

- [ ] **Step 3: 实现配置、事务服务和运行器**

创建配置记录：

```java
@ConfigurationProperties("myblog.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String username,
        String password,
        boolean exitAfterRun
) { }
```

服务在单个 `@Transactional` 方法中校验：用户名 trim 后非空且不超过 64，密码非空且长度 8 至 128。已有有效 ADMIN 返回 `false`；否则调用 `PasswordHashService.encode`、Task 1 的 `createAdmin`，再调用 `UserProfileRepository.insert` 并返回 `true`。日志只能写入 `CREATED` 或 `SKIPPED` 与用户名，绝不写入密码或摘要。

运行器使用：

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "myblog.bootstrap-admin", name = "enabled", havingValue = "true")
class BootstrapAdminRunner implements ApplicationRunner {
    private final BootstrapAdminApplicationService service;
    private final BootstrapAdminProperties properties;

    @Override
    public void run(ApplicationArguments arguments) {
        service.bootstrap(properties);
    }
}
```

`MyBlogV2Application.main` 在 `SpringApplication.run` 返回后，仅当 `exitAfterRun` 为 true 时执行 `SpringApplication.exit(context)` 后退出；这让生产命令以非 Web 模式稳定结束，而 local profile 保持正常 Web 服务。默认 `application.yml` 将 enabled/exit-after-run 设为 false；`application-local.yml` 设置 enabled true、username `admin`、password `12345678`、exit-after-run false；`application-test.yml` 显式保持 disabled。

- [ ] **Step 4: 添加 H2 集成验证**

在 `BootstrapAdminIntegrationTest` 使用 `test` profile 并通过 `TestPropertySource` 开启 bootstrap。验证第一次启动后可通过 `/api/auth/login` 以创建的凭据登录，第二次调用不改变数据库中已有密码摘要；另验证缺少密码时应用启动失败且账号表仍为空。

- [ ] **Step 5: 运行后端局部验证**

Run:

```powershell
Set-Location MyBlog-springboot-v2
mvn -Dtest=BootstrapAdminApplicationServiceTest,BootstrapAdminIntegrationTest,ChangePasswordIntegrationTest test
```

Expected: `BUILD SUCCESS`，且测试输出不出现 `12345678` 或 BCrypt 摘要。

- [ ] **Step 6: 提交初始化能力**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2 MyBlog-springboot-v2/src/main/resources/application*.yml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/bootstrap MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration/BootstrapAdminIntegrationTest.java MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "功能：支持本地与生产首个管理员初始化"
```

### Task 3：在个人资料页接入现有改密接口

**Files:**

- Create: `frontend/apps/admin/src/features/profile/password-form.ts`
- Create: `frontend/apps/admin/src/features/profile/password-form.test.ts`
- Modify: `frontend/apps/admin/src/api/auth.ts`
- Modify: `frontend/apps/admin/src/api/auth.test.ts`
- Modify: `frontend/apps/admin/src/features/profile/useProfileManagement.ts`
- Modify: `frontend/apps/admin/src/features/profile/useProfileManagement.test.ts`
- Modify: `frontend/apps/admin/src/features/profile/index.vue`
- Modify: `frontend/apps/admin/src/features/profile/index.test.ts`
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`

**Interfaces:**

- Consumes: `PUT /api/auth/me/password`、`sessionService.expire()`、当前 `ProfileManagement` 页面与 `CurrentUser.type`。
- Produces: `ChangePasswordPayload`、`changeCurrentUserPassword(payload)` 和仅 ADMIN 可见的密码卡片。

- [ ] **Step 1: 写入前端 API 与表单失败测试**

在 `auth.test.ts` 断言新函数向 `/api/auth/me/password` 发出 PUT，body 仅含 `currentPassword` 和 `newPassword`。在 `password-form.test.ts` 覆盖空值、长度不足、两次密码不一致与合法值。

```ts
await expect(
  changeCurrentUserPassword({
    currentPassword: "old-password",
    newPassword: "new-password"
  })
).resolves.toMatchObject({ data: null });
```

```ts
expect(validatePasswordForm({
  currentPassword: "old-password",
  newPassword: "new-password",
  confirmPassword: "different-password"
})).toEqual({ confirmPassword: "mismatch" });
```

- [ ] **Step 2: 运行前端失败测试**

Run:

```powershell
Set-Location frontend/apps/admin
pnpm test -- auth.test.ts password-form.test.ts
```

Expected: 导入的改密函数和密码表单模块不存在而失败。

- [ ] **Step 3: 实现 API、纯表单校验和页面状态**

在 `auth.ts` 添加：

```ts
export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const changeCurrentUserPassword = (payload: ChangePasswordPayload) =>
  http.request<ApiResponse<null>>(
    "put",
    "/api/auth/me/password",
    { data: payload }
  );
```

`password-form.ts` 只导出表单类型、`createPasswordForm()` 和 `validatePasswordForm()`；不把密码写入 Pinia、URL 或 localStorage。`useProfileManagement` 扩展默认 API 和状态，提供 `changePassword()`：本地校验失败或 HTTP 失败时返回 false 并保留会话；HTTP 成功时清空三个字段并返回 true。

页面在资料表单下增加 ADMIN 专用 `profile-password-card`，以密码输入框展示当前密码、新密码、确认新密码。点击成功后调用 `sessionService.expire()`、显示成功消息并 `router.replace("/login")`；不调用 `sessionService.signOut()`。DEMO 只保留现有只读提示，不渲染密码卡片或提交按钮。三套 locale 同时添加密码卡片标题、字段、校验和成功/失败文案。

- [ ] **Step 4: 补齐组件与状态测试**

扩展 `useProfileManagement.test.ts`：成功时 `changeCurrentUserPassword` 接收正确 payload 且密码字段清空；失败时字段保留且 session 清理函数不被调用。扩展 `index.test.ts`：ADMIN 渲染 `profile-password-card`，DEMO 不渲染；成功点击后请求 PUT 并跳转 `/login`。

- [ ] **Step 5: 运行前端局部验证**

Run:

```powershell
Set-Location frontend/apps/admin
pnpm test -- auth.test.ts password-form.test.ts useProfileManagement.test.ts index.test.ts
pnpm typecheck
```

Expected: Vitest 全部通过，typecheck 无错误。

- [ ] **Step 6: 提交管理端改密入口**

```powershell
git add frontend/apps/admin/src/api/auth.ts frontend/apps/admin/src/api/auth.test.ts frontend/apps/admin/src/features/profile frontend/apps/admin/locales
git commit -m "功能：管理端支持修改当前密码"
```

### Task 4：同步部署契约并完成阶段验证

**Files:**

- Modify: `docs/handbook/ops/environment.md`
- Modify: `docs/handbook/ops/production-runbook.md`

**Interfaces:**

- Consumes: Task 2 的 `MYBLOG_BOOTSTRAP_ADMIN_ENABLED`、`MYBLOG_BOOTSTRAP_ADMIN_USERNAME`、`MYBLOG_BOOTSTRAP_ADMIN_PASSWORD`、`MYBLOG_BOOTSTRAP_ADMIN_EXIT_AFTER_RUN` 和 Task 3 的页面行为。
- Produces: 可执行的一次性初始化变量契约和部署步骤；发布清单与开放问题由已推送的文档分支维护。

- [ ] **Step 1: 更新环境变量和生产运行手册**

`environment.md` 说明 bootstrap 变量只用于一次性非 Web 命令，正常 `api` 服务不得保留它们。`production-runbook.md` 给出 root-only `bootstrap.env`、`docker compose run --rm --no-deps api --spring.main.web-application-type=none` 的执行顺序、成功日志判定和删除文件动作；不得把生产密码写入示例。

- [ ] **Step 2: 运行文档和完整代码验证**

Run:

```powershell
Set-Location E:\My-Blog\.worktrees\feature-bootstrap-admin-password
git diff --check
Set-Location MyBlog-springboot-v2
mvn clean test
Set-Location ..\frontend\apps\admin
pnpm test
pnpm typecheck
pnpm build
```

Expected: 差异检查、后端完整测试、前端测试、类型检查和生产构建均通过。

- [ ] **Step 3: 提交文档与验证结果**

```powershell
git add docs/handbook
git commit -m "文档：补充管理员初始化部署说明"
git status --short
```

## 验收回顾

- 本地空库可用 `admin / 12345678` 登录，重复启动不重置密码。
- 生产初始化只在临时 root-only 文件存在时运行，创建后可安全删除该文件。
- 管理端 ADMIN 可改密码，DEMO 不可见；成功改密后页面回到登录页，旧 token 无法继续使用。
- 生产初始化密码、BCrypt 摘要和 root-only 环境文件均未写入 Git、日志或前端状态。
