# 后端 V2 数据库真实账号登录实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项执行本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 让后端 V2 不再依赖配置文件账号，而是读取本地 MySQL `aurora` 库中的旧用户、资料、角色表完成真实账号登录。

**架构：** 保留现有 Bearer JWT、安全过滤器、统一 401/403 响应和 `AuthService` 用例，只替换 `UserCredentialReader` 的数据来源。V2 先通过 Spring JDBC 读取旧库表结构，避免过早引入 MyBatis Plus 或 JPA；动态接口资源权限、后台菜单、Redis 在线用户后续单独规划。

**技术栈：** Java 17、Spring Boot 3.5.14、Spring Security 6.5.x、Spring JDBC、Flyway、H2 测试数据库、MySQL Connector/J、JUnit 5、MockMvc、AssertJ。

---

## 范围约束

本计划只处理真实账号登录，不扩大到其它业务域。

包含：

- 为 V2 本地 profile 增加 MySQL 连接配置。
- 增加 MySQL JDBC 驱动。
- 在测试环境用 H2 建立旧身份表的最小兼容结构和测试数据。
- 新增数据库身份读取适配器 `DatabaseUserCredentialReader`。
- 将配置账号适配器改成非默认启用，避免生产和本地同时存在两个 `UserCredentialReader` Bean。
- 让 `AuthControllerTest` 使用数据库账号完成登录回归。
- 明确禁用用户不能登录。
- 保留当前 JWT 签发、解析、撤销方式。

不包含：

- 不迁移后台菜单接口。
- 不实现动态接口资源权限。
- 不接入 Redis 在线用户。
- 不改前台和后台管理端。
- 不改旧后端 `MyBlog-springboot`。
- 不改线上运行服务。

## 关键决策

### 本地连接旧库时暂时关闭 Flyway

本地 `aurora` 库是从线上导入的旧库，已经有大量旧表，但没有 V2 的 `flyway_schema_history`。如果直接给本地 profile 开启 Flyway，Spring Boot 启动时可能把 V2 测试迁移跑到旧库，或者因为非空 schema 未 baseline 而失败。

所以本计划要求：

- `application-local.yml` 配置 MySQL datasource。
- `application-local.yml` 暂时设置 `spring.flyway.enabled: false`。
- 测试 profile 继续使用 H2，并开启 Flyway。
- 测试用身份表迁移放在 `src/test/resources/db/migration`，只服务测试，不写入本地 MySQL。

### 角色名先做稳定映射

旧库角色名是小写字符串：`admin`、`user`、`test`。V2 当前角色枚举只有 `ADMIN` 和 `USER`。

本计划先做保守映射：

- `admin` -> `AuthRole.ADMIN`
- `user` -> `AuthRole.USER`
- 其它角色暂时忽略，不阻断登录

原因：V2 当前安全配置只识别 `ADMIN` 和 `USER`。`test` 角色的运行时语义还没在 V2 设计里定下来，不能为了兼容一个旧角色名就随便扩大权限模型。后续做后台菜单或权限模型时再决定是否引入 `TEST`。

### 禁用用户必须不能登录

旧库用户禁用字段在 `t_user_info.is_disable`。数据库身份读取适配器必须在 SQL 中排除禁用用户，或者查询后拒绝禁用用户。为了不泄露账号状态，禁用用户登录仍返回 `BAD_CREDENTIALS` 和统一消息 `用户名或密码错误`。

## 主要参考

- 盘点文档：`docs/superpowers/reviews/2026-05-24-backend-v2-identity-permission-inventory.zh-CN.md`
- V2 身份端口：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserCredentialReader.java`
- V2 登录用例：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthService.java`
- 当前配置账号适配器：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/ConfiguredUserCredentialReader.java`
- V2 安全配置：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityConfig.java`
- 旧账号实体：`MyBlog-springboot/src/main/java/com/aurora/entity/UserAuth.java`
- 旧资料实体：`MyBlog-springboot/src/main/java/com/aurora/entity/UserInfo.java`
- 旧角色 Mapper：`MyBlog-springboot/src/main/resources/mapper/RoleMapper.xml`

## 文件结构

本计划完成后，身份模块结构变为：

