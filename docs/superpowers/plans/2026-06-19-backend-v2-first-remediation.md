# 后端 V2 第一批审查问题修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 local Profile 公开端点覆盖、MySQL 测试临时表污染和已确认的阶段文档漂移，使后端可以稳定支持本地前端联调。

**Architecture:** 将公开端点拆成所有环境共享的 `public-endpoints` 与 Profile 专用的 `additional-public-endpoints`，由类型化配置统一合并后交给 Spring Security。测试夹具继续与生产 Flyway 隔离，并在测试结束阶段显式清理；文档只同步已由代码和 fresh 测试证明的事实。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、JUnit 5、AssertJ、Spring Test SQL、Maven、H2、MySQL 8、PowerShell

---

## 执行边界

- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`
- 后端目录：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`
- 当前分支：`backend-v2-refactor`
- 开始实施前先检查 `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`；如果它存在未提交格式化变更，必须保留，不得还原、格式化、暂存或提交。
- 每次 `git add` 必须使用任务中列出的精确路径，禁止使用 `git add .`、`git add -A` 或整目录暂存。
- 不实现 PASSWORD 解锁，不调整 ADMIN/DEMO 可见字段，不处理 Web → Domain 架构迁移，不开展全量 OpenAPI/Javadoc 补齐。
- MySQL 命令只允许连接用户授权的 `myblog_v2_dev`；凭据必须来自进程环境变量，不得写入文件、命令历史、计划或提交消息。

### Task 1: 组合基础与 Profile 增量公开端点

**Files:**
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/SecurityPublicEndpointPropertiesTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/BackendPropertiesTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/RuntimeProfileConfigurationTest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/SecurityPublicEndpointProperties.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

- [ ] **Step 1: 写配置合并 RED 单元测试**

创建 `SecurityPublicEndpointPropertiesTest.java`：

```java
package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPublicEndpointPropertiesTest {

    @Test
    void mergesBaseAndAdditionalEndpointsAndNormalizesMissingLists() {
        var base = new SecurityPublicEndpointProperties.PublicEndpoint(
                "GET", "/api/public/site-config");
        var additional = new SecurityPublicEndpointProperties.PublicEndpoint(
                "GET", "/api/public/security-probe");

        var properties = new SecurityPublicEndpointProperties(
                List.of(base),
                List.of(additional));

        assertThat(properties.allPublicEndpoints())
                .containsExactly(base, additional);
        assertThat(new SecurityPublicEndpointProperties(null, null)
                .allPublicEndpoints())
                .isEmpty();
    }
}
```

- [ ] **Step 2: 让绑定测试断言最终合并结果**

在 `BackendPropertiesTest.bindsPublicEndpoints()` 中把：

```java
assertThat(securityProperties.publicEndpoints())
```

改为：

```java
assertThat(securityProperties.allPublicEndpoints())
```

现有 `containsExactly(...)` 的 19 个 method + path 断言保持不变，它同时保护 13 个基础端点与 6 个 test Profile 增量端点。

- [ ] **Step 3: 运行 RED 测试并确认失败原因正确**

在 `MyBlog-springboot-v2/` 运行：

```powershell
mvn '-Dtest=SecurityPublicEndpointPropertiesTest,BackendPropertiesTest' test
```

Expected: FAIL at test compilation because `SecurityPublicEndpointProperties` 尚无双列表构造器和 `allPublicEndpoints()`；不得接受与目标无关的失败。

- [ ] **Step 4: 实现不可变的公开端点合并模型**

将 `SecurityPublicEndpointProperties` 的声明和主体改为：

```java
@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(
        List<PublicEndpoint> publicEndpoints,
        List<PublicEndpoint> additionalPublicEndpoints) {

    public SecurityPublicEndpointProperties {
        publicEndpoints = immutable(publicEndpoints);
        additionalPublicEndpoints = immutable(additionalPublicEndpoints);
    }

    /**
     * 返回基础业务端点与当前 Profile 增量端点的有序并集。
     */
    public List<PublicEndpoint> allPublicEndpoints() {
        return java.util.stream.Stream.concat(
                        publicEndpoints.stream(),
                        additionalPublicEndpoints.stream())
                .toList();
    }

    private static List<PublicEndpoint> immutable(
            List<PublicEndpoint> endpoints) {
        return endpoints == null ? List.of() : List.copyOf(endpoints);
    }

    // 保留现有 PublicEndpoint record 及 httpMethod() 实现。
}
```

