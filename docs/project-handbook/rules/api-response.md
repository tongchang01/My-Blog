# API 响应与契约规则

> 本文档回答："API 返回什么结构？分页怎么写？字段命名？"
> 适用范围：V2 所有 Controller。
> 当前实现：`com.tyb.myblog.v2.common.web.ApiResponse`、`PageResponse`

## 1. 统一响应包装

所有 Controller 返回 `ApiResponse<T>`：

```json
{
  "code": "OK",
  "message": "ok",
  "data": { ... }
}
```

- `code`：错误码字符串（成功为 `OK`，失败为对应 `ApiErrorCode`）
- `message`：人类可读消息（成功时为 `"ok"` 或省略；失败时为中文错误说明）
- `data`：业务数据；失败时为 `null`

🔴 禁止：HTTP 200 + body 里塞 `success: false`，不符合 REST 语义。失败用对应 HTTP 状态码（见 `error-handling.md` §4）。

## 2. 分页响应

统一用 `PageResponse<T>`：

```json
{
  "code": "OK",
  "data": {
    "list": [ ... ],
    "total": 123,
    "page": 1,
    "size": 10
  }
}
```

- `page` 从 **1** 开始（不是 0）
- `size` 默认 10，最大 100（超出报 `VALIDATION_ERROR`）
- 即使无数据，`list` 也是空数组 `[]`，不是 `null`

## 3. 请求参数

- 入参对象命名：`{Resource}{Action}Request`，如 `CommentCreateRequest`、`ArticleSearchRequest`
- 必须用 `@Valid` + Bean Validation 注解校验
- 必填字段标 `@NotNull` / `@NotBlank`
- 字符串长度标 `@Size(max=...)`
- 枚举字段用枚举类型而非 String

## 4. 出参对象

- 命名：`{Resource}Response`、`{Resource}{Aspect}Response`，如 `CommentResponse`、`ArticleListResponse`
- 字段必须有 Javadoc 说明"前端如何理解该字段"（不是描述数据库来源）
- 时间字段统一 ISO-8601 字符串，时区 UTC
- 金额、数量等数字字段明确单位

## 5. 命名约定

| 位置              | 命名风格       | 例                           |
| --------------- | ---------- | --------------------------- |
| URL path        | kebab-case | `/api/admin/comment-audits` |
| Query / Body 字段 | camelCase  | `articleId`、`createdAt`     |
| 路径参数            | camelCase  | `/api/articles/{articleId}` |

## 6. URL 路径前缀约定

| 前缀            | 用途                       | 鉴权            |
| ------------- | ------------------------ | ------------- |
| `/api/`       | 前台公开 / 半公开接口             | 视具体接口（白名单或登录） |
| `/api/admin/` | 后台管理接口                   | 必须 ADMIN 角色   |
| `/actuator/`  | 运维（Spring Boot Actuator） | 仅 health 公开   |

## 7. 错误响应示例

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "code": "COMMENT_NOT_FOUND",
  "message": "评论不存在",
  "data": null
}
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": "VALIDATION_ERROR",
  "message": "content: 评论内容不能为空",
  "data": null
}
```

## 8. 不允许的做法

- ❌ 同一接口在不同分支返回不同结构的 `data`
- ❌ 字段值为空时整个字段省略不返
- ❌ 用 `null` / `""` / `0` 混乱表达"无数据"
- ❌ 把内部 Entity 当 Response 返回（必须先转 `XxxResponse`）

## 9. Swagger / OpenAPI

- 每个 Controller 类用 `@Tag(name="...", description="...")` 描述
- 每个公开方法用 `@Operation(summary="...")` 描述
- DTO 字段用 `@Schema(description="...")` 描述（中文，与 Javadoc 保持一致）

## 10. 测试要求

- 成功路径：断言 `code=OK` 且 `data` 结构正确
- 失败路径：断言 HTTP 状态码 + `code` 错误码 + `message` 中文
- 分页：断言 `total / page / size / list.length` 一致