```text
MyBlog-springboot-v2
├─ pom.xml
├─ src/main/java/com/aurora/myblog/v2/modules/identity
│  ├─ domain
│  │  ├─ AuthRole.java
│  │  ├─ AuthenticatedUser.java
│  │  ├─ LoginCommand.java
│  │  └─ UserCredentialReader.java
│  └─ infrastructure
│     ├─ ConfiguredIdentityProperties.java
│     ├─ ConfiguredUserCredentialReader.java
│     ├─ DatabaseUserCredentialReader.java
│     └─ RoleNameMapper.java
├─ src/main/resources
│  ├─ application.yml
│  └─ application-local.yml
└─ src/test
   ├─ java/com/aurora/myblog/v2/modules/identity
   │  ├─ AuthControllerTest.java
   │  ├─ AuthServiceTest.java
   │  ├─ DatabaseUserCredentialReaderTest.java
   │  └─ RoleNameMapperTest.java
   └─ resources
      ├─ application-test.yml
      └─ db/migration
         └─ V2__create_legacy_identity_tables_for_tests.sql
```

## 验收标准

- `POST /api/auth/login` 可以使用 H2 测试库中的 `admin@163.com/password123` 登录。
- 登录成功响应中的用户为 `admin@163.com`，角色包含 `ADMIN`。
- `admin@163.com` 的 Bearer Token 可以访问 `/api/admin/security-probe`。
- 普通用户 `user@163.com/password123` 登录后访问 `/api/admin/security-probe` 返回 403。
- 禁用用户 `disabled@163.com/password123` 登录返回 401，错误码为 `BAD_CREDENTIALS`。
- 缺失用户和错误密码仍返回相同错误，不泄露账号存在性。
- 本地 profile 可以连接 `localhost:3306/aurora`，但不会自动执行 Flyway 迁移到旧库。
- 全量测试 `mvn -f MyBlog-springboot-v2/pom.xml test` 通过。

## 任务 1：补齐数据库依赖与本地 MySQL 配置

**文件：**

- 修改：`MyBlog-springboot-v2/pom.xml`
- 修改：`MyBlog-springboot-v2/src/main/resources/application-local.yml`
- 修改：`MyBlog-springboot-v2/src/test/resources/application-test.yml`

- [x] **步骤 1：先确认当前测试仍通过**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：通过。若失败，先停下排查，因为本任务不应该建立在红灯测试上。

- [x] **步骤 2：增加 MySQL JDBC 驱动**

修改 `MyBlog-springboot-v2/pom.xml`，在 `spring-boot-starter-jdbc` 后加入：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

- [x] **步骤 3：配置本地 MySQL datasource，并关闭本地 Flyway**

修改 `MyBlog-springboot-v2/src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: ${MYBLOG_DATASOURCE_URL:jdbc:mysql://localhost:3306/aurora?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Tokyo}
    username: ${MYBLOG_DATASOURCE_USERNAME:root}
    password: ${MYBLOG_DATASOURCE_PASSWORD:2423137093}
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: false

myblog:
  cors:
    allowed-origins:
      - http://localhost:5173
      - http://localhost:5174
  security:
    public-endpoints:
      - /actuator/health
      - /api/public/security-probe
      - /api/auth/login
```

说明：密码先放在本地 profile 默认值里是为了配合当前开发机验证。后续提交前如果你希望更严格，可以改成只允许环境变量。

- [x] **步骤 4：确认测试 profile 仍然使用 H2 和 Flyway**

检查 `MyBlog-springboot-v2/src/test/resources/application-test.yml` 保持如下 datasource 和 Flyway 配置：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:myblog_v2_test;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password:
  flyway:
    enabled: true
```

- [x] **步骤 5：运行测试确认配置没有破坏测试环境**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：通过。

- [x] **步骤 6：提交数据库连接配置**

```powershell
git add MyBlog-springboot-v2/pom.xml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "配置后端V2本地数据库连接"
```

## 任务 2：建立测试身份表和角色映射规则

**文件：**

- 新建：`MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/RoleNameMapper.java`
- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/RoleNameMapperTest.java`

- [x] **步骤 1：先写角色映射失败测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/RoleNameMapperTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.infrastructure.RoleNameMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameMapperTest {

    @Test
    void mapsLegacyRoleNamesToV2Roles() {
        List<AuthRole> roles = RoleNameMapper.toAuthRoles(List.of("admin", "user", "test", "ADMIN"));

        assertThat(roles).containsExactly(AuthRole.ADMIN, AuthRole.USER);
    }

    @Test
    void returnsEmptyListForUnknownOrBlankRoles() {
        List<AuthRole> roles = RoleNameMapper.toAuthRoles(List.of("", " ", "test", "operator"));

        assertThat(roles).isEmpty();
    }
}
```

- [x] **步骤 2：运行测试，确认先失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=RoleNameMapperTest
```

