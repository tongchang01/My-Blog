# 异常与错误处理规则

> 本文档回答："抛什么异常、谁来接、客户端看到什么？"
> 适用范围：V2 所有业务代码与 Web 层。
> 当前实现：`com.tyb.myblog.v2.common.error.GlobalExceptionHandler`、`ApiErrorCode`、`ApiException`

## 1. 总原则

- 业务异常用 `ApiException`，携带 `ApiErrorCode`
- 系统异常（NPE、SQL 错误等）由全局兜底，**不**暴露内部细节
- 所有异常通过 `@RestControllerAdvice` 统一格式返回
- Controller / Service 方法**不**自己 `try-catch` 后返回错误响应（除非有明确业务原因）

## 2. 异常分层

| 异常 | 何时抛 | 谁处理 |
|------|--------|--------|
| `ApiException(ApiErrorCode, msg)` | 业务规则违反（如评论已删除不可再审核） | `GlobalExceptionHandler` 返回 400/403/404 等 |
| Spring 校验异常（`MethodArgumentNotValidException` 等） | `@Valid` 参数校验失败 | `GlobalExceptionHandler` 返回 `VALIDATION_ERROR` |
| Spring Security 异常（`AuthenticationException`、`AccessDeniedException`） | 鉴权失败 | Security 配置中的 EntryPoint / DeniedHandler 统一处理 |
| 其他未捕获异常 | 系统级 bug、外部依赖故障 | `GlobalExceptionHandler` 返回 `INTERNAL_ERROR`，**不暴露堆栈** |

## 3. ApiErrorCode 设计

- 每个错误码是一个枚举值：`code`、`httpStatus`、`defaultMessage`
- 新增错误码时必须明确：
  - 业务场景
  - HTTP 状态码（按下面"状态码语义"选择）
  - 默认消息（中文）
- 错误码命名约定：`{MODULE}_{SUBJECT}_{REASON}`，如 `COMMENT_NOT_FOUND`、`AUTH_INVALID_TOKEN`

## 4. HTTP 状态码语义

| 状态 | 用途 |
|------|------|
| 200 | 正常返回 |
| 400 | 参数错误、业务校验失败 |
| 401 | 未认证（缺 token / token 无效 / token 过期 / token 已撤销） |
| 403 | 已认证但无权限（角色不够、不是资源所有者） |
| 404 | 资源不存在 |
| 409 | 资源冲突（重复创建、状态冲突等） |
| 500 | 系统错误（未预期异常） |

⚠️ **401 vs 403 的区分**：身份验证问题用 401，授权问题用 403。两者不可混用。

## 5. 统一响应格式

成功：
```json
{ "code": "OK", "message": "ok", "data": {...} }
```

失败：
```json
{ "code": "COMMENT_NOT_FOUND", "message": "评论不存在", "data": null }
```

字段语义见 `api-response.md`。

## 6. 业务异常正例

```java
// application 层
public CommentCreateResult createComment(CreateCommentCommand cmd) {
    Article article = articleRepository.findById(cmd.articleId())
        .orElseThrow(() -> new ApiException(ApiErrorCode.ARTICLE_NOT_FOUND));

    if (!article.allowsComment()) {
        throw new ApiException(ApiErrorCode.COMMENT_FORBIDDEN_BY_ARTICLE);
    }
    ...
}
```

## 7. 禁止事项

- ❌ Controller 自己 `try-catch` 后 `return ApiResponse.fail(...)`（除非有不得已的业务原因，并写注释说明）
- ❌ 抛 `RuntimeException("xxx")` 把消息直接展示给用户
- ❌ 异常消息里暴露内部表名、SQL、堆栈
- ❌ 用 HTTP 200 + body 里塞 error 字段（违反 REST 语义）

## 8. 日志策略

- `GlobalExceptionHandler` 处理 `ApiException` 时记录 **warn** 级日志（业务可预期）
- 处理未预期异常时记录 **error** 级日志（含堆栈）
- 不要在每个 catch 点都打日志，避免重复

## 9. 测试要求

- 业务异常：`{Module}ControllerTest` 中验证关键 ApiErrorCode 的 HTTP 状态与响应体
- 参数校验：覆盖必填、长度、格式等边界
- 全局兜底：`GlobalExceptionHandlerTest` 验证未知异常返回 500 且不含堆栈

## 10. 例外

审计失败时（如登录后更新 `last_login_time` 失败），登录接口不签发 token 且返回 500。此为有意为之，见 `security-baseline.md` §8。
