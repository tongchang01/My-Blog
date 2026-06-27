# Admin Comment Reply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让管理员可以在后台评论管理中回复已通过评论，并把回复作为正式评论数据写入后端。

**Architecture:** 后端新增后台回复服务与接口，复用现有评论领域模型、Markdown 渲染、文章评论数和通知事件。后台新增评论回复 API、状态管理和弹窗 UI，不引入树形后台列表。

**Tech Stack:** Spring Boot 3.5、JUnit/Mockito、Vue 3、Element Plus、Vitest、TypeScript、pnpm。

---

## File Structure

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentCommandService.java`
  - 增加 `reply` 命令，保持后台评论命令集中在一个服务内，避免新增小服务造成依赖分散。
- Add: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentReplyCommand.java`
  - 后台回复命令对象。
- Add: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentReplyResult.java`
  - 后台回复结果对象。
- Add: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentReplyRequest.java`
  - Controller 请求体。
- Add: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentReplyVO.java`
  - Controller 响应体。
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentController.java`
  - 新增 `POST /api/admin/comments/{id}/reply`。
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/AdminCommentCommandServiceTest.java`
  - 覆盖后台回复成功和非法目标。
- Modify Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/AdminCommentControllerTest.java`
  - 覆盖 Controller 委派。
- Modify: `frontend/apps/admin/src/api/comment.ts`
  - 新增 `replyComment`。
- Modify: `frontend/apps/admin/src/features/comments/model.ts`
  - 新增回复响应与表单状态类型。
- Modify: `frontend/apps/admin/src/features/comments/useCommentManagement.ts`
  - 新增回复弹窗状态与提交行为。
- Modify: `frontend/apps/admin/src/features/comments/index.vue`
  - 新增回复按钮、回复对象展示和弹窗。
- Test: `frontend/apps/admin/src/api/comment.test.ts`
- Test: `frontend/apps/admin/src/features/comments/useCommentManagement.test.ts`
- Test: `frontend/apps/admin/src/features/comments/index.test.ts`

---

### Task 1: Backend Reply Command

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentReplyCommand.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentReplyResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentCommandService.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/AdminCommentCommandServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Add tests that construct `AdminCommentCommandService` with mocked `CommentRepository`, `ArticleCommentCountService`, `CommentAuthorization`, `CommentMarkdownRenderer`, `ApplicationEventPublisher`, and `Clock`.

Required test cases:

```java
@Test
void adminCanReplyToPassedArticleComment() {
    // Given an ADMIN principal and an active PASS article comment.
    // When reply(principal, 10L, new AdminCommentReplyCommand("谢谢反馈")) is called.
    // Then repository.insert receives NewComment with:
    // target ARTICLE same targetId, parentId 10, replyToCommentId 10,
    // replyToNickname from target comment, authorNickname "站长",
    // auditStatus PASS, createdBy admin id.
    // And articleCommentCountService.increment(articleId, 1) is called.
}

@Test
void rejectsReplyToHiddenComment() {
    // Given active comment auditStatus HIDDEN.
    // Expect ApiException with CONFLICT.
    // Verify repository.insert is never called.
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=AdminCommentCommandServiceTest test
```

Expected: compilation fails because reply command/result and service method do not exist.

- [ ] **Step 3: Implement command/result and service method**

Add:

```java
public record AdminCommentReplyCommand(String contentMd) {
}
```

Add:

```java
public record AdminCommentReplyResult(long id, CommentAuditStatus auditStatus) {
}
```

Extend `AdminCommentCommandService` constructor dependencies with:

```java
private final CommentMarkdownRenderer markdownRenderer;
private final ApplicationEventPublisher eventPublisher;
```

Add `reply(...)`:

```java
@Transactional
public AdminCommentReplyResult reply(
        AuthenticatedPrincipal principal,
        long replyToCommentId,
        AdminCommentReplyCommand command) {
    long operatorId = authorization.requireAdmin(principal);
    Comment replyTo = requireActive(replyToCommentId);
    if (!replyTo.auditStatus().publiclyVisible()) {
        throw new ApiException(ApiErrorCode.CONFLICT, "不能回复该评论");
    }
    long parentId = replyTo.parentId() == null ? replyTo.id() : replyTo.parentId();
    Comment inserted = repository.insert(NewComment.create(
            replyTo.target(),
            parentId,
            replyTo.id(),
            replyTo.author().userId(),
            replyTo.author().nickname(),
            operatorId,
            "站长",
            null,
            null,
            null,
            null,
            command.contentMd(),
            markdownRenderer.render(command.contentMd()),
            CommentAuditStatus.PASS,
            LocalDateTime.now(clock),
            operatorId));
    if (inserted.target().targetType() == CommentTargetType.ARTICLE) {
        articleCommentCountService.increment(inserted.target().targetId(), 1);
    }
    eventPublisher.publishEvent(new CommentNotificationEvent(
            inserted.id(),
            replyTo.id(),
            inserted.author().nickname(),
            inserted.content().safeHtml(),
            inserted.auditStatus()));
    return new AdminCommentReplyResult(inserted.id(), inserted.auditStatus());
}
```