预期：编译失败，因为 `RoleNameMapper` 还不存在。

- [x] **步骤 3：实现角色映射器**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/RoleNameMapper.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class RoleNameMapper {

    private RoleNameMapper() {
    }

    public static List<AuthRole> toAuthRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<AuthRole> roles = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            toAuthRole(roleName).ifPresent(roles::add);
        }
        return List.copyOf(roles);
    }

    private static java.util.Optional<AuthRole> toAuthRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return java.util.Optional.empty();
        }
        return switch (roleName.trim().toLowerCase(Locale.ROOT)) {
            case "admin" -> java.util.Optional.of(AuthRole.ADMIN);
            case "user" -> java.util.Optional.of(AuthRole.USER);
            default -> java.util.Optional.empty();
        };
    }
}
```

- [x] **步骤 4：新增 H2 测试迁移，模拟旧身份表**

创建 `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`：

```sql
create table t_user_info (
    id int auto_increment primary key,
    email varchar(50),
    nickname varchar(50) not null,
    avatar varchar(1024) not null default '',
    intro varchar(255),
    website varchar(255),
    is_subscribe tinyint,
    is_disable tinyint not null default 0,
    create_time timestamp not null,
    update_time timestamp
);

create table t_user_auth (
    id int auto_increment primary key,
    user_info_id int not null,
    username varchar(50) not null unique,
    password varchar(100) not null,
    login_type tinyint not null,
    ip_address varchar(50),
    ip_source varchar(50),
    create_time timestamp not null,
    update_time timestamp,
    last_login_time timestamp
);

create table t_role (
    id int auto_increment primary key,
    role_name varchar(20) not null,
    is_disable tinyint not null default 0,
    create_time timestamp not null,
    update_time timestamp
);

create table t_user_role (
    id int auto_increment primary key,
    user_id int,
    role_id int
);

insert into t_user_info (id, email, nickname, avatar, intro, website, is_subscribe, is_disable, create_time)
values
    (1, 'admin@163.com', '管理员', '', null, null, 0, 0, current_timestamp),
    (2, 'user@163.com', '普通用户', '', null, null, 0, 0, current_timestamp),
    (3, 'disabled@163.com', '禁用用户', '', null, null, 0, 1, current_timestamp);

insert into t_user_auth (id, user_info_id, username, password, login_type, create_time)
values
    (1, 1, 'admin@163.com', '$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy', 1, current_timestamp),
    (2, 2, 'user@163.com', '$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy', 1, current_timestamp),
    (3, 3, 'disabled@163.com', '$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy', 1, current_timestamp);

insert into t_role (id, role_name, is_disable, create_time)
values
    (1, 'admin', 0, current_timestamp),
    (2, 'user', 0, current_timestamp),
    (14, 'test', 0, current_timestamp);

insert into t_user_role (id, user_id, role_id)
values
    (1, 1, 1),
    (2, 1, 14),
    (3, 2, 2),
    (4, 3, 1);
```

说明：`$2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy` 对应明文密码 `password123`，只用于测试。

- [x] **步骤 5：运行角色映射测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=RoleNameMapperTest
```

预期：通过。

- [x] **步骤 6：运行 Flyway 测试，确认测试迁移可执行**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest,MyBlogV2ApplicationTest'
```

预期：通过。

- [x] **步骤 7：提交测试身份表和角色映射**

```powershell
git add MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/RoleNameMapper.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/RoleNameMapperTest.java
git commit -m "新增后端V2旧角色映射规则"
```

## 任务 3：实现数据库身份读取适配器

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseUserCredentialReader.java`
- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseUserCredentialReaderTest.java`

- [ ] **步骤 1：先写数据库读取失败测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseUserCredentialReaderTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.infrastructure.DatabaseUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseUserCredentialReader.class)
class DatabaseUserCredentialReaderTest {

    @Autowired
    private DatabaseUserCredentialReader reader;

    @Test
    void loadsActiveUserWithMappedRoles() {
        var credential = reader.findByUsername("admin@163.com");

        assertThat(credential).isPresent();
        assertThat(credential.get().id()).isEqualTo("1");
        assertThat(credential.get().username()).isEqualTo("admin@163.com");
        assertThat(credential.get().passwordHash()).startsWith("$2a$10$");
        assertThat(credential.get().roles()).containsExactly(AuthRole.ADMIN);
    }

    @Test
    void ignoresCaseWhenFindingUsername() {
        var credential = reader.findByUsername("ADMIN@163.COM");

        assertThat(credential).isPresent();
        assertThat(credential.get().username()).isEqualTo("admin@163.com");
    }

    @Test
    void doesNotLoadDisabledUser() {
        assertThat(reader.findByUsername("disabled@163.com")).isEmpty();
    }

    @Test
    void returnsEmptyWhenUserDoesNotExist() {
        assertThat(reader.findByUsername("missing@163.com")).isEmpty();
    }
}
```

