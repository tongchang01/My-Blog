# API 响应与契约规则

> 状态：当前有效
> 适用范围：V2 HTTP 接口与前端 API 客户端
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/`、`frontend/apps/blog/src/`、`frontend/apps/admin/src/`
> 权威程度：规则

## 统一响应

所有成功与失败响应使用 `ApiResponse<T>`：

```json
{ "code": "00000", "msg": "success", "data": {} }
```

- 成功固定为 `code = "00000"`、`msg = "success"`。
- 失败使用与 HTTP 状态一致的业务码，`data` 固定为 `null`。
- 不返回数据库 Entity，不使用 HTTP 200 包装业务失败。
- 错误消息不得暴露 SQL、堆栈、密钥、服务器路径和内部类型。

分页载荷使用 `PageResponse<T>`：

```json
{ "records": [], "total": 0, "page": 1, "size": 10 }
```

`records` 不为 `null`，page 从 1 开始，size 大于 0。当前契约没有 `pages` 字段。

## 数据类型

- Java Snowflake ID 向前端序列化为 JSON string，避免 JavaScript 精度丢失。
- 日期时间采用 `yyyy-MM-dd'T'HH:mm:ss` 本地字符串，语义固定为 `Asia/Tokyo`，不附加 `Z` 或 offset。
- URL path 使用 kebab-case；query 与 JSON 字段使用 camelCase。
- Request、application Command/Query、application Result 和 Web VO 分离。
- nullable、枚举、权限和敏感字段必须在契约或 OpenAPI 中说明。

## 路径与权限

| 路径 | 语义 |
| --- | --- |
| `/api/public/**` | 配置白名单后允许匿名访问 |
| `/api/auth/**` | 按 method + path 单独配置 |
| `/api/admin/**` GET | 当前列出的读接口允许 ADMIN 和 DEMO |
| `/api/admin/**` 写方法 | ADMIN |
| `/actuator/health` | 匿名健康检查 |

公开权限以 `application.yml` 的 method + path 白名单和 `SecurityConfig` 为准，不能只依据命名推断。

## 当前错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| `90001` | 400 | 参数校验失败 |
| `90002` | 429 | 请求过于频繁 |
| `10001` | 401 | 用户名或密码错误 |
| `10002` | 401 | 登录状态失效 |
| `10003` | 403 | 权限不足 |
| `90003` | 404 | 目标资源不存在 |
| `90004` | 409 | 状态或唯一性冲突 |
| `99999` | 500 | 未预期服务端错误 |

只有前端需要稳定区分新的失败语义时才新增错误码，并同步修改契约与测试。

## OpenAPI 与测试

- Controller 使用中文 `@Tag`，公开 operation 使用中文 `@Operation`。
- 关键字段用 `@Schema` 说明，避免为自解释字段添加机械注释。
- springdoc 与 Knife4j 只在 local 启用，prod 关闭。
- Controller 测试至少验证 HTTP 状态、code、data 结构、ID string 和角色边界。

异常转换见 `error-handling.md`，接口明细见 `../api/`。
