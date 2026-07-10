# 新增 REST API

> 状态：当前有效
> 适用范围：V2 现有业务模块
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：标准流程

1. 确认所属模块、公开或后台路径、角色、数据裁剪和是否需要新错误码。
2. 在 web 定义 Request 与 VO，加入 Bean Validation 和必要的中文 OpenAPI 描述。
3. 在 application 定义独立 Command/Query/Result，不复用 Web DTO。
4. 在 application service 编排权限、领域规则、事务和端口调用；可预期失败抛 `ApiException`。
5. Controller 只做 HTTP 映射和类型转换，返回 `ApiResponse<T>`。
6. 匿名接口在 `myblog.security.public-endpoints` 增加精确 method + path；后台读写同步更新 `SecurityConfig` 与角色测试。
7. 更新 `../api/` 中对应主题的契约，不为单个小接口新建重复文档。
8. 编写 application 与 Controller 测试，覆盖成功、校验、业务失败、认证和授权。
9. 先运行相关测试，再按风险运行 `mvn test`。

路径、响应和异常规则分别见 `../rules/package-layout.md`、`../rules/api-response.md`、`../rules/error-handling.md`。