- [ ] **步骤 2：运行测试，确认先失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=DatabaseUserCredentialReaderTest
```

预期：编译失败，因为 `DatabaseUserCredentialReader` 还不存在。

- [ ] **步骤 3：实现数据库身份读取适配器**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseUserCredentialReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Primary
@Component
public class DatabaseUserCredentialReader implements UserCredentialReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserCredentialReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        List<AccountRow> accounts = jdbcTemplate.query("""
                        select
                            ua.id as auth_id,
                            ua.username as username,
                            ua.password as password_hash
                        from t_user_auth ua
                        join t_user_info ui on ua.user_info_id = ui.id
                        where lower(ua.username) = lower(?)
                          and ui.is_disable = 0
                        limit 1
                        """,
                (rs, rowNum) -> new AccountRow(
                        rs.getString("auth_id"),
                        rs.getString("username"),
                        rs.getString("password_hash")),
                username.trim());
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        AccountRow account = accounts.get(0);
        List<AuthRole> roles = RoleNameMapper.toAuthRoles(loadRoleNames(account.id()));
        return Optional.of(new UserCredential(account.id(), account.username(), account.passwordHash(), roles));
    }

    private List<String> loadRoleNames(String authId) {
        return jdbcTemplate.query("""
                        select r.role_name
                        from t_user_auth ua
                        join t_user_role ur on ua.user_info_id = ur.user_id
                        join t_role r on ur.role_id = r.id
                        where ua.id = ?
                          and r.is_disable = 0
                        order by r.id
                        """,
                (rs, rowNum) -> rs.getString("role_name"),
                authId);
    }

    private record AccountRow(String id, String username, String passwordHash) {
    }
}
```

说明：这里返回的 `id` 使用旧表 `t_user_auth.id`，与旧 JWT subject 语义一致；角色关系通过 `user_info_id` 连接 `t_user_role.user_id`。

- [ ] **步骤 4：运行数据库读取测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=DatabaseUserCredentialReaderTest
```

预期：通过。

- [ ] **步骤 5：提交数据库身份读取适配器**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseUserCredentialReader.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseUserCredentialReaderTest.java
git commit -m "新增后端V2数据库身份读取"
```

## 任务 4：切换登录回归到数据库账号

**文件：**

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/ConfiguredUserCredentialReader.java`
- 修改：`MyBlog-springboot-v2/src/test/resources/application-test.yml`
- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`
- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java`

- [ ] **步骤 1：让配置账号适配器只在专用 profile 启用**

修改 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/ConfiguredUserCredentialReader.java`，增加 `@Profile("configured-identity")`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Profile("configured-identity")
@Component
@EnableConfigurationProperties(ConfiguredIdentityProperties.class)
public class ConfiguredUserCredentialReader implements UserCredentialReader {

    private final ConfiguredIdentityProperties properties;

    public ConfiguredUserCredentialReader(ConfiguredIdentityProperties properties) {
        this.properties = properties;
    }

    public static ConfiguredUserCredentialReader singleUser(String username, String passwordHash, List<AuthRole> roles) {
        ConfiguredIdentityProperties.User user =
                new ConfiguredIdentityProperties.User("test-user", username, passwordHash, roles);
        return new ConfiguredUserCredentialReader(new ConfiguredIdentityProperties(List.of(user)));
    }

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        return properties.users().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst()
                .map(user -> new UserCredential(user.id(), user.username(), user.passwordHash(), user.roles()));
    }
}
```

- [ ] **步骤 2：删除测试 profile 中不再使用的配置账号**

修改 `MyBlog-springboot-v2/src/test/resources/application-test.yml`，删除 `myblog.identity.users` 整段，只保留 CORS、安全 JWT、公开端点等配置。

保留后的关键结构应类似：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:myblog_v2_test;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password:
  flyway:
    enabled: true

myblog:
  cors:
    allowed-origins:
      - http://localhost:5173
  security:
    jwt:
      issuer: myblog-v2-test
      secret: test-secret-test-secret-test-secret-123456
      access-token-ttl: 15m
    public-endpoints:
      - /actuator/health
      - /api/public/security-probe
      - /api/auth/login
```

