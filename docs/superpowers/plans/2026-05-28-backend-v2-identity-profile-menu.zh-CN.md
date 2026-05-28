# 后端 V2 身份资料与后台菜单实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项执行本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 在真实账号登录和登录审计之后，补齐后端 V2 当前用户资料读取与后台菜单读取能力，让后台 V2 后续有稳定的身份接口和菜单接口可接入。

**架构：** 继续保持模块化单体边界。`identity` 模块新增查询用例，应用层只依赖领域端口；基础设施层用 `JdbcTemplate` 读取旧表；API 层只做 HTTP 入参、当前用户注入和响应转换。本阶段不恢复旧系统的动态接口资源权限运行时拦截。

**技术栈：** Java 17、Spring Boot 3.5、Spring Security、JdbcTemplate、JUnit 5、MockMvc、AssertJ、H2 测试库、MySQL 本地冒烟。

---

## 1. 背景与边界

后端 V2 已完成以下能力：

- 从旧库 `t_user_auth`、`t_user_info`、`t_user_role`、`t_role` 读取真实账号。
- 使用 BCrypt 校验密码。
- 签发和解析 Bearer JWT。
- `GET /api/auth/me` 能基于 JWT 返回账号 ID、用户名和角色。
- 登录成功后能回写 `t_user_auth.last_login_time` 和 `t_user_auth.ip_address`。

当前缺口是：后台管理端后续迁移时，登录后还需要用户资料和菜单树。旧系统对应接口是：

- `GET /admin/user/menus`：读取当前用户可访问的后台菜单。
- 当前用户资料散落在登录态和用户信息接口中，V2 需要先形成清晰的 `/api/auth/me` 响应。

本计划只做两个稳定基础能力：

- 增强 `/api/auth/me`，返回当前登录账号关联的用户资料快照。
- 新增 `GET /api/admin/user/menus`，返回当前登录用户的后台菜单树。

本计划明确不做：

- 不做后台菜单新增、修改、删除。
- 不做角色管理接口。
- 不做资源权限管理接口。
- 不把旧系统 `FilterInvocationSecurityMetadataSourceImpl` 原样搬到 V2。
- 不做 Redis 在线用户、踢下线、多端登录管理。
- 不改前台或后台管理端。

## 2. 文件结构

### 新建文件

- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/CurrentUserProfile.java`
  - 当前用户资料快照，面向 `/api/auth/me` 响应。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/CurrentUserProfileReader.java`
  - 当前用户资料读取端口。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserMenu.java`
  - 后台菜单树领域模型。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserMenuReader.java`
  - 当前用户菜单读取端口。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/IdentityQueryService.java`
  - 当前用户资料和菜单查询用例。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseCurrentUserProfileReader.java`
  - 从旧表读取当前用户资料。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseUserMenuReader.java`
  - 从旧表读取并组装当前用户菜单树。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/UserMenuResponse.java`
  - 后台菜单 API 响应 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AdminIdentityController.java`
  - 后台身份相关 API，先承载当前用户菜单接口。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseCurrentUserProfileReaderTest.java`
  - 当前用户资料数据库读取测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseUserMenuReaderTest.java`
  - 当前用户菜单数据库读取和树组装测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AdminIdentityControllerTest.java`
  - 后台菜单接口安全和响应回归测试。

### 修改文件

- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`
  - `/api/auth/me` 改为通过 `IdentityQueryService` 读取资料快照。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/MeResponse.java`
  - 增加 `userInfoId`、`nickname`、`avatar`、`email`、`roles`。
- `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
  - 补齐 `t_menu`、`t_role_menu` 测试表和菜单数据。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`
  - 补充 `/api/auth/me` 在真实 token 下返回资料的测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java`
  - 如现有架构规则覆盖不到新增查询端口，需要补充 identity 边界规则。

---

## 任务 1：补齐测试菜单表和固定数据

**文件：**

- 修改：`MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`

- 验证：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java`

- [x] **步骤 1：给测试迁移增加菜单与角色菜单表**

在 `V2__create_legacy_identity_tables_for_tests.sql` 的 `t_user_role` 表之后增加：

```sql
create table t_menu (
    id int auto_increment primary key,
    name varchar(50) not null,
    path varchar(100) not null,
    component varchar(100) not null,
    icon varchar(50),
    order_num int not null default 0,
    parent_id int,
    is_hidden tinyint not null default 0,
    create_time timestamp not null,
    update_time timestamp
);

create table t_role_menu (
    id int auto_increment primary key,
    role_id int not null,
    menu_id int not null
);
```

