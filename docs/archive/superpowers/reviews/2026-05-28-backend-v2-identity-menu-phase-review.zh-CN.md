# 后端 V2 身份资料与菜单阶段复盘

## 阶段结论

本阶段已经完成后端 V2 身份基盘的主要闭环：真实账号可以登录，登录成功会写入审计字段，Bearer JWT 可以识别当前用户，`/api/auth/me` 可以返回当前用户资料，`/api/admin/user/menus` 可以返回当前管理员的后台菜单树。

这仍然属于重构基盘阶段，不是正式业务模块迁移。它的价值是为后续后台 V2、前台 V2 和业务域迁移提供稳定的身份、安全和菜单接口边界。

## 已完成范围

- 真实账号登录：从旧库 `t_user_auth`、`t_user_info`、`t_user_role`、`t_role` 读取账号、密码哈希和角色。
- 登录安全：保留 V2 Bearer JWT 签发、解析、撤销和统一 401/403 JSON 响应。
- 登录审计：登录成功后回写 `t_user_auth.last_login_time` 和 `t_user_auth.ip_address`。
- 当前用户资料：`GET /api/auth/me` 返回账号 ID、用户资料 ID、用户名、昵称、头像、邮箱和角色。
- 后台菜单：`GET /api/admin/user/menus` 按当前登录用户读取旧表菜单树。
- 后台权限保护：`/api/admin/**` 继续由 Spring Security 固定要求 `ADMIN` 角色。
- 测试基线：H2 测试迁移已补齐身份、角色、菜单和角色菜单测试表。

## 当前接口形态

### `POST /api/auth/login`

请求：

```json
{
  "username": "tongyibin1@gmail.com",
  "password": "<本地真实密码>"
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
    "expiresAt": "2026-05-28T12:00:00Z",
    "user": {
      "id": "1",
      "username": "tongyibin1@gmail.com",
      "roles": ["ADMIN"]
    }
  }
}
```

失败响应仍统一为：

```json
{
  "success": false,
  "code": "BAD_CREDENTIALS",
  "message": "用户名或密码错误",
  "data": null
}
```

### `GET /api/auth/me`

需要 Bearer Token。成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "id": "1",
    "userInfoId": "1",
    "username": "tongyibin1@gmail.com",
    "nickname": "<昵称>",
    "avatar": "<头像地址>",
    "email": "tongyibin1@gmail.com",
    "roles": ["ADMIN"]
  }
}
```

### `GET /api/admin/user/menus`

需要 Bearer Token，且当前用户必须拥有 `ADMIN` 角色。成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": [
    {
      "name": "首页",
      "path": "/",
      "component": "Layout",
      "icon": "Home",
      "hidden": false,
      "children": [
        {
          "name": "首页",
          "path": "",
          "component": "Layout",
          "icon": "Home",
          "hidden": false,
          "children": []
        }
      ]
    }
  ]
}
```

## 架构边界

### `modules.identity.domain`

身份领域层现在包含认证、资料和菜单查询端口：

- `UserCredentialReader`
- `LoginAuditRecorder`
- `CurrentUserProfileReader`
- `UserMenuReader`
- `AuthenticatedUser`
- `CurrentUserProfile`
- `UserMenu`

领域层不依赖 Spring MVC、Spring Security、`JdbcTemplate` 或旧系统实体。

### `modules.identity.application`

应用层承载登录和查询用例：

- `AuthService` 负责登录、签发 token、登出和登录审计调用。
- `IdentityQueryService` 负责当前用户资料和菜单查询编排。

### `modules.identity.infrastructure`

基础设施层继续兼容旧表：

- `DatabaseUserCredentialReader`
- `DatabaseLoginAuditRecorder`
- `DatabaseCurrentUserProfileReader`
- `DatabaseUserMenuReader`

当前实现只读取或更新必要旧表，不引入 MyBatis Plus 或 JPA。

### `modules.identity.api`

API 层负责 HTTP 入口和响应 DTO：

- `AuthController`
- `AdminIdentityController`
- `LoginRequest`
- `LoginResponse`
- `MeResponse`
- `UserMenuResponse`

## 数据库取舍

本阶段没有改真实 MySQL 表结构，这是有意控制范围。

当前旧表已经能支撑身份基盘：

- `t_user_auth`
- `t_user_info`
- `t_user_role`
- `t_role`
- `t_menu`
- `t_role_menu`

但数据库治理仍然需要后续专项处理：

- `t_user_role` 建议后续补 `user_id + role_id` 唯一约束。
- `t_role_menu` 建议后续补 `role_id + menu_id` 唯一约束。
- `t_user_auth.user_info_id`、`t_user_role.user_id`、`t_role_menu.role_id`、`t_role_menu.menu_id` 等关系建议先写完整性校验，再评估是否加外键。
- `t_resource` 和 `t_role_resource` 暂不作为 V2 运行时鉴权核心，后续只在后台权限管理确实需要时单独设计。

我不建议现在直接改真实库。原因是旧线上服务仍依赖这些表，直接加约束或改结构可能影响旧服务写入、删除和权限维护逻辑。更稳的做法是先按业务域迁移，等每个域边界清楚后再落地对应迁移脚本和数据校验。

## 已验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCurrentUserProfileReaderTest,DatabaseUserMenuReaderTest,AuthControllerTest,AdminIdentityControllerTest,SecurityConfigTest'
mvn -f MyBlog-springboot-v2/pom.xml test
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

验证结果：

- 身份资料与菜单相关测试通过，17 个测试，0 失败，0 错误。
- 全量测试通过，49 个测试，0 失败，0 错误。
- 打包通过，生成 `MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar`。
- 本地 MySQL 只读确认：`t_menu=35`，`t_role_menu=70`。
- 本地真实账号冒烟确认：登录成功，`/api/auth/me` 返回用户资料字段，`/api/admin/user/menus` 返回菜单数组。

## 当前临时能力

- token 撤销仍是内存存储，服务重启会丢失撤销状态。
- `/api/admin/**` 目前只做固定 `ADMIN` 角色保护，没有恢复旧动态接口资源权限。
- 菜单接口只读，不包含菜单管理、角色管理或权限管理 CRUD。
- 真实库表结构暂不调整，V2 目前走兼容读取。
- 前台和后台管理端还没有接入这些 V2 接口。

## 下一阶段建议

下一阶段建议进入第一个业务域迁移前，先写一份 `content` 业务域迁移实施计划。

建议新计划命名：

`docs/superpowers/plans/2026-05-28-backend-v2-content-domain-migration.zh-CN.md`

建议覆盖：

- 盘点旧后端文章、分类、标签相关 Controller、Service、Mapper 和 DTO。
- 盘点旧表：`t_article`、`t_category`、`t_tag`、`t_article_tag`。
- 明确前台读者接口和后台管理接口的拆分边界。
- 优先迁移只读查询能力：文章列表、文章详情、分类列表、标签列表。
- 后台写操作先不急，等只读 API 契约稳定后再做保存、更新、删除、上下架。
- 数据库先兼容旧表，不立即改真实表结构；需要改表时单独写 Flyway 迁移和数据校验。

## 风险提示

- 如果现在直接迁文章写操作，权限、草稿、置顶、推荐、密码文章和标签关系容易混在一起。
- 如果现在直接改真实库表结构，旧线上服务兼容风险高。
- 如果不先定义 `content` API 契约，前台和后台后续会重复适配。
- 如果动态资源权限现在恢复到运行时，复杂度会压过当前个人博客体量的实际收益。