- [ ] **步骤 3：修改登录 Controller 测试使用数据库账号**

修改 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`，把成功登录账号从 `admin@example.com` 改成 `admin@163.com`：

```java
@Test
void logsInWithDatabaseCredentialWithoutExistingAuthentication() throws Exception {
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"admin@163.com","password":"password123"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.user.username").value("admin@163.com"))
            .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"));
}
```

继续保留错误密码和缺失账号测试，并新增禁用用户测试：

```java
@Test
void rejectsDisabledUserWithSameBadCredentialResponse() throws Exception {
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"disabled@163.com","password":"password123"}
                            """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
}
```

- [ ] **步骤 4：修改后台授权测试使用数据库普通用户**

修改 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java` 中普通用户登录账号：

```java
String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"user@163.com","password":"password123"}
                        """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
```

如果测试里还有 `admin@example.com` 或 `user@example.com`，分别改成 `admin@163.com` 和 `user@163.com`。

- [ ] **步骤 5：运行认证和授权回归测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=AuthControllerTest,SecurityConfigTest,JwtAuthenticationFilterTest
```

预期：通过。

- [ ] **步骤 6：提交数据库账号登录回归**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/ConfiguredUserCredentialReader.java MyBlog-springboot-v2/src/test/resources/application-test.yml MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java
git commit -m "切换后端V2登录为数据库账号"
```

## 任务 5：本地 MySQL 冒烟验证与阶段收口

**文件：**

- 验证：`MyBlog-springboot-v2/**`
- 可选修改：`docs/superpowers/plans/2026-05-24-backend-v2-database-identity-login.zh-CN.md`

- [ ] **步骤 1：运行全量测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：通过。

- [ ] **步骤 2：运行打包验证**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

预期：通过，并生成：

```text
MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar
```

- [ ] **步骤 3：只读确认本地 MySQL 旧账号存在**

运行：

```powershell
$env:MYSQL_PWD='2423137093'
mysql -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 -N aurora -e "select ua.id, ua.username, ui.is_disable, group_concat(r.role_name order by r.id) from t_user_auth ua join t_user_info ui on ua.user_info_id = ui.id left join t_user_role ur on ui.id = ur.user_id left join t_role r on ur.role_id = r.id group by ua.id, ua.username, ui.is_disable;"
```

预期：至少看到 `admin@163.com` 或当前本地导入库中的真实管理员账号，且 `is_disable = 0`，角色包含 `admin`。

- [ ] **步骤 4：本地启动 V2 服务**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:MYBLOG_JWT_SECRET='local-dev-secret-local-dev-secret-123456'
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

预期：应用启动成功，Flyway 不会对本地 `aurora` 库执行迁移。

- [ ] **步骤 5：调用本地登录接口**

另开一个终端运行：

```powershell
$body = @{ username = 'admin@163.com'; password = 'password123' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType 'application/json' -Body $body
```

预期：

```text
success : True
code    : OK
data    : 包含 accessToken，user.username 为 admin@163.com
```

如果本地导入库的管理员密码不是 `password123`，这一步允许失败，但必须在最终说明中明确：代码验证已通过，本地真实数据密码未知或不匹配，不能用该账号完成接口登录。

- [ ] **步骤 6：更新计划完成状态**

如果实施者按本计划逐项完成，可以把本文件对应任务步骤勾选为 `[x]`。只勾选实际完成的步骤，不要一次性全部勾选。

- [ ] **步骤 7：提交阶段验证状态**

如果只更新计划勾选状态：

```powershell
git add docs/superpowers/plans/2026-05-24-backend-v2-database-identity-login.zh-CN.md
git commit -m "同步后端V2数据库登录计划状态"
```

如果没有修改计划文档，则不用提交。

## 自检记录

- 覆盖范围：数据库连接、测试表结构、角色映射、数据库账号读取、禁用用户、登录回归、本地 MySQL 冒烟验证均有任务。
- 明确排除：动态资源权限、菜单接口、Redis 在线用户、前端联调、旧后端改造不在本计划内。
- 类型一致性：继续使用现有 `UserCredentialReader.UserCredential`、`AuthRole`、`AuthService`、`AuthController`，没有新增跨层依赖。
- 风险控制：本地旧库关闭 Flyway，测试迁移只放在 test resources。