- [x] **步骤 2：给测试库插入菜单数据**

在现有 `insert into t_user_role` 之后增加：

```sql
insert into t_menu (id, name, path, component, icon, order_num, parent_id, is_hidden, create_time)
values
    (1, '首页', '/', 'Layout', 'Home', 1, null, 0, current_timestamp),
    (2, '文章管理', '/article', 'Layout', 'Document', 2, null, 0, current_timestamp),
    (3, '文章列表', 'list', 'article/ArticleList', 'List', 1, 2, 0, current_timestamp),
    (4, '草稿箱', 'drafts', 'article/DraftList', 'Edit', 2, 2, 1, current_timestamp),
    (5, '评论管理', '/comments', 'comment/CommentList', 'Message', 3, null, 0, current_timestamp),
    (6, '个人中心', '/profile', 'user/Profile', 'User', 4, null, 0, current_timestamp);

insert into t_role_menu (id, role_id, menu_id)
values
    (1, 1, 1),
    (2, 1, 2),
    (3, 1, 3),
    (4, 1, 4),
    (5, 1, 5),
    (6, 1, 6),
    (7, 2, 6);
```

说明：

- 管理员角色拥有完整菜单。

- 普通用户只拥有个人中心菜单，用于验证 `/api/admin/**` 仍会拒绝普通用户访问后台接口。

- `草稿箱` 设置为隐藏菜单，用于验证 `hidden=true`。

- [x] **步骤 3：运行 Flyway 测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest'
```

预期：通过，确认 H2 测试迁移仍可执行。

- [x] **步骤 4：提交测试表基线**

```powershell
git add MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql
git commit -m "补齐后端V2身份菜单测试表"
```

---

## 任务 2：增强当前用户资料读取

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/CurrentUserProfile.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/CurrentUserProfileReader.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/IdentityQueryService.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseCurrentUserProfileReader.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseCurrentUserProfileReaderTest.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/MeResponse.java`

- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`

- [x] **步骤 1：先写数据库资料读取测试**

新建 `DatabaseCurrentUserProfileReaderTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.infrastructure.DatabaseCurrentUserProfileReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseCurrentUserProfileReader.class)
class DatabaseCurrentUserProfileReaderTest {

    @Autowired
    private DatabaseCurrentUserProfileReader reader;

    @Test
    void loadsProfileByAuthId() {
        var profile = reader.findByAuthId("1");

        assertThat(profile).isPresent();
        assertThat(profile.get().authId()).isEqualTo("1");
        assertThat(profile.get().userInfoId()).isEqualTo("1");
        assertThat(profile.get().username()).isEqualTo("admin@163.com");
        assertThat(profile.get().nickname()).isEqualTo("管理员");
        assertThat(profile.get().avatar()).isEqualTo("");
        assertThat(profile.get().email()).isEqualTo("admin@163.com");
    }

    @Test
    void returnsEmptyWhenAuthIdDoesNotExist() {
        assertThat(reader.findByAuthId("999")).isEmpty();
    }
}
```

- [x] **步骤 2：运行测试确认失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCurrentUserProfileReaderTest'
```

预期：编译失败，提示 `DatabaseCurrentUserProfileReader` 或 `CurrentUserProfile` 不存在。

- [x] **步骤 3：新增当前用户资料领域模型和端口**

新建 `CurrentUserProfile.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

public record CurrentUserProfile(
        String authId,
        String userInfoId,
        String username,
        String nickname,
        String avatar,
        String email
) {
}
```

新建 `CurrentUserProfileReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

import java.util.Optional;

public interface CurrentUserProfileReader {

    Optional<CurrentUserProfile> findByAuthId(String authId);
}
```

- [x] **步骤 4：实现数据库资料读取适配器**

新建 `DatabaseCurrentUserProfileReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfile;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfileReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseCurrentUserProfileReader implements CurrentUserProfileReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCurrentUserProfileReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CurrentUserProfile> findByAuthId(String authId) {
        if (authId == null || authId.isBlank()) {
            return Optional.empty();
        }
        List<CurrentUserProfile> profiles = jdbcTemplate.query("""
                        select
                            ua.id as auth_id,
                            ui.id as user_info_id,
                            ua.username as username,
                            ui.nickname as nickname,
                            ui.avatar as avatar,
                            ui.email as email
                        from t_user_auth ua
                        join t_user_info ui on ua.user_info_id = ui.id
                        where ua.id = ?
                          and ui.is_disable = 0
                        limit 1
                        """,
                (rs, rowNum) -> new CurrentUserProfile(
                        rs.getString("auth_id"),
                        rs.getString("user_info_id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("email")),
                authId.trim());
        return profiles.stream().findFirst();
    }
}
```

- [x] **步骤 5：运行数据库资料读取测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCurrentUserProfileReaderTest'
```

预期：通过，2 个测试，0 失败。

- [x] **步骤 6：新增 IdentityQueryService**

新建 `IdentityQueryService.java`：

```java
package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfile;
import com.aurora.myblog.v2.modules.identity.domain.CurrentUserProfileReader;
import org.springframework.stereotype.Service;

@Service
public class IdentityQueryService {

    private final CurrentUserProfileReader profileReader;

    public IdentityQueryService(CurrentUserProfileReader profileReader) {
        this.profileReader = profileReader;
    }

    public CurrentUserProfile currentProfile(AuthenticatedPrincipal principal) {
        return profileReader.findByAuthId(principal.id())
                .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_REQUIRED, "用户未登录"));
    }
}
```

- [x] **步骤 7：增强 MeResponse 和 AuthController**

把 `MeResponse.java` 修改为：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.util.Set;

public record MeResponse(
        String id,
        String userInfoId,
        String username,
        String nickname,
        String avatar,
        String email,
        Set<AuthRole> roles
) {
}
```

修改 `AuthController`：

```java
private final AuthService authService;
private final IdentityQueryService identityQueryService;

public AuthController(AuthService authService, IdentityQueryService identityQueryService) {
    this.authService = authService;
    this.identityQueryService = identityQueryService;
}
```

`me(...)` 方法改为：

```java
@GetMapping("/me")
ApiResponse<MeResponse> me(@CurrentUser AuthenticatedPrincipal user) {
    var profile = identityQueryService.currentProfile(user);
    return ApiResponse.ok(new MeResponse(
            profile.authId(),
            profile.userInfoId(),
            profile.username(),
            profile.nickname(),
            profile.avatar(),
            profile.email(),
            user.roles().stream().map(AuthRole::valueOf).collect(java.util.stream.Collectors.toSet())));
}
```

同时为 `AuthController.java` 增加导入：

```java
import com.aurora.myblog.v2.modules.identity.application.IdentityQueryService;
```

- [x] **步骤 8：补充 `/api/auth/me` 资料响应测试**

在 `AuthControllerTest` 增加测试：

```java
@Test
void meReturnsCurrentDatabaseUserProfile() throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"admin@163.com","password":"password123"}
                            """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.accessToken");

    mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("1"))
            .andExpect(jsonPath("$.data.userInfoId").value("1"))
            .andExpect(jsonPath("$.data.username").value("admin@163.com"))
            .andExpect(jsonPath("$.data.nickname").value("管理员"))
            .andExpect(jsonPath("$.data.avatar").value(""))
            .andExpect(jsonPath("$.data.email").value("admin@163.com"))
            .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
}
```

- [x] **步骤 9：运行资料相关测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCurrentUserProfileReaderTest,AuthControllerTest'
```

预期：通过。

- [x] **步骤 10：提交当前用户资料能力**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/CurrentUserProfile.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/CurrentUserProfileReader.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/IdentityQueryService.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseCurrentUserProfileReader.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/MeResponse.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseCurrentUserProfileReaderTest.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java
git commit -m "增强后端V2当前用户资料"
```

---

## 任务 3：新增当前用户后台菜单读取

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserMenu.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserMenuReader.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseUserMenuReader.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/UserMenuResponse.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AdminIdentityController.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseUserMenuReaderTest.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AdminIdentityControllerTest.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/IdentityQueryService.java`

- [x] **步骤 1：先写菜单读取数据库测试**

新建 `DatabaseUserMenuReaderTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.infrastructure.DatabaseUserMenuReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseUserMenuReader.class)
class DatabaseUserMenuReaderTest {

    @Autowired
    private DatabaseUserMenuReader reader;

