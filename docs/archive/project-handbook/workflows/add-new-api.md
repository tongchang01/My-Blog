# 新增 API 接口（SOP）

> 目标：在已有模块中新增一个 REST 接口，符合 V2 规范。

## 1. 前置思考

- 接口归属哪个模块？
- 前台 `/api/` 还是后台 `/api/admin/`？
- 鉴权要求？写入白名单还是需登录？需要 ADMIN 角色？
- 是否需要新的 Command/Query？

## 2. 步骤

### 步骤 1：定义 Request / Response

- `web/request/{Resource}{Action}Request.java`
- `web/response/{Resource}Response.java`
- 字段必填用 `@NotNull` / `@NotBlank`，长度 `@Size`
- 每个字段写 Javadoc（中文，业务语义）+ `@Schema(description=...)`

### 步骤 2：定义 application 层的 Command/Query/Result

- 与 web 层 DTO 分离
- 不带 Web 注解
- 不直接复用 Request 类

### 步骤 3：写 ApplicationService 方法

```java
@Transactional
public XxxResult doSomething(XxxCommand cmd) {
    // 校验
    // 取领域对象
    // 调用领域方法或服务
    // 持久化
    return XxxResult.of(...);
}
```

业务异常抛 `ApiException(ApiErrorCode.XXX)`，不自己 `try-catch` 返错。

### 步骤 4：Controller 暴露接口

```java
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @PostMapping
    public ApiResponse<CommentResponse> create(@Valid @RequestBody CommentCreateRequest req) {
        CommentCreateCommand cmd = mapper.toCommand(req);
        CommentCreateResult result = service.create(cmd);
        return ApiResponse.ok(mapper.toResponse(result));
    }
}
```

- URL kebab-case
- 字段 camelCase
- 方法上 `@Operation(summary=...)`

### 步骤 5：处理鉴权

- 需登录但任意角色 → 不在白名单
- 匿名可访问 → 在 `myblog.security.public-endpoints` 加 `method + path`
- 需 ADMIN → 在 Security 配置中标识或加注解

### 步骤 6：错误码

- 复用已有 `ApiErrorCode`，命名 `{MODULE}_{SUBJECT}_{REASON}`
- 新增错误码必须确定 HTTP 状态码（参考 `../rules/error-handling.md` §4）

### 步骤 7：写测试

- `XxxControllerTest`：成功 + 校验失败 + 权限失败 + 业务错误
- ApplicationService 集成测试（如涉及复杂规则）

### 步骤 8：跑 `mvn test`

## 3. Checklist

- [ ] Request `@Valid` + 字段校验
- [ ] Response 字段 Javadoc + `@Schema`
- [ ] Command/Result 与 Request/Response 分离
- [ ] ApplicationService 标 `@Transactional`（如有写）
- [ ] 业务异常抛 `ApiException`
- [ ] URL 前缀正确（前台 / 后台）
- [ ] 鉴权配置正确
- [ ] 错误码新增已加注释
- [ ] 测试覆盖成功+失败+鉴权
- [ ] Swagger 注解齐全
