# DEMO 敏感字段裁剪收口实施计划

> 状态：已完成
> 适用范围：O-002 DEMO 敏感字段裁剪边界
> 分支：`feature/demo-sensitive-field-trimming`
> 目标：确认并收口 DEMO 后台只读安全边界，关闭 O-002。

## 现状判断

O-002 的核心后端逻辑已经基本存在：

- `ArticleQueryService.adminDetail(...)`：ADMIN 可读所有正文；DEMO 只可读 `PUBLISHED` 正文，其他状态 `body=null`。
- `AdminCommentQueryService.page(...)`：ADMIN 返回 `authorEmail/authorIp/authorUserAgent`；DEMO 固定返回 `null`。
- `AttachmentVO`：只输出公开元数据，不输出 `storageType/bucket/objectKey/hashSha256`。
- `SecurityConfigTest` 和各 command service 测试已经覆盖多处 DEMO 写操作 `403 + 10003`。

所以本批不做大重构，不抽公共权限框架，只补缺口测试、API 文档和状态收口。

## 任务拆分

### Task 1：补 Web 契约测试

**文件：**

- 修改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminArticleControllerTest.java`
- 修改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/AdminCommentControllerTest.java`
- 已覆盖不改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/web/AdminAttachmentControllerTest.java`

**步骤：**

- [x] 在 `AdminArticleControllerTest` 增加测试：当应用层返回 `body=null` 时，后台文章详情响应不存在非空正文，且仍不暴露 `password/accessPassword`。

关键断言：

```java
mockMvc.perform(get("/api/admin/articles/100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.body").doesNotExist())
        .andExpect(jsonPath("$.data.password").doesNotExist())
        .andExpect(jsonPath("$.data.accessPassword").doesNotExist());
```

- [x] 在 `AdminCommentControllerTest` 增加测试：后台评论列表不输出非空审计字段。

关键断言：

```java
mockMvc.perform(get("/api/admin/comments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].authorEmail").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].authorIp").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].authorUserAgent").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].contentMd").value("hello"));
```

- [x] 运行定向 Web 测试，确认失败或通过情况。

命令：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=AdminArticleControllerTest,AdminCommentControllerTest,AdminAttachmentControllerTest" test
```

期望：`BUILD SUCCESS`。如果新增测试直接通过，说明裁剪逻辑已有，只保留测试作为契约锁定。

### Task 2：跑应用层现有裁剪测试

**文件：**

- 不改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/AdminArticleQueryServiceTest.java`
- 不改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/AdminCommentQueryServiceTest.java`

**步骤：**

- [x] 运行 O-002 核心应用层测试。

命令：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=AdminArticleQueryServiceTest,AdminCommentQueryServiceTest" test
```

期望：`BUILD SUCCESS`，覆盖：

- DEMO 读取 `PUBLISHED` 正文。
- DEMO 读取 `DRAFT/PRIVATE/PASSWORD/SCHEDULED` 正文为 `null`。
- ADMIN 可读评论审计字段。
- DEMO 评论审计字段为 `null`。

### Task 3：更新 API 文档和状态

**文件：**

- 修改：`docs/handbook/api/article.md`
- 修改：`docs/handbook/api/comment.md`
- 修改：`docs/handbook/api/attachment.md`
- 修改：`docs/handbook/start-here/open-issues.md`
- 修改：`docs/handbook/start-here/current-status.md`
- 修改：`docs/handbook/frontend/admin/integration-status.md`

**步骤：**

- [x] 在文章 API 文档注明后台详情 DEMO 裁剪规则：
  - `PUBLISHED`：`body` 正常返回。
  - `DRAFT/PRIVATE/PASSWORD/SCHEDULED`：`body=null`。
  - `password/accessPassword/passwordHash` 不返回。
- [x] 在评论 API 文档注明后台评论 DEMO 裁剪规则：
  - `authorEmail=null`
  - `authorIp=null`
  - `authorUserAgent=null`
  - 内容、审核状态、目标信息仍返回。
- [x] 在附件 API 文档确认响应只包含公开管理元数据，不包含内部存储字段。
- [x] 将 `open-issues.md` 的 O-002 改为已关闭，写明关闭原因和验证命令。
- [x] 将 `current-status.md` / admin integration status 中的 DEMO 裁剪待办改为已完成。

### Task 4：最终验证并提交

**步骤：**

- [x] 检查变更范围。

命令：

```powershell
git diff --stat
git status --short
```

期望：只包含 Task 1 和 Task 3 的测试/文档变更。

- [x] 运行后端定向回归。

命令：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=AdminArticleQueryServiceTest,AdminCommentQueryServiceTest,AdminArticleControllerTest,AdminCommentControllerTest,AdminAttachmentControllerTest" test
```

期望：`BUILD SUCCESS`。

- [x] 提交。

命令：

```powershell
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminArticleControllerTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/AdminCommentControllerTest.java docs/handbook/api docs/handbook/start-here docs/handbook/frontend/admin
git commit -m "测试：收口DEMO敏感字段裁剪"
```

- [x] 推送并跑 CI。

命令：

```powershell
git push -u origin feature/demo-sensitive-field-trimming
gh workflow run CI --ref feature/demo-sensitive-field-trimming
```

## 不做

- 不重构 `ContentAuthorization` / `CommentAuthorization`。
- 不把角色字符串抽枚举。
- 不修改后台页面，只要现有页面能显示 `null` 字段。
- 不处理 review 文档里的重复代码、风格或事务整改。