同步更新类 Javadoc 的 record 参数说明：

```java
 * @param publicEndpoints           所有环境共享的匿名业务端点
 * @param additionalPublicEndpoints 当前 Profile 追加的匿名端点
```

- [ ] **Step 5: 让 Spring Security 使用合并后的唯一来源**

在 `SecurityConfig.apiSecurity(...)` 中把：

```java
publicEndpointProperties.publicEndpoints().forEach(endpoint ->
```

改为：

```java
publicEndpointProperties.allPublicEndpoints().forEach(endpoint ->
```

其余 matcher 顺序和 ADMIN/DEMO 规则不得调整。

- [ ] **Step 6: 把 local Profile 改成只声明增量端点**

在 `application-local.yml` 中，将 `myblog.security.public-endpoints` 整段替换为：

```yaml
    additional-public-endpoints:
      - method: GET
        path: /api/public/security-probe
      - method: GET
        path: /doc.html
      - method: GET
        path: /webjars/**
      - method: GET
        path: /v3/api-docs/**
      - method: GET
        path: /swagger-ui/**
      - method: GET
        path: /media/**
```

不要在 local 文件中重复 actuator、auth 或任何公开业务端点；它们继续来自 `application.yml`。

- [ ] **Step 7: 把 test Profile 改成只声明增量端点**

在 `src/test/resources/application-test.yml` 中，将完整 `public-endpoints` 列表替换成与 Step 6 相同的 `additional-public-endpoints` 六项列表。

- [ ] **Step 8: 增加 YAML 防漂移断言**

在 `RuntimeProfileConfigurationTest` 中新增：

```java
@Test
void profilesDeclareOnlyAdditionalPublicEndpoints() throws Exception {
    PropertySource<?> application = load("application.yml");
    PropertySource<?> local = load("application-local.yml");
    PropertySource<?> test = load("application-test.yml");

    assertThat(application.getProperty(
            "myblog.security.public-endpoints[3].path"))
            .isEqualTo("/api/public/site-config");
    assertThat(application.getProperty(
            "myblog.security.public-endpoints[9].path"))
            .isEqualTo("/api/public/articles/*/comments");
    assertThat(application.getProperty(
            "myblog.security.public-endpoints[10].path"))
            .isEqualTo("/api/public/guestbook/comments");

    assertThat(local.getProperty(
            "myblog.security.public-endpoints[0].path")).isNull();
    assertThat(test.getProperty(
            "myblog.security.public-endpoints[0].path")).isNull();
    assertThat(local.getProperty(
            "myblog.security.additional-public-endpoints[0].path"))
            .isEqualTo("/api/public/security-probe");
    assertThat(test.getProperty(
            "myblog.security.additional-public-endpoints[5].path"))
            .isEqualTo("/media/**");
}
```

- [ ] **Step 9: 运行 GREEN 定向测试**

运行：

```powershell
mvn '-Dtest=SecurityPublicEndpointPropertiesTest,BackendPropertiesTest,RuntimeProfileConfigurationTest,ApplicationConfigurationTest,SecurityConfigTest' test
```

Expected: BUILD SUCCESS；测试 Profile 的基础端点和增量 probe/docs/media 均可匿名访问，method 不匹配仍返回 401。

- [ ] **Step 10: 检查差异并提交公开端点修复**

在 worktree 根目录运行：

```powershell
git diff --check
git status --short
git add -- MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/SecurityPublicEndpointPropertiesTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/BackendPropertiesTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/RuntimeProfileConfigurationTest.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/SecurityPublicEndpointProperties.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml
git diff --cached --check
git commit -m "修复Profile公开端点配置合并"
```

Expected: 提交不包含 `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`。

