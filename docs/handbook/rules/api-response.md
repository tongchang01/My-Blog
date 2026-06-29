# API 响应与契约规则

> 状态：当前有效
> 适用范围：MyBlog V2 所有 Controller 和 API 契约文档
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/ApiResponse.java`、`PageResponse.java`
> 权威程度：规则

## 本文档回答什么问题

本文档规定 V2 API 返回什么 JSON 结构、分页怎么表达、字段如何命名、错误码如何分配，以及 OpenAPI 和测试应如何约束这些契约。

## 1. 统一响应包装

Controller 成功和失败响应统一使用 `ApiResponse<T>`：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {}
}
```

字段固定为：

| 字段 | 类型 | 含义 |
|------|------|------|
| `code` | string | 5 位业务码，成功固定为 `00000` |
| `msg` | string | 人类可读消息，成功固定为 `success` |
| `data` | any | 成功响应载荷；失败时为 `null` |

禁止：

- 改成 `message`、`result`、`payload` 等字段名。
- HTTP 200 中返回 `success: false` 表达失败。
- 同一接口不同分支返回完全不同结构的 `data`。
- 直接返回数据库 Entity。

## 2. 成功响应

成功响应使用：

```java
return ApiResponse.ok(data);
```

成功示例：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": "1930000000000000000"
  }
}
```

如果成功但没有业务载荷，可以返回 `ApiResponse.ok(null)`；不要自造其它成功结构。

## 3. 失败响应

失败响应由 `GlobalExceptionHandler` 或 `SecurityProblemSupport` 统一写出：

```json
{
  "code": "90001",
  "msg": "参数校验失败",
  "data": null
}
```

失败时必须同时满足：

- HTTP 状态码符合错误语义。
- `code` 是稳定业务码。
- `msg` 不暴露 SQL、堆栈、表名、密钥、内部路径。
- `data` 为 `null`。

## 4. 分页响应

分页数据统一使用 `PageResponse<T>`，再包进 `ApiResponse<PageResponse<T>>`。

当前实际结构为：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10
  }
}
```

字段规则：

| 字段 | 规则 |
|------|------|
| `records` | 当前页记录。无数据时返回空数组 `[]`，不是 `null` |
| `total` | 符合查询条件的总数，不能为负数 |
| `page` | 当前页码，从 1 开始 |
| `size` | 每页大小，必须大于 0 |

注意：当前 `PageResponse` 不包含 `pages` 字段。若未来需要总页数，必须先修改 `PageResponse`、API 契约和测试，再更新本文档。

## 5. 请求参数

请求模型命名建议：

```text
{Resource}{Action}Request
```

示例：

- `LoginRequest`
- `RefreshTokenRequest`
- `ArticleCreateRequest`
- `SiteConfigUpdateRequest`

规则：

- HTTP body 入参使用请求对象。
- 必填字段使用 `@NotNull`、`@NotBlank` 等 Bean Validation。
- 字符串长度使用 `@Size`。
- 枚举字段优先使用枚举类型，不用裸字符串长期维护。
- 复杂 presence 语义应隔离在 Web request，不暴露到 OpenAPI 内部实现类型。

## 6. 响应对象

响应模型命名建议：

```text
{Resource}VO
{Resource}{Aspect}VO
```

规则：

- 响应对象不得直接复用数据库 Entity。
- ID 返回给前端时，Snowflake ID 必须按 JSON string 表达，避免 JavaScript 精度丢失。
- 时间字段使用 ISO-8601 本地时间字符串，按 Asia/Tokyo 语义，不带 `Z` 或 `+09:00` 后缀。
- nullable 字段应在 API 文档中说明含义。
- 敏感字段对 DEMO 是否返回，应由 application 层统一裁剪，不在 Controller、Repository 多点判断。

## 7. URL 与字段命名

| 位置 | 风格 | 示例 |
|------|------|------|
| URL path | kebab-case | `/api/admin/friend-links` |
| Query 参数 | camelCase | `articleId`、`createdAt` |
| Body 字段 | camelCase | `displayName`、`publishedAt` |
| 路径参数 | camelCase | `/api/admin/articles/{id}` |

## 8. 路径前缀与权限语义

| 前缀 | 用途 | 鉴权语义 |
|------|------|----------|
| `/api/public/**` | 前台公开接口 | 匿名访问 |
| `/api/auth/**` | 登录、刷新、退出、当前用户 | 逐项按 method + path 配置 |
| `/api/admin/** GET` | 后台读 | 通常 ADMIN + DEMO |
| `/api/admin/** POST/PUT/PATCH/DELETE` | 后台写 | ADMIN |
| `/actuator/health` | 健康检查 | 匿名访问 |

实际白名单以 `application.yml` 的 `myblog.security.public-endpoints` 和 `SecurityConfig` 为准。新增公开接口时必须同步配置 method + path。

## 9. 错误码空间

错误码为 5 位字符串，前导零必须保留。

| 段 | 含义 |
|----|------|
| `00000` | 成功 |
| `10xxx` | identity |
| `20xxx` | content |
| `30xxx` | comment |
| `40xxx` | system |
| `50xxx` | stats |
| `90xxx` | common-infra、参数、限流、通用错误 |
| `99999` | 系统兜底错误 |

当前 `ApiErrorCode` 已存在：

| code | HTTP | 含义 |
|------|------|------|
| `90001` | 400 | 参数校验失败 |
| `90002` | 429 | 请求过于频繁 |
| `10001` | 401 | 用户名或密码错误 |
| `10002` | 401 | 登录状态已失效 |
| `10003` | 403 | 无权执行当前操作 |
| `90003` | 404 | 目标资源不存在 |
| `90004` | 409 | 当前操作与已有状态冲突 |
| `99999` | 500 | 系统内部错误 |

新增错误码前，必须确认前端确实需要稳定分支。能用现有通用语义表达时，不新增模块专用码。

## 10. OpenAPI / Knife4j

规则：

- Controller 类使用 `@Tag` 描述业务分组。
- 公开 operation 使用 `@Operation(summary = "...")` 描述面向调用方的动作。
- 关键字段使用 `@Schema` 说明权限、状态、时间、nullable、枚举和敏感语义。
- 普通自解释字段不机械添加冗余注解。
- API 文档仅在 local/test 环境启用，生产关闭。

## 11. 测试要求

API 测试至少覆盖：

- 成功响应：`code = "00000"`，`msg = "success"`，`data` 结构正确。
- 失败响应：HTTP 状态码、`code`、`msg`、`data = null`。
- 分页响应：`records`、`total`、`page`、`size`。
- ID 契约：前端可见 Snowflake ID 为 string。
- 权限边界：ADMIN、DEMO、GUEST 的响应与状态码。

## 相关文档

- 异常处理规则：`error-handling.md`
- 安全基线：`security-baseline.md`（待迁移）
- 接口契约目录：`../api/`（待迁移）
- 包结构规则：`package-layout.md`
