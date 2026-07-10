# 异常与错误处理规则

> 状态：当前有效
> 适用范围：V2 Web、安全和应用层
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityProblemSupport.java`
> 权威程度：规则

## 处理边界

- 可预期业务失败抛出携带 `ApiErrorCode` 的 `ApiException`。
- Bean Validation、请求解析、multipart 和未匹配路由由 `GlobalExceptionHandler` 转换。
- 认证与授权失败由 `SecurityProblemSupport` 输出相同的 `ApiResponse` 结构。
- 未预期异常记录服务端堆栈并返回 `500 + 99999`，响应不包含内部异常消息。
- Controller 和 Service 不捕获异常后手写失败响应，也不吞掉异常返回成功。

| 场景 | HTTP | code |
| --- | --- | --- |
| 参数、请求体或上传格式错误 | 400 | `90001` |
| 未认证、token 无效或已撤销 | 401 | `10002` |
| 账号密码错误 | 401 | `10001` |
| 已认证但权限不足 | 403 | `10003` |
| 资源或路由不存在 | 404 | `90003` |
| HTTP 方法不支持 | 405 | `90001` |
| 状态、引用或唯一性冲突 | 409 | `90004` |
| 限流 | 429 | `90002` |
| 未预期异常 | 500 | `99999` |

## 消息与日志

- 4xx 可以使用经过审查的中文业务消息补充上下文。
- 5xx 始终向调用方返回默认消息。
- 参数校验消息必须稳定、中文且不包含实现细节。
- 服务端日志不得输出密码、token、JWT 密钥、数据库密码或上传内容。
- 可预期业务异常避免在多层重复记录相同日志。

## 测试

关键接口测试需同时断言 HTTP 状态、业务码、`data = null` 和敏感信息未泄漏。错误码定义与响应结构见 `api-response.md`。