### Task 2: 清理审计字段集成测试临时表

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/infrastructure/persistence/audit/AuditFieldHandlerIntegrationTest.java`

- [ ] **Step 1: 用 MySQL 元数据复现现有污染**

确认调用进程已经通过环境变量提供 MySQL 凭据，然后在 `MyBlog-springboot-v2/` 运行：

```powershell
if (-not $env:SPRING_DATASOURCE_PASSWORD) {
    throw '请先在当前进程设置 SPRING_DATASOURCE_PASSWORD'
}
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/myblog_v2_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&connectionTimeZone=Asia/Tokyo&forceConnectionTimeToSession=true&sessionVariables=time_zone='%2B09:00'"
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
$env:SPRING_FLYWAY_ENABLED = 'true'
mvn '-Dtest=AuditFieldHandlerIntegrationTest' test
```

再运行只读查询：

```powershell
if (-not $env:MYSQL_PWD) {
    throw '请先在当前进程设置 MYSQL_PWD'
}
& 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe' `
    -h 127.0.0.1 -P 3306 -u root --batch --skip-column-names `
    myblog_v2_dev `
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='myblog_v2_dev' AND table_name='t_audit_update_test';"
```

Expected RED: Maven 测试通过，但查询返回 `1`，证明测试结束后仍遗留临时表。

- [ ] **Step 2: 增加测试结束阶段清理**

在现有建表 `@Sql` 后新增：

```java
@Sql(
        statements = "drop table if exists t_audit_update_test",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
```

保留原有 `@AfterEach clearSecurityContext()`；数据库夹具与 SecurityContext 分别清理。

- [ ] **Step 3: 在 H2 上验证测试仍通过**

清除 MySQL 数据源环境变量，避免误判运行目标：

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_DRIVER_CLASS_NAME -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_FLYWAY_ENABLED -ErrorAction SilentlyContinue
mvn '-Dtest=AuditFieldHandlerIntegrationTest' test
```

Expected: BUILD SUCCESS。

- [ ] **Step 4: 在 MySQL 上验证测试后表不存在**

重新按 Step 1 设置 Spring 数据源环境变量，运行：

```powershell
mvn '-Dtest=AuditFieldHandlerIntegrationTest' test
```

随后重复 Step 1 的 `information_schema.tables` 查询。

Expected GREEN: Maven BUILD SUCCESS，查询返回 `0`。

- [ ] **Step 5: 检查差异并提交测试隔离修复**

在 worktree 根目录运行：

```powershell
git diff --check
git add -- MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/infrastructure/persistence/audit/AuditFieldHandlerIntegrationTest.java
git diff --cached --check
git commit -m "清理审计集成测试临时表"
```

Expected: 单文件测试提交，不包含审查报告格式化差异。

### Task 3: 同步当前阶段文档与准确注释

**Files:**
- Modify: `docs/project-handbook/CLAUDE.md`
- Modify: `docs/project-handbook/arch/module-map.md`
- Modify: `docs/project-handbook/rules/testing-policy.md`
- Modify: `docs/project-handbook/rules/sql-placement.md`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/MyBatisPlusConfig.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/UserAgentResolver.java`

- [ ] **Step 1: 先取得本批代码后的 fresh H2 测试总数**

在 `MyBlog-springboot-v2/` 清除 MySQL 数据源环境变量后运行：

```powershell
mvn clean test
```

Expected: BUILD SUCCESS，0 failures，0 errors，4 skipped；新增 1 个配置单元测试后预期总数为 614。若实际总数不同，以 Surefire 的 fresh 汇总为准，不得照抄预期值。

- [ ] **Step 2: 更新 AI 工作入口的当前阶段**

在 `CLAUDE.md` 中把 V2 后端状态改为：

```markdown
- **V2 后端**：`MyBlog-springboot-v2/` — 六个业务模块已完成第一版，当前进入审查问题修复与前端联调支持
- **V2 前台 / 后台**：进入 M4 前端工程骨架与接口联调阶段；规格分别在 `frontend-user/` 和 `frontend-admin/`
```

把“当前阶段”段落改为：

```markdown
当前阶段：**M4 前端骨架与后端联调准备**。M3 六个后端模块第一版已经完成并通过 H2/MySQL 回归；当前先处理发布前审查确认的联调阻塞项，再进入前台与后台工程骨架，详见 `status.md` / `roadmap.md` / `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`。
```

把“十、当前焦点”改为：

```markdown
- **进行中**：修复后端发布前审查确认的联调阻塞与测试隔离问题
- **下一步**：启动 M4 前端工程骨架，优先联调登录、公开站点配置和非 PASSWORD 文章链路
- **后续裁决**：PASSWORD 解锁、DEMO 敏感字段和 Web → Domain 边界单独设计，不在联调修复中顺带改变
```

- [ ] **Step 3: 更新模块状态说明**

把 `arch/module-map.md` 第 6 节状态替换为：

```markdown
**当前 V2 实现状态**：顶级 `infrastructure` 已删除，公共数据库基础设施归入 `common.infrastructure`；`common`、`identity`、`content`、`comment`、`system`、`stats` 六个模块第一版均已建立。发布前审查确认的架构规则缺口按独立批次处理，不改变本节的模块完成状态。
```

- [ ] **Step 4: 用 fresh 结果更新测试策略**

把 `rules/testing-policy.md` 顶部最近全量结果中的 `612 tests` 替换为 Step 1 得到的实际测试总数，并保持 `0 failures，0 errors，4 skipped` 与真实输出一致。

将“已知缺测”列表改为：

```markdown
**已知缺测**：
- `AdminCommentCommandService` 真实 HTTP/事务集成测试
- 评论软删除 → 恢复完整链路测试
- local Profile 与基础公开端点合并后的真实启动回归
```

不要保留已经不存在的 `CommentCommandService`。

- [ ] **Step 5: 修正 SQL 规则的 ADR 链接**

把 `rules/sql-placement.md:5` 改为：

```markdown
> 相关 ADR：`../decisions/0005-mybatis-plus-as-primary-orm.md`、`../decisions/0010-sql-placement-strategy.md`
```

本批不重写旧库兼容示例或 projection 规则；这些内容属于后续规则质量批次。

- [ ] **Step 6: 修正 MyBatis 配置类的阶段性注释**

将 `MyBatisPlusConfig` 类 Javadoc 改为：

```java
/**
 * MyBatis-Plus 基础配置。
 *
 * <p>Mapper 扫描限定为带 {@link Mapper} 注解的接口，避免把 domain 层业务端口
 * 误注册为数据库 Mapper。业务 Mapper 统一位于各模块的
 * {@code infrastructure.persistence.mapper} 包。</p>
 */
```

- [ ] **Step 7: 修正 User-Agent 长度注释**

将 `UserAgentResolver.MAX_LENGTH` 的 Javadoc 改为：

```java
/**
 * User-Agent 最大入库长度。
 *
 * <p>与当前登录和评论审计字段上限保持一致，同时避免异常请求头放大数据库写入。</p>
 */
```

- [ ] **Step 8: 验证过时文本和链接已经消失**

在 worktree 根目录运行：

```powershell
rg -n "当前阶段：\*\*M3|identity 后台登录最小纵向切片|612 tests|CommentCommandService|0005-mybatis-plus-as-orm|0010-sql-placement\.md|不在本任务迁移现有|旧库或审计表字段长度" docs/project-handbook/CLAUDE.md docs/project-handbook/arch/module-map.md docs/project-handbook/rules/testing-policy.md docs/project-handbook/rules/sql-placement.md MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/MyBatisPlusConfig.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/UserAgentResolver.java
```

Expected: 对本任务列出的过时文本无匹配；不扫描或修改历史设计/计划。

验证 ADR 文件存在：

```powershell
Test-Path docs/project-handbook/decisions/0005-mybatis-plus-as-primary-orm.md
Test-Path docs/project-handbook/decisions/0010-sql-placement-strategy.md
```

Expected: 两项均为 `True`。

- [ ] **Step 9: 编译并运行架构守护**

在 `MyBlog-springboot-v2/` 运行：

```powershell
mvn '-Dtest=ArchitectureRulesTest,SecurityPublicEndpointPropertiesTest,BackendPropertiesTest,RuntimeProfileConfigurationTest' test
```

Expected: BUILD SUCCESS，ArchUnit 29 项继续通过。

- [ ] **Step 10: 检查差异并提交文档同步**

在 worktree 根目录运行：

```powershell
git diff --check
git add -- docs/project-handbook/CLAUDE.md docs/project-handbook/arch/module-map.md docs/project-handbook/rules/testing-policy.md docs/project-handbook/rules/sql-placement.md MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/MyBatisPlusConfig.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/UserAgentResolver.java
git diff --cached --check
git commit -m "同步后端V2当前阶段文档"
```

Expected: 提交只包含上述六个文件，不包含审查报告格式化差异。

### Task 4: 完成 H2、MySQL 与 local Profile 验收

**Files:**
- Verify only: `MyBlog-springboot-v2/**`
- Preserve unstaged: `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **Step 1: 运行 fresh H2 全量测试**

在 `MyBlog-springboot-v2/` 清除 MySQL 数据源环境变量后运行：

```powershell
mvn clean test
```

Expected: BUILD SUCCESS，0 failures，0 errors，4 skipped；预期 614 tests，最终以 fresh Surefire 汇总为准。

- [ ] **Step 2: 运行本地 MySQL 广泛回归**

确认当前进程已设置 `SPRING_DATASOURCE_PASSWORD`，再设置其余非敏感环境变量并运行：

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/myblog_v2_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&connectionTimeZone=Asia/Tokyo&forceConnectionTimeToSession=true&sessionVariables=time_zone='%2B09:00'"
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
$env:SPRING_FLYWAY_ENABLED = 'true'
mvn '-Dtest=**/*Test,!FlywayMigrationTest,!RefreshSessionTransactionIntegrationTest,!DatabasePasswordAccountRepositoryTest,!DatabaseUserProfileRepositoryTest' test
```

Expected: BUILD SUCCESS，0 failures，0 errors，4 skipped；新增配置单元测试后预期 598 tests。日志必须明确显示连接 `jdbc:mysql://localhost:3306/myblog_v2_dev`。

- [ ] **Step 3: 验证 MySQL 测试库没有临时表**

运行：

```powershell
& 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe' `
    -h 127.0.0.1 -P 3306 -u root --batch --skip-column-names `
    myblog_v2_dev `
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='myblog_v2_dev' AND table_name='t_audit_update_test';"
```

Expected: `0`。

- [ ] **Step 4: 打包并以 local Profile 启动真实应用**

确认 `MYBLOG_DATASOURCE_USERNAME`、`MYBLOG_DATASOURCE_PASSWORD`、`MYBLOG_JWT_SECRET` 和 `MYBLOG_STATS_HASH_SECRET` 已在当前进程设置，然后运行：

```powershell
mvn -DskipTests package
$app = Start-Process `
    -FilePath 'java' `
    -ArgumentList @(
        '-Duser.timezone=Asia/Tokyo',
        '-jar',
        'target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar',
        '--spring.profiles.active=local',
        '--server.port=18080') `
    -PassThru `
    -WindowStyle Hidden
```

等待健康检查，最多 60 秒：

```powershell
$ready = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    if ((curl.exe -s -o NUL -w '%{http_code}' `
            'http://localhost:18080/actuator/health') -eq '200') {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $ready) { throw 'local Profile 未在 60 秒内就绪' }
```

- [ ] **Step 5: 验证四个联调端点不再返回 401**

运行：

```powershell
$statuses = [ordered]@{
    siteConfig = curl.exe -s -o NUL -w '%{http_code}' `
        'http://localhost:18080/api/public/site-config'
    articleComment = curl.exe -s -o NUL -w '%{http_code}' `
        -X POST -H 'Content-Type: application/json' -d '{}' `
        'http://localhost:18080/api/public/articles/1/comments'
    guestbookGet = curl.exe -s -o NUL -w '%{http_code}' `
        'http://localhost:18080/api/public/guestbook/comments'
    guestbookPost = curl.exe -s -o NUL -w '%{http_code}' `
        -X POST -H 'Content-Type: application/json' -d '{}' `
        'http://localhost:18080/api/public/guestbook/comments'
}
$statuses
if ($statuses.Values -contains '401') {
    throw '公开业务端点仍被认证层拒绝'
}
```

Expected: `siteConfig` 和 `guestbookGet` 为业务响应；两个空 POST 可以因参数校验返回 400，但四项都不得为 401。

如果断言失败，先保存 `$statuses` 作为证据，仍必须执行 Step 6 停止进程，然后再报告失败。

- [ ] **Step 6: 停止 local 应用并确认端口释放**

无论 Step 5 是否成功，都运行：

```powershell
if ($app -and -not $app.HasExited) {
    Stop-Process -Id $app.Id -Force
    Wait-Process -Id $app.Id -ErrorAction SilentlyContinue
}
if (Get-NetTCPConnection -LocalPort 18080 -State Listen `
        -ErrorAction SilentlyContinue) {
    throw '端口 18080 仍被占用，请定位并停止本次验证进程'
}
```

- [ ] **Step 7: 最终检查提交范围与工作区**

在 worktree 根目录运行：

```powershell
git log -4 --oneline
git status --short
git diff --check
```

Expected:

- 最近三个实现提交依次对应公开端点合并、测试临时表清理、当前阶段文档同步；
- 没有未提交的实现文件；
- 如果实施前已存在 `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md` 格式化差异，只允许保留该既有差异；
- 不合并、不推送、不删除当前分支或 worktree。
