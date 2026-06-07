# 请求处理链路

> 本文档回答："一个请求从前端到 DB 是怎么走的？谁负责什么？"
> 适用范围：V2 所有受保护接口。

## 1. 整体链路

```
[前端]
   │
   ▼  HTTP + JSON
[Spring DispatcherServlet]
   │
   ▼
[Filter 链]
   ├─ CorsFilter
   ├─ JwtAuthenticationFilter   ← 解析 Bearer，注入 SecurityContext
   └─ FilterSecurityInterceptor ← 鉴权
   │
   ▼
[Controller (web 层)]
   ├─ 入参绑定（@Valid 校验）
   ├─ 调用 ApplicationService
   └─ 返回 ApiResponse<T>
   │
   ▼
[ApplicationService (application 层)]
   ├─ 用例编排（事务边界）
   ├─ 调 Domain 服务做业务规则
   └─ 调 Repository 接口（domain.repository）持久化
   │
   ▼
[Domain (domain 层)]
   ├─ Entity / 值对象
   ├─ Domain Service
   └─ Repository 接口
   │
   ▼
[Repository 实现 (infrastructure.persistence)]
   ├─ 调 MyBatis-Plus Mapper
   └─ Entity ↔ PO 映射
   │
   ▼
[MySQL / H2]
```

## 2. 异常处理链路

任意层抛 `ApiException` →
```
   ▼
[GlobalExceptionHandler]  (@RestControllerAdvice)
   ├─ ApiException        → ApiErrorCode 对应 HTTP 状态 + 中文消息
   ├─ 校验异常             → VALIDATION_ERROR（400）
   ├─ Spring Security 异常 → 401/403
   └─ 其它未捕获           → INTERNAL_ERROR（500），不暴露堆栈
   │
   ▼
ApiResponse {code, message, data:null}
```

详见 `../rules/error-handling.md`。

## 3. 一个完整例子：发评论

`POST /api/comments`，Body：`{ articleId, content, parentId? }`

### Filter 阶段
- `JwtAuthenticationFilter`：解析 Bearer token，注入 `Authentication`
- `FilterSecurityInterceptor`：白名单匹配 `POST /api/comments` 需登录

### web 层：`CommentController`
```java
@PostMapping
public ApiResponse<CommentResponse> create(@Valid @RequestBody CommentCreateRequest req) {
    CommentCreateCommand cmd = CommentRequestMapper.toCommand(req, currentUserId());
    CommentCreateResult result = commentApplicationService.create(cmd);
    return ApiResponse.ok(CommentResponseMapper.toResponse(result));
}
```

### application 层：`CommentApplicationService.create()`
```java
@Transactional
public CommentCreateResult create(CommentCreateCommand cmd) {
    // 1. 校验文章存在且允许评论（跨模块通过 content.application）
    contentQueryService.assertArticleCommentable(cmd.articleId());

    // 2. 构建领域对象
    Comment comment = Comment.draft(cmd);

    // 3. 应用领域规则
    commentReviewPolicy.applyOnCreate(comment);

    // 4. 持久化（走 Repository 接口）
    Comment saved = commentRepository.save(comment);

    return CommentCreateResult.of(saved);
}
```

### domain 层
- `Comment` Entity：业务字段、状态机方法
- `CommentReviewPolicy`：审核策略
- `CommentRepository`：接口

### infrastructure 层：`CommentRepositoryImpl`
```java
public Comment save(Comment comment) {
    CommentPO po = CommentPersistenceMapper.toPo(comment);
    if (po.getId() == null) commentMapper.insert(po);
    else commentMapper.updateById(po);
    return CommentPersistenceMapper.toDomain(po);
}
```

→ MyBatis-Plus → SQL → MySQL

## 4. 各层职责速查

| 层 | 职责 | 禁止 |
|----|------|------|
| web | HTTP 入口、参数校验、调 application、组装 ApiResponse | 直接调 Mapper / Repository 实现 |
| application | 用例编排、事务边界、跨模块协调 | 包含业务规则（应在 domain）|
| domain | 业务规则、领域模型、仓储接口 | 依赖 web / infrastructure |
| infrastructure | 仓储实现、Mapper、外部适配 | 反向依赖业务模块 |

## 5. 事务边界

- 事务在 `application` 层用 `@Transactional` 标注
- 不在 `Controller` 加事务
- 不在 `Repository` 实现内加事务（除非有独立子事务需求并写注释）

## 6. 跨模块调用

业务模块 A 调业务模块 B：**只**能通过 B 的 `application` 层公开接口。

🔴 不能：
- 调 B 的 `infrastructure.persistence.*`
- 直接 import B 的 `domain.*` 内部实体（如需共享只读视图，B 的 application 层提供 DTO）

## 7. 测试切片对应

| 层 | 典型测试 |
|----|----------|
| web | `@WebMvcTest` + MockMvc |
| application | 单元 + Mock Repository，或 `@SpringBootTest` 集成 |
| domain | 纯 JUnit，无 Spring |
| infrastructure | `@SpringBootTest` + H2 |

详见 `../rules/testing-policy.md`。

## 8. 服务端消息语言

- `LocaleResolver` 读取请求 `Accept-Language`，支持 `zh-CN`、`ja`、`en`
- 缺少请求头或请求语言不受支持时回退 `zh-CN`
- `MessageSource` 只负责错误、校验和邮件等服务端消息
- 文章、分类、标签等业务内容语言仍由 `/{lang}/` 接口路径决定，不依赖全局 Locale
- 前端按钮、菜单等 UI 文案由 `vue-i18n` 管理，不放入后端消息资源
