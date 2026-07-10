# 请求处理流程

> 状态：当前有效
> 适用范围：MyBlog V2 后端 HTTP 请求
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/*/web/`
> 权威程度：架构权威说明

## 通用流程

```text
HTTP request
  -> CORS / Spring Security / JwtAuthenticationFilter
  -> Controller + Bean Validation
  -> Web request 到 application command/query 的映射
  -> Application service
  -> Domain rule / Repository port / 其他模块 application 能力
  -> Infrastructure adapter / Mapper / external service
  -> Application result
  -> Web VO
  -> ApiResponse<T>
```

Web 层只负责协议边界，不直接访问 Entity、Mapper 或其他模块内部类型。Application 层负责用例、事务和跨模块协作。Domain 层只表达业务规则。Infrastructure 层处理持久化和外部服务。

## 公开请求

`application.yml` 的 `myblog.security.public-endpoints` 使用 HTTP method 与 path 共同声明匿名入口。公开接口仍经过参数校验、限流、内容清洗和统一异常处理。

公开读取主要位于 `/api/public/**`；登录和 refresh 位于 `/api/auth/**`。新增公开入口时必须同步配置白名单、API 契约和安全测试。

## 后台请求

`JwtAuthenticationFilter` 解析 Bearer access token，`AccessTokenVerifier` 校验签名、issuer、有效期、token 类型、账号状态和 `token_version`，成功后写入 `SecurityContext`。

- 后台读取通常允许 `ADMIN` 和 `DEMO`。
- 后台写入只允许 `ADMIN`。
- application 层继续执行敏感字段裁剪和关键权限复核，不能只依赖前端隐藏按钮。

## 事务

- 写用例的事务边界位于 application service 或专用 transaction service。
- BCrypt、文件读取等高成本操作不应无必要地占用数据库事务。
- refresh 轮换、改密撤销、评论状态与计数等一致性场景必须在同一事务完成。
- 并发敏感场景使用行锁、条件更新或版本条件，并由 H2/MySQL 测试验证。

## 错误响应

可预期业务失败抛出 `ApiException`，参数和框架异常由 `GlobalExceptionHandler` 转换，认证授权失败由 `SecurityProblemSupport` 转换。所有失败保持 `ApiResponse` 结构，未预期异常返回 `99999` 且不暴露内部细节。

具体契约见 `../rules/api-response.md` 和 `../rules/error-handling.md`。