    @Test
    void loadsAdminMenuTreeByAuthId() {
        var menus = reader.findByAuthId("1");

        assertThat(menus).extracting("name").containsExactly("首页", "文章管理", "评论管理", "个人中心");
        var article = menus.get(1);
        assertThat(article.children()).extracting("name").containsExactly("文章列表", "草稿箱");
        assertThat(article.children().get(1).hidden()).isTrue();
    }

    @Test
    void wrapsLeafRootMenuWithEmptyChildPathForRouterCompatibility() {
        var menus = reader.findByAuthId("1");

        var home = menus.get(0);
        assertThat(home.path()).isEqualTo("/");
        assertThat(home.component()).isEqualTo("Layout");
        assertThat(home.children()).hasSize(1);
        assertThat(home.children().get(0).path()).isEqualTo("");
        assertThat(home.children().get(0).component()).isEqualTo("Layout");
    }

    @Test
    void returnsEmptyWhenAuthIdDoesNotExist() {
        assertThat(reader.findByAuthId("999")).isEmpty();
    }
}
```

- [x] **步骤 2：运行测试确认失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseUserMenuReaderTest'
```

预期：编译失败，提示 `DatabaseUserMenuReader` 或 `UserMenu` 不存在。

- [x] **步骤 3：新增菜单领域模型和端口**

新建 `UserMenu.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

import java.util.List;

public record UserMenu(
        String name,
        String path,
        String component,
        String icon,
        boolean hidden,
        List<UserMenu> children
) {
    public UserMenu {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
```

新建 `UserMenuReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

import java.util.List;

public interface UserMenuReader {

    List<UserMenu> findByAuthId(String authId);
}
```

- [x] **步骤 4：实现数据库菜单读取适配器**

新建 `DatabaseUserMenuReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.UserMenu;
import com.aurora.myblog.v2.modules.identity.domain.UserMenuReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DatabaseUserMenuReader implements UserMenuReader {

    private static final String ROOT_COMPONENT = "Layout";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserMenuReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<UserMenu> findByAuthId(String authId) {
        if (authId == null || authId.isBlank()) {
            return List.of();
        }
        List<MenuRow> rows = jdbcTemplate.query("""
                        select distinct
                            m.id,
                            m.name,
                            m.path,
                            m.component,
                            m.icon,
                            m.is_hidden,
                            m.parent_id,
                            m.order_num
                        from t_user_auth ua
                        join t_user_role ur on ua.user_info_id = ur.user_id
                        join t_role_menu rm on ur.role_id = rm.role_id
                        join t_menu m on rm.menu_id = m.id
                        join t_role r on ur.role_id = r.id
                        where ua.id = ?
                          and r.is_disable = 0
                        order by m.order_num, m.id
                        """,
                (rs, rowNum) -> new MenuRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("path"),
                        rs.getString("component"),
                        rs.getString("icon"),
                        rs.getInt("is_hidden") == 1,
                        (Integer) rs.getObject("parent_id"),
                        rs.getInt("order_num")),
                authId.trim());
        return buildTree(rows);
    }

    private List<UserMenu> buildTree(List<MenuRow> rows) {
        Map<Integer, List<MenuRow>> childrenByParent = rows.stream()
                .filter(row -> row.parentId() != null)
                .collect(Collectors.groupingBy(MenuRow::parentId));
        return rows.stream()
                .filter(row -> row.parentId() == null)
                .sorted(Comparator.comparing(MenuRow::orderNum).thenComparing(MenuRow::id))
                .map(row -> toUserMenu(row, childrenByParent.getOrDefault(row.id(), List.of())))
                .toList();
    }

    private UserMenu toUserMenu(MenuRow row, List<MenuRow> children) {
        if (children.isEmpty()) {
            List<UserMenu> leaf = new ArrayList<>();
            leaf.add(new UserMenu(row.name(), "", row.component(), row.icon(), false, List.of()));
            return new UserMenu(row.name(), row.path(), ROOT_COMPONENT, row.icon(), row.hidden(), leaf);
        }
        List<UserMenu> childMenus = children.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MenuRow::orderNum).thenComparing(MenuRow::id))
                .map(child -> new UserMenu(child.name(), child.path(), child.component(), child.icon(), child.hidden(), List.of()))
                .toList();
        return new UserMenu(row.name(), row.path(), row.component(), row.icon(), row.hidden(), childMenus);
    }

    private record MenuRow(
            Integer id,
            String name,
            String path,
            String component,
            String icon,
            boolean hidden,
            Integer parentId,
            Integer orderNum
    ) {
    }
}
```