- [ ] **Step 4: Run backend service test**

Run:

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=AdminCommentCommandServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git status --short
git diff --stat
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/AdminCommentCommandServiceTest.java
git commit -m "接入后台评论回复命令"
```

---

### Task 2: Backend Reply API

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentReplyRequest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentReplyVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentController.java`
- Modify Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/AdminCommentControllerTest.java`

- [ ] **Step 1: Write failing controller test**

Add to `AdminCommentControllerTest`:

```java
@Test
void delegatesReplyCommand() throws Exception {
    when(commandService.reply(eq(principal), eq(10L), any()))
            .thenReturn(new AdminCommentReplyResult(9007199254740997L, CommentAuditStatus.PASS));

    mockMvc.perform(post("/api/admin/comments/10/reply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"contentMd\":\"谢谢反馈\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value("9007199254740997"))
            .andExpect(jsonPath("$.data.auditStatus").value("PASS"));

    verify(commandService).reply(eq(principal), eq(10L), any(AdminCommentReplyCommand.class));
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=AdminCommentControllerTest test
```

Expected: FAIL because endpoint is missing.

- [ ] **Step 3: Implement request/VO/controller**

Request:

```java
public record AdminCommentReplyRequest(@NotBlank String contentMd) {
}
```

VO:

```java
public record AdminCommentReplyVO(String id, CommentAuditStatus auditStatus) {
    public static AdminCommentReplyVO from(AdminCommentReplyResult result) {
        return new AdminCommentReplyVO(String.valueOf(result.id()), result.auditStatus());
    }
}
```

Controller method:

```java
@Operation(summary = "后台回复评论")
@PostMapping("/{id:\\d+}/reply")
public ApiResponse<AdminCommentReplyVO> reply(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id,
        @Valid @RequestBody AdminCommentReplyRequest request) {
    return ApiResponse.ok(AdminCommentReplyVO.from(
            commandService.reply(
                    principal,
                    id,
                    new AdminCommentReplyCommand(request.contentMd()))));
}
```

- [ ] **Step 4: Run controller test**

Run:

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=AdminCommentControllerTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git status --short
git diff --stat
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/AdminCommentControllerTest.java
git commit -m "开放后台评论回复接口"
```

---

### Task 3: Admin API and State

**Files:**
- Modify: `frontend/apps/admin/src/api/comment.ts`
- Modify: `frontend/apps/admin/src/api/comment.test.ts`
- Modify: `frontend/apps/admin/src/features/comments/model.ts`
- Modify: `frontend/apps/admin/src/features/comments/useCommentManagement.ts`
- Modify: `frontend/apps/admin/src/features/comments/useCommentManagement.test.ts`

- [ ] **Step 1: Write failing frontend tests**

Add API test asserting:

```ts
await replyComment("10", "谢谢反馈");
expect(mock.history.post[0].url).toBe("/api/admin/comments/10/reply");
expect(JSON.parse(mock.history.post[0].data)).toEqual({ contentMd: "谢谢反馈" });
```

Add composable tests for:

- `openReplyDialog(item)` sets `replyTarget`, clears `replyContent`, and opens dialog.
- `submitReply()` trims content, calls API, refreshes list, closes dialog.
- empty content returns false and does not call API.
- failed submit keeps dialog open and sets `operationError`.

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
cd frontend/apps/admin
pnpm test -- src/api/comment.test.ts src/features/comments/useCommentManagement.test.ts
```

Expected: FAIL because `replyComment` and reply state are missing.

- [ ] **Step 3: Implement API/model/composable**

API:

```ts
export const replyComment = (id: string, contentMd: string) =>
  http.post<ApiResponse<CommentReplyResponse>>(
    `/api/admin/comments/${id}/reply`,
    { contentMd }
  );
```

Model:

```ts
export interface CommentReplyResponse {
  id: string;
  auditStatus: CommentAuditStatus;
}
```

Composable state/actions:

```ts
const replyDialogVisible = ref(false);
const replyTarget = ref<CommentListItem | null>(null);
const replyContent = ref("");
const replySubmitting = ref(false);

function openReplyDialog(item: CommentListItem): void {
  operationError.value = null;
  replyTarget.value = item;
  replyContent.value = "";
  replyDialogVisible.value = true;
}

function closeReplyDialog(): void {
  if (replySubmitting.value) return;
  replyDialogVisible.value = false;
  replyTarget.value = null;
  replyContent.value = "";
}

async function submitReply(): Promise<boolean> {
  if (!replyTarget.value || !replyContent.value.trim()) return false;
  operationError.value = null;
  replySubmitting.value = true;
  try {
    await api.replyComment(replyTarget.value.id, replyContent.value.trim());
    replyDialogVisible.value = false;
    replyTarget.value = null;
    replyContent.value = "";
    await refreshAfterOperation();
    return true;
  } catch (reason) {
    operationError.value = asError(reason);
    return false;
  } finally {
    replySubmitting.value = false;
  }
}
```

- [ ] **Step 4: Run frontend unit tests**

Run:

```powershell
cd frontend/apps/admin
pnpm test -- src/api/comment.test.ts src/features/comments/useCommentManagement.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git status --short
git diff --stat
git add frontend/apps/admin/src/api/comment.ts frontend/apps/admin/src/api/comment.test.ts frontend/apps/admin/src/features/comments/model.ts frontend/apps/admin/src/features/comments/useCommentManagement.ts frontend/apps/admin/src/features/comments/useCommentManagement.test.ts
git commit -m "接入后台评论回复状态"
```

---

### Task 4: Admin Reply UI

**Files:**
- Modify: `frontend/apps/admin/src/features/comments/index.vue`
- Modify: `frontend/apps/admin/src/features/comments/index.test.ts`

- [ ] **Step 1: Write failing UI tests**

Add tests:

- PASS comment row renders `comment-reply` button for admin.
- Clicking reply opens dialog and displays target nickname.
- Submitting content calls composable submit path and closes dialog on success.
- Rows with `replyToNickname` render `回复 @昵称`.

- [ ] **Step 2: Run UI tests and verify failure**

Run:

```powershell
cd frontend/apps/admin
pnpm test -- src/features/comments/index.test.ts
```

Expected: FAIL because UI has no reply button/dialog.

- [ ] **Step 3: Implement UI**

Add destructured composable state/actions:

```ts
replyDialogVisible,
replyTarget,
replyContent,
replySubmitting,
openReplyDialog,
closeReplyDialog,
submitReply
```

Add capability helper:

```ts
function canReply(item: CommentListItem): boolean {
  return isAdmin.value && !item.deleted && item.auditStatus === "PASS";
}
```

Add row metadata:

```vue
<span v-if="row.replyToNickname" class="reply-meta">
  {{ transformI18n("comments.reply.replyTo") }} @{{ row.replyToNickname }}
</span>
```

Add operation button:

```vue
<el-button
  v-if="canReply(row)"
  data-testid="comment-reply"
  link
  type="primary"
  @click="openReplyDialog(row)"
>
  {{ transformI18n("comments.actions.reply") }}
</el-button>
```

Add dialog:

```vue
<el-dialog
  v-model="replyDialogVisible"
  data-testid="comment-reply-dialog"
  :title="transformI18n('comments.reply.title')"
  width="520px"
  :before-close="closeReplyDialog"
>
  <p v-if="replyTarget" class="reply-target">
    {{ transformI18n("comments.reply.target") }}：{{ replyTarget.authorNickname }}
  </p>
  <el-input
    v-model="replyContent"
    data-testid="comment-reply-content"
    type="textarea"
    :rows="5"
    :placeholder="transformI18n('comments.reply.placeholder')"
  />
  <template #footer>
    <el-button @click="closeReplyDialog">
      {{ transformI18n("articles.actions.cancel") }}
    </el-button>
    <el-button
      data-testid="comment-reply-submit"
      type="primary"
      :loading="replySubmitting"
      :disabled="!replyContent.trim()"
      @click="submitReply"
    >
      {{ transformI18n("comments.reply.submit") }}
    </el-button>
  </template>
</el-dialog>
```

Add i18n keys in the existing locale files used by comments.

- [ ] **Step 4: Run UI tests**

Run:

```powershell
cd frontend/apps/admin
pnpm test -- src/features/comments/index.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git status --short
git diff --stat
git add frontend/apps/admin/src/features/comments/index.vue frontend/apps/admin/src/features/comments/index.test.ts frontend/apps/admin/src
git commit -m "实现后台评论回复弹窗"
```

---

### Task 5: Stage Verification

**Files:**
- Modify or create: `docs/superpowers/reviews/2026-06-27-admin-comment-reply-verification.md`

- [ ] **Step 1: Run backend targeted tests**

Run:

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=AdminCommentCommandServiceTest,AdminCommentControllerTest test
```

Expected: PASS.

- [ ] **Step 2: Run admin targeted tests**

Run:

```powershell
cd frontend/apps/admin
pnpm test -- src/api/comment.test.ts src/features/comments/useCommentManagement.test.ts src/features/comments/index.test.ts
pnpm run typecheck
```

Expected: PASS.

- [ ] **Step 3: Run broader backend/frontend checks if targeted tests pass**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test

cd frontend/apps/admin
pnpm test
pnpm run build
```

Expected: PASS.

- [ ] **Step 4: Record verification**

Create `docs/superpowers/reviews/2026-06-27-admin-comment-reply-verification.md` with command results and any warnings.

- [ ] **Step 5: Commit**

```powershell
git status --short
git diff --stat
git add docs/superpowers/reviews/2026-06-27-admin-comment-reply-verification.md
git commit -m "记录后台评论回复验证结果"
```

---

## Self-Review

- Spec coverage: backend command, backend API, admin API/state, admin UI, validation and verification are each mapped to a task.
- Placeholder scan: no `TBD` or unbounded task remains.
- Type consistency: API response uses `CommentReplyResponse`; backend result uses `AdminCommentReplyResult`; request field is consistently `contentMd`.
