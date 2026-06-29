# 异常与错误处理规则

> 状态：当前有效
> 适用范围：MyBlog V2 后端所有业务代码、Web 层和安全异常响应
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/`、`common/security/SecurityProblemSupport.java`
> 权威程度：规则

## 本文档回答什么问题

本文档规定 V2 后端应该抛什么异常、由谁转换成 HTTP 响应、客户端会看到什么，以及哪些错误信息禁止暴露。

## 1. 总原则

- 可预期业务失败使用 `ApiException`。
- `ApiException` 必须携带 `ApiErrorCode`。
- 参数校验、请求体解析、上传文件大小、缺少 multipart 字段等由 `GlobalExceptionHandler` 统一转换。
- Spring Security 的认证和授权失败由 `SecurityProblemSupport` 写成统一 JSON。
- 未预期异常统一返回 `99999`，不暴露内部细节。
- Controller 和 Service 不应自己 `try-catch` 后手写错误响应。

## 2. 异常分层

| 异常来源 | 示例 | 处理者 | 响应 |
|----------|------|--------|------|
| 业务异常 | `throw new ApiException(ApiErrorCode.CONFLICT)` | `GlobalExceptionHandler` | 使用错误码自带 HTTP 状态 |
| Bean Validation | `@Valid` 失败 | `GlobalExceptionHandler` | 400 + `90001` |
| 缺少 query 参数 | `MissingServletRequestParameterException` | `GlobalExceptionHandler` | 400 + `90001` |
| 请求体格式错误 | `HttpMessageNotReadableException` | `GlobalExceptionHandler` | 400 + `90001` |
| HTTP 方法不支持 | `HttpRequestMethodNotSupportedException` | `GlobalExceptionHandler` | 405 + `90001` |
| 上传过大 | `MaxUploadSizeExceededException` | `GlobalExceptionHandler` | 400 + `90001` |
| 缺少上传文件字段 | `MissingServletRequestPartException` | `GlobalExceptionHandler` | 400 + `90001` |
| 未匹配接口 | `NoResourceFoundException` | `GlobalExceptionHandler` | 404 + `90003` |
| 未认证 | 缺 token、token 无效、token 过期 | `SecurityProblemSupport` | 401 + `10002` |
| 无权限 | 角色不足、DEMO 写操作 | `SecurityProblemSupport` | 403 + `10003` |
| 未预期异常 | NPE、外部依赖异常、SQL 异常 | `GlobalExceptionHandler` | 500 + `99999` |

## 3. ApiException 使用规则

正例：

```java
throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
```

规则：

- 应用层和领域边界遇到可预期业务失败时抛 `ApiException`。
- 错误码优先复用 `ApiErrorCode` 中已有稳定语义。
- 业务消息可以补充上下文，但不能包含内部实现细节。
- 5xx 类 `ApiException` 的原始消息不会返回给前端，只返回错误码默认消息。

禁止：

```java
throw new RuntimeException("select * from t_article failed");
```

## 4. ApiErrorCode 规则

`ApiErrorCode` 每项包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 返回给前端的 5 位业务码 |
| `status` | `HttpStatus` | HTTP 状态 |
| `defaultMessage` | string | 中文默认消息 |

当前错误码：

| 枚举 | code | HTTP | 默认消息 |
|------|------|------|----------|
| `VALIDATION_ERROR` | `90001` | 400 | 参数校验失败 |
| `RATE_LIMITED` | `90002` | 429 | 请求过于频繁 |
| `BAD_CREDENTIALS` | `10001` | 401 | 用户名或密码错误 |
| `INVALID_TOKEN` | `10002` | 401 | 登录状态已失效 |
| `FORBIDDEN` | `10003` | 403 | 无权执行当前操作 |
| `NOT_FOUND` | `90003` | 404 | 目标资源不存在 |
| `CONFLICT` | `90004` | 409 | 当前操作与已有状态冲突 |
| `INTERNAL_ERROR` | `99999` | 500 | 系统内部错误 |

新增错误码 checklist：

1. 确认前端需要对此原因做稳定分支。
2. 确认现有错误码不能表达。
3. 选择所属模块码段。
4. 设置正确 HTTP 状态。
5. 写中文默认消息。
6. 更新 API 契约和测试。

## 5. HTTP 状态语义

| HTTP | 用途 |
|------|------|
| 200 | 成功响应 |
| 400 | 参数错误、请求体格式错误、业务校验失败 |
| 401 | 未认证、token 缺失、token 无效、token 过期、token version 不匹配 |
| 403 | 已认证但无权限 |
| 404 | 资源不存在、接口不存在 |
| 405 | URL 存在但 HTTP 方法不支持 |
| 409 | 唯一冲突、状态冲突、引用冲突 |
| 429 | 限流 |
| 500 | 未预期服务端错误 |

认证失败和授权失败必须区分：

- 认证问题：`10002` + 401。
- 授权问题：`10003` + 403。

## 6. 统一错误响应

失败响应结构固定为：

```json
{
  "code": "90003",
  "msg": "目标资源不存在",
  "data": null
}
```

字段名不得改动。失败响应不得携带内部 exception class、SQL、表名、堆栈、服务器路径或密钥信息。

## 7. 参数校验消息

当前实现规则：

- Bean Validation 失败时，返回第一个字段错误的 default message。
- 缺少必填 query 参数时，返回 `缺少必填请求参数: {name}`。
- 请求体无法解析时，返回 `请求体格式错误`。
- 上传超过统一限制时，返回 `上传文件不能超过10 MiB`。
- 缺少上传文件字段时，返回 `缺少上传文件`。

新增请求模型时，应写清楚 Bean Validation 消息，避免前端拿到默认英文或含糊错误。

## 8. 日志策略

当前 `GlobalExceptionHandler` 对未预期异常记录 error 日志并包含堆栈。

规则：

- 未预期异常必须记录服务端日志。
- 可预期业务异常不应在每个 catch 点重复打日志。
- 日志中不得输出密码、token、密钥或上传文件原始敏感内容。
- 前端响应不得包含服务端内部日志细节。

## 9. Controller 禁止事项

禁止在 Controller 中：

- `try-catch` 后手动返回 `ApiResponse.fail(...)`。
- 把异常吞掉后返回成功。
- 根据异常字符串判断业务分支。
- 直接暴露 Java exception message 给前端。
- 返回 HTTP 200 表达失败。

如确实有不得已的业务原因，需要写明原因，并优先考虑是否应提炼为 application 层用例或全局异常规则。

## 10. 测试要求

至少覆盖：

- 关键业务异常的 HTTP 状态和 `code`。
- 参数校验失败返回 `90001`。
- 未认证返回 `10002`。
- 无权限返回 `10003`。
- 未匹配接口返回 `90003`。
- 未预期异常返回 `99999` 且不含堆栈。
- 5xx `ApiException` 不把自定义内部消息返回给前端。

## 相关文档

- API 响应规则：`api-response.md`
- 安全基线：`security-baseline.md`（待迁移）
- 包结构规则：`package-layout.md`