注意：这里保留旧后台路由兼容逻辑。根菜单没有子菜单时，返回一个 `path=""` 的子节点，外层组件固定为 `Layout`，和旧 `MenuServiceImpl.convertUserMenuList(...)` 的语义一致。

- [x] **步骤 5：运行菜单读取测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseUserMenuReaderTest'
```

预期：通过，3 个测试，0 失败。

- [x] **步骤 6：把菜单查询接入 IdentityQueryService**

修改 `IdentityQueryService.java`：

```java
private final CurrentUserProfileReader profileReader;
private final UserMenuReader userMenuReader;

public IdentityQueryService(CurrentUserProfileReader profileReader, UserMenuReader userMenuReader) {
    this.profileReader = profileReader;
    this.userMenuReader = userMenuReader;
}
```

增加方法：

```java
public java.util.List<UserMenu> currentUserMenus(AuthenticatedPrincipal principal) {
    return userMenuReader.findByAuthId(principal.id());
}
```

并增加导入：

```java
import com.aurora.myblog.v2.modules.identity.domain.UserMenu;
import com.aurora.myblog.v2.modules.identity.domain.UserMenuReader;
```

- [x] **步骤 7：新增菜单响应 DTO 和 Controller**

新建 `UserMenuResponse.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.modules.identity.domain.UserMenu;

import java.util.List;

public record UserMenuResponse(
        String name,
        String path,
        String component,
        String icon,
        boolean hidden,
        List<UserMenuResponse> children
) {
    public static UserMenuResponse from(UserMenu menu) {
        return new UserMenuResponse(
                menu.name(),
                menu.path(),
                menu.component(),
                menu.icon(),
                menu.hidden(),
                menu.children().stream().map(UserMenuResponse::from).toList());
    }
}
```

新建 `AdminIdentityController.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.auth.CurrentUser;
import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.modules.identity.application.IdentityQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user")
public class AdminIdentityController {

    private final IdentityQueryService identityQueryService;

    public AdminIdentityController(IdentityQueryService identityQueryService) {
        this.identityQueryService = identityQueryService;
    }

    @GetMapping("/menus")
    ApiResponse<List<UserMenuResponse>> menus(@CurrentUser AuthenticatedPrincipal user) {
        List<UserMenuResponse> menus = identityQueryService.currentUserMenus(user)
                .stream()
                .map(UserMenuResponse::from)
                .toList();
        return ApiResponse.ok(menus);
    }
}
```

- [x] **步骤 8：写后台菜单接口测试**

新建 `AdminIdentityControllerTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminIdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMenusWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/user/menus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsMenusForNonAdminUser() throws Exception {
        String token = loginAndToken("user@163.com");

        mockMvc.perform(get("/api/admin/user/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsMenusForAdminUser() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(get("/api/admin/user/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("首页"))
                .andExpect(jsonPath("$.data[1].name").value("文章管理"))
                .andExpect(jsonPath("$.data[1].children[0].name").value("文章列表"))
                .andExpect(jsonPath("$.data[1].children[1].name").value("草稿箱"))
                .andExpect(jsonPath("$.data[1].children[1].hidden").value(true));
    }

    private String loginAndToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }
}
```

- [x] **步骤 9：运行菜单接口测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseUserMenuReaderTest,AdminIdentityControllerTest,SecurityConfigTest'
```

预期：通过。

