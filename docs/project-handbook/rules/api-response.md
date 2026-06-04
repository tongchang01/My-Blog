# API 响应与契约规则

> 本文档回答："API 返回什么结构？分页怎么写？字段命名？"
> 适用范围：V2 所有 Controller。
> 相关：`product/decisions-draft.md` R6 B3 / B4 / B5、R7 D11
> 实现目标类：`com.tyb.myblog.v2.common.web.ApiResponse`、`PageResponse`

## 1. 统一响应包装

所有 Controller 返回 `ApiResponse<T>`：

```json
{
  "code": "00000",
  "msg": "success",
  "data": { ... }
}
```

- `code`：**String 类型**，5 位字符串错误码，前导零保留（成功固定 `"00000"`）。详见 §7
- `msg`：人类可读消息（成功时 `"success"`；失败时为对应错误码默认消息或上下文消息）
- `data`：业务数据；失败时为 `null`

🔴 禁止：HTTP 200 + body 里塞 `success: false`，不符合 REST 语义。失败用对应 HTTP 状态码（见 `error-handling.md` §4）。

🔴 字段名固定为 `code` / `msg` / `data`，不得改名（不要用 `message` / `result` / `payload` 等别名）。

## 2. 分页响应

统一用 `PageResponse<T>`（与 MyBatis-Plus `IPage` 一致）：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [ ... ],
    "total": 123,
    "page": 1,
    "size": 10,
    "pages": 13
  }
}
```

- `page` 从 **1** 开始（不是 0），与 MyBatis-Plus IPage 对齐
- `size` 默认 10，最大 100（超出报 `90001` 参数校验失败）
- `pages` = `ceil(total / size)`
- 即使无数据，`records` 也是空数组 `[]`，不是 `null`
- 入参格式：`?page=1&size=10`；`page < 1` 或 `size < 1` 一律 400

## 3. 请求参数

- 入参对象命名：`{Resource}{Action}Request`，如 `CommentCreateRequest`、`ArticleSearchRequest`
- 必须用 `@Valid` + Bean Validation 注解校验
- 必填字段标 `@NotNull` / `@NotBlank`
- 字符串长度标 `@Size(max=...)`
- 枚举字段用枚举类型而非 String

## 4. 出参对象

- 命名：`{Resource}VO`、`{Resource}{Aspect}VO`（详见 R6 B2 DTO 分层）
- 字段必须有 Javadoc 说明"前端如何理解该字段"（不是描述数据库来源）
- 时间字段统一 ISO-8601 字符串，**不带时区后缀**（如 `"2026-06-03T14:30:00"`），按 **Asia/Tokyo** 语义；前端直接渲染（详见 R7 D11）
- 金额、数量等数字字段明确单位

## 5. 命名约定

| 位置 | 命名风格 | 例 |
|------|---------|-----|
| URL path | kebab-case | `/api/admin/comment-audits` |
| Query / Body 字段 | camelCase | `articleId`、`createdAt` |
| 路径参数 | camelCase | `/api/articles/{articleId}` |

## 6. URL 路径前缀约定

| 前缀 | 用途 | 鉴权 |
|------|------|------|
| `/api/public/**` | 前台公开接口 | `permitAll()` |
| `/api/auth/**` | 登录 / 刷新 / 登出 | `permitAll()` |
| `/api/admin/** GET` | 后台读 | 默认 ADMIN+DEMO；敏感读单独标 ADMIN |
| `/api/admin/** POST/PUT/DELETE/PATCH` | 后台写 | 仅 ADMIN |
| `/actuator/**` | 运维（Spring Boot Actuator） | 仅 health 公开 |

详见 `product/decisions-draft.md` R1 衍生设计。

## 7. 错误码空间（5 位 MMSSS）

| MM | 模块 |
|---|---|
| 00 | 通用成功（`00000` = success） |
| 10 | identity |
| 20 | content |
| 30 | comment |
| 40 | system |
| 50 | stats |
| 90 | common-infra（参数校验 / 限流 / 通用错） |
| 99 | 系统兜底（`99999` = 系统错误，HTTP 500） |

**`9X` 段预留给基础设施，业务模块不占。** 模块内 `SSS` 从 `001` 起递增，按业务追加，不复用编号。

常用错误码示例：

| code | HTTP | 含义 |
|---|---|---|
| `00000` | 200 | success |
| `10001` | 401 | 用户名或密码错误 |
| `10002` | 401 | token 已失效 |
| `20001` | 404 | 文章不存在 |
| `20002` | 401 | 文章密码错误 |
| `20003` | 400 | 文章定时发布时间必须晚于当前时间 |
| `30001` | 200 | 评论命中关键词进入待审（成功语义，业务码区分） |
| `30002` | 429 | 评论频率超限 |
| `90001` | 400 | 参数校验失败 |
| `90002` | 429 | 请求过于频繁（限流） |
| `99999` | 500 | 系统错误（兜底） |

详见 `error-handling.md` §3。

## 8. 错误响应示例

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "code": "20001",
  "msg": "文章不存在",
  "data": null
}
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": "90001",
  "msg": "content: 评论内容不能为空",
  "data": null
}
```

## 9. 不允许的做法

- ❌ 同一接口在不同分支返回不同结构的 `data`
- ❌ 字段值为空时整个字段省略不返
- ❌ 用 `null` / `""` / `0` 混乱表达"无数据"
- ❌ 把内部 Entity 当 VO 返回（必须先转 `XxxVO`，由 MapStruct 生成）
- ❌ 时间字段输出带时区后缀（如 `+09:00` / `Z`）

## 10. Knife4j / OpenAPI

- 每个 Controller 类用 `@Tag(name="...", description="...")` 描述
- 每个公开方法用 `@Operation(summary="...")` 描述
- DTO 字段用 `@Schema(description="...")` 描述（中文，与 Javadoc 保持一致）
- 仅 `dev` / `test` profile 启用（详见 R7 D9）

## 11. 测试要求

- 成功路径：断言 `code="00000"` 且 `data` 结构正确
- 失败路径：断言 HTTP 状态码 + `code` 错误码 + `msg` 非空
- 分页：断言 `total / page / size / pages / records.length` 一致
