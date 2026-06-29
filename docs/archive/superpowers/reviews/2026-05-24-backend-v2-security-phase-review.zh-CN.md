# 后端 V2 安全认证阶段复盘

## 阶段结论

本阶段已经完成后端 V2 的基础认证闭环：配置账号可以登录，后端可以签发 JWT，接口可以通过 Bearer Token 识别当前用户，登出后 token 会被撤销，未登录和权限不足会返回统一 JSON 响应。

这不是最终身份体系迁移。当前能力的目标是先把后端 V2 的安全骨架搭起来，让后续真实用户表、角色权限、前后台联调可以建立在稳定边界上继续推进。

## 已完成范围

- JWT 配置基线：`issuer`、`secret`、`access-token-ttl` 已通过配置绑定。
- 身份模型基线：`AuthRole`、`AuthenticatedUser`、`LoginCommand`、`UserCredentialReader` 已建立。
- JWT 令牌服务：支持签发、解析、过期校验和撤销校验。
- 登录用例：支持账号密码登录，错误账号和错误密码返回同一错误。
- Bearer 认证链路：支持 `Authorization: Bearer <token>` 注入当前用户。
- 登出撤销：`POST /api/auth/logout` 会撤销当前 access token。
- 当前用户接口：`GET /api/auth/me` 返回当前登录用户。
- 角色授权回归：`/api/admin/**` 需要 `ADMIN` 角色。
- 架构规则：已保护 identity domain 不依赖 Spring Security，common security 不反向依赖 identity infrastructure。

## 当前接口形态

### `POST /api/auth/login`

请求：

```json
{
  "username": "admin@example.com",
  "password": "password123"
}
```

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "accessToken": "<jwt>",
    "expiresAt": "2026-05-24T00:00:00Z",
    "user": {
      "id": "test-admin",
      "username": "admin@example.com",
      "roles": ["ADMIN"]
    }
  }
}
```

失败响应统一为：

```json
{
  "success": false,
  "code": "BAD_CREDENTIALS",
  "message": "用户名或密码错误",
  "data": null
}
```

### `GET /api/auth/me`

需要 Bearer Token。未登录时返回：

```json
{
  "success": false,
  "code": "AUTHENTICATION_REQUIRED",
  "message": "用户未登录",
  "data": null
}
```

### `POST /api/auth/logout`

需要 Bearer Token。成功后当前 token 会进入撤销存储，再次访问受保护接口会返回 401。

## 架构边界

### `common.auth`

放公共认证抽象，不包含 JWT 实现细节，也不依赖业务模块。

- `AuthenticatedPrincipal`
- `CurrentUser`
- `CurrentUserArgumentResolver`

### `common.security`

放安全框架接入和 JWT 具体实现。

- `SecurityConfig`
- `SecurityProblemSupport`
- `JwtTokenService`
- `JwtAuthenticationFilter`
- `TokenRevocationStore`

### `modules.identity`

放身份业务用例和身份领域模型。它不直接依赖 `common.security`，而是通过自身端口 `AuthTokenService` 需要令牌能力。

### `infrastructure.security`

放跨层适配器。当前 `JwtAuthTokenServiceAdapter` 负责把 identity 的 `AuthTokenService` 端口接到 `JwtTokenService`。

## 当前临时能力

- 用户来源仍是配置文件，不是真实用户表。
- token 撤销存储是内存实现，服务重启会丢失撤销状态。
- 角色只有 `USER` 和 `ADMIN`，没有迁移旧系统的动态角色、资源和菜单权限。
- JWT secret 当前提供了默认值，生产环境必须通过环境变量覆盖。
- 没有接入旧 Redis 登录态，也没有做新旧 token 的兼容迁移。

## 已验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=ArchitectureRulesTest
mvn -f MyBlog-springboot-v2/pom.xml test
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

验证结果：

- 架构测试通过。
- 全量测试通过，27 个测试通过。
- 打包通过，生成 `MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar`。

## 下一阶段建议

下一阶段建议只做“真实身份领域迁移”，不要同时改前台、后台管理端和部署。

建议新计划命名：

`docs/superpowers/plans/2026-05-24-backend-v2-identity-domain-migration.zh-CN.md`

建议覆盖：

- 盘点旧表：`t_user_auth`、`t_user_info`、`t_user_role`、`t_role`、`t_resource`。
- 设计 V2 identity 表结构和 Flyway 迁移。
- 用真实仓储替换 `ConfiguredUserCredentialReader`。
- 明确角色、菜单、资源权限是否继续保留动态模型。
- 评估旧 Redis 登录态与 V2 Bearer JWT 的迁移策略。
- 补充身份仓储、登录、权限加载的回归测试。

## 风险提示

- 如果直接让前端接当前配置账号体系，后续切真实用户表时会再次改接口和联调逻辑。
- 如果不先盘点旧权限模型，`ADMIN/USER` 二级角色可能会低估后台管理端权限复杂度。
- 如果 token 撤销长期保留内存实现，多实例部署和服务重启后登出语义会不可靠。
- 如果生产环境未覆盖 `MYBLOG_JWT_SECRET`，默认 secret 会成为安全隐患。