- [x] **步骤 10：提交当前用户菜单能力**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserMenu.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserMenuReader.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseUserMenuReader.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/UserMenuResponse.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AdminIdentityController.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/IdentityQueryService.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseUserMenuReaderTest.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AdminIdentityControllerTest.java
git commit -m "新增后端V2当前用户菜单接口"
```

---

## 任务 4：整体验证与本地 MySQL 冒烟确认

**文件：**

- 验证：`MyBlog-springboot-v2/**`

- 可选修改：`docs/superpowers/plans/2026-05-28-backend-v2-identity-profile-menu.zh-CN.md`

- [x] **步骤 1：运行身份资料与菜单相关测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCurrentUserProfileReaderTest,DatabaseUserMenuReaderTest,AuthControllerTest,AdminIdentityControllerTest,SecurityConfigTest'
```

预期：全部通过。

- [x] **步骤 2：运行全量测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：全部通过。

- [x] **步骤 3：运行打包验证**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

预期：通过，并生成：

```text
MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar
```

- [x] **步骤 4：只读确认本地 MySQL 菜单数据**

运行：

```powershell
$env:MYSQL_PWD = Read-Host '请输入本地 MySQL 密码'
mysql -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 -N aurora -e "select count(*) from t_menu; select count(*) from t_role_menu;"
```

预期：

- `t_menu` 数量大于 0。

- `t_role_menu` 数量大于 0。

- 不修改本地库结构，不写入测试数据。

- [x] **步骤 5：本地启动 V2 服务**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:MYBLOG_JWT_SECRET='local-dev-secret-local-dev-secret-123456'
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

预期：应用启动成功，监听 `8080`。

- [x] **步骤 6：调用真实账号登录、当前用户和菜单接口**

另开一个终端运行。密码只保存在当前 PowerShell 变量里，不写入任何文件：

```powershell
$password = Read-Host '请输入本地真实密码'
$body = @{ username = 'tongyibin1@gmail.com'; password = $password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -Headers @{ 'X-Forwarded-For' = '203.0.113.10' } -ContentType 'application/json' -Body $body
$token = $login.data.accessToken
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/auth/me -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/admin/user/menus -Headers @{ Authorization = "Bearer $token" }
```

预期：

- 登录返回 `success=true`。

- `/api/auth/me` 返回 `username=tongyibin1@gmail.com`，并包含用户资料字段。

- `/api/admin/user/menus` 返回菜单数组。

- [x] **步骤 7：停止本地 V2 服务并确认端口释放**

运行：

```powershell
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*MyBlog-springboot-v2*spring-boot:run*' -or $_.CommandLine -like '*MyBlogV2Application*' } | Select-Object ProcessId,Name,CommandLine
```

只停止本轮启动的 Maven/Java 进程，之后确认：

```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

预期：`8080` 不再监听。

- [x] **步骤 8：更新本计划完成状态和执行记录**

如果实施者按本计划逐项完成，可以把本文件对应任务步骤勾选为 `[x]`，并在文末追加执行记录。只记录验证事实，不记录真实密码。

- [x] **步骤 9：提交阶段验证状态**

```powershell
git add docs/superpowers/plans/2026-05-28-backend-v2-identity-profile-menu.zh-CN.md
git commit -m "同步后端V2身份菜单计划状态"
```

如果没有修改计划文档，则不用提交。

---

## 自检记录

- 覆盖范围：当前用户资料、后台菜单读取、后台接口角色保护、H2 测试迁移、本地 MySQL 冒烟。
- 明确排除：菜单管理 CRUD、角色管理、动态资源权限运行时拦截、Redis 在线用户、前端联调。
- 类型一致性：JWT 中仍只保存认证必要信息；用户资料和菜单通过数据库查询端口读取，不把旧 Redis 登录态模型带入 V2。
- 风险控制：本阶段只读取旧表，不修改本地 MySQL 表结构；测试环境菜单表只存在于 H2 test migration。

## 任务 4 执行记录

- 身份资料与菜单相关测试：`DatabaseCurrentUserProfileReaderTest,DatabaseUserMenuReaderTest,AuthControllerTest,AdminIdentityControllerTest,SecurityConfigTest` 通过，17 个测试，0 失败，0 错误。
- 全量测试：`mvn -f MyBlog-springboot-v2/pom.xml test` 通过，49 个测试，0 失败，0 错误。
- 打包验证：`mvn -f MyBlog-springboot-v2/pom.xml clean package` 通过，生成 `MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar`。
- 本地 MySQL 只读检查：`t_menu=35`，`t_role_menu=70`，未修改本地库结构，未写入测试数据。
- 本地 V2 服务：使用 `local` profile 启动成功，监听 `8080`。
- 真实账号接口冒烟：登录返回 `success=true`、`code=OK`，`accessToken` 存在；`/api/auth/me` 返回 `username=tongyibin1@gmail.com`，并包含 `userInfoId`、`nickname`、`email` 等用户资料字段；`/api/admin/user/menus` 返回菜单数组，数量为 10。
- 清理：本轮启动的 V2 Maven/Java 进程已停止，`8080` 已释放。
