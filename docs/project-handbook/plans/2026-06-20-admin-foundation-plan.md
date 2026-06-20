# 博客后台基础工程实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 Pure Admin Thin i18n 固定快照建立可运行的三语博客后台，并接通 V2 登录、会话刷新、退出、当前用户资料与 ADMIN/DEMO 静态权限。

**Architecture:** 将上游 `i18n` 分支提交 `2395bb11b51b45c3e9ae78ee23f88e3f3510b606` 作为无 Git 历史的固定快照导入 `frontend/apps/admin/`，先建立原始模板基线，再按小提交裁剪。后台保持独立 pnpm 工程；认证、HTTP、国际化与权限按特性拆分，静态路由不依赖后端菜单接口。

**Tech Stack:** Node 24、pnpm 9、Vue 3、TypeScript、Vite 7、Element Plus、Pinia、Vue Router、vue-i18n、Axios、Vitest、Spring Boot 3、JUnit 5、MockMvc

---

## 文件结构

新增或接管的核心文件职责如下：

```text
frontend/apps/admin/
├── UPSTREAM.md                         # 上游来源、版本和固定提交
├── vitest.config.ts                    # 单元测试运行环境
├── src/api/
│   ├── contract.ts                     # code/msg/data 通用响应类型
│   └── auth.ts                         # login/refresh/logout/me API
├── src/features/
│   ├── auth/
│   │   ├── model.ts                    # Token、CurrentUser、Role 类型
│   │   ├── session-storage.ts          # 会话持久化
│   │   └── session.ts                  # 登录、刷新、退出编排
│   ├── dashboard/index.vue             # 空仪表盘
│   └── i18n/locale.ts                  # 系统语言映射
├── src/router/
│   ├── guard.ts                        # 会话和角色守卫
│   └── modules/home.ts                 # 静态仪表盘路由
├── src/store/modules/user.ts           # 当前账号与只读状态
├── src/utils/http/
│   ├── error.ts                        # 业务码错误模型
│   └── index.ts                        # Axios、单飞刷新、一次重放
└── locales/{zh-CN,ja,en}.yaml          # 三语资源
```

## Task 1：修复当前用户 ID 的前端精度契约

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/CurrentUserVO.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserOpenApiTest.java`
- Modify: `docs/project-handbook/api-contract/auth.md`

- [ ] **Step 1: 先写超过 JavaScript 安全整数的失败测试**

在 `CurrentUserControllerTest` 将当前用户 ID 改为 `9007199254740993L`，断言 JSON 字符串：

```java
when(queryService.query("1001"))
        .thenReturn(new CurrentUserProfileResult(
                9007199254740993L,
                "admin",
                AccountType.ADMIN,
                profile));

mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("9007199254740993"));
```

在 `CurrentUserOpenApiTest` 增加：

```java
@Test
void documentsCurrentUserIdAsInt64String() throws Exception {
    String content = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode id = objectMapper.readTree(content)
            .at("/components/schemas/CurrentUserVO/properties/id");

    assertThat(id.path("type").asText()).isEqualTo("string");
    assertThat(id.path("format").asText()).isEqualTo("int64");
}
```

- [ ] **Step 2: 运行局部测试并确认失败**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=CurrentUserControllerTest,CurrentUserOpenApiTest test
```

Expected: `CurrentUserControllerTest` 或 OpenAPI schema 断言失败，因为当前 ID 仍是 JSON number。

- [ ] **Step 3: 最小化修改响应序列化**

在 `CurrentUserVO.id` 上添加：

```java
@Schema(type = "string", format = "int64",
        example = "9007199254740993")
@JsonSerialize(using = ToStringSerializer.class)
long id,
```

添加对应 import：

```java
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
```

同步把 `auth.md` 的 `data.id` 示例改为字符串，并明确 Java、数据库和路径内部仍使用原有 ID 类型。

- [ ] **Step 4: 运行测试确认通过**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=CurrentUserControllerTest,CurrentUserOpenApiTest test
```

Expected: 两个测试类全部通过。

- [ ] **Step 5: 检查范围并提交**

```powershell
git diff --stat
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/CurrentUserVO.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserControllerTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserOpenApiTest.java docs/project-handbook/api-contract/auth.md
git commit -m "修复后台账号ID前端精度契约"
```

## Task 2：导入 Pure Admin Thin i18n 固定快照

**Files:**
- Create: `frontend/apps/admin/**`
- Create: `frontend/apps/admin/UPSTREAM.md`
- Modify: `frontend/apps/admin/package.json`

- [ ] **Step 1: 导出固定快照**

使用临时目录导出，不复制 `.git`：

```powershell
$source = Join-Path $env:TEMP "pure-admin-thin-source"
$archive = Join-Path $env:TEMP "pure-admin-thin-i18n.zip"
if (Test-Path $source) { Remove-Item -LiteralPath $source -Recurse -Force }
if (Test-Path $archive) { Remove-Item -LiteralPath $archive -Force }
git clone --branch i18n --single-branch https://github.com/pure-admin/pure-admin-thin.git $source
git -C $source checkout 2395bb11b51b45c3e9ae78ee23f88e3f3510b606
git -C $source archive --format=zip --output=$archive 2395bb11b51b45c3e9ae78ee23f88e3f3510b606
New-Item -ItemType Directory -Force frontend/apps/admin | Out-Null
Expand-Archive -LiteralPath $archive -DestinationPath frontend/apps/admin
```

Expected: `frontend/apps/admin/package.json` 存在，目录内不存在 `.git`。

- [ ] **Step 2: 记录来源并调整包标识**

创建 `frontend/apps/admin/UPSTREAM.md`：

```markdown
# Upstream

- Project: pure-admin/pure-admin-thin
- Branch: i18n
- Version: 6.2.0
- Commit: 2395bb11b51b45c3e9ae78ee23f88e3f3510b606
- Imported: 2026-06-20
- License: MIT

This directory is a vendored snapshot maintained by MyBlog. Upstream history is
not embedded. Future upgrades must pin a new commit and be reviewed as a
separate change.
```

将 `package.json` 的 `name` 改为 `myblog-admin`，增加 `"packageManager": "pnpm@9.15.9"`，并保留 `private: true` 和 MIT LICENSE。

- [ ] **Step 3: 冻结安装并验证原始基线**

Run:

```powershell
pnpm --dir frontend/apps/admin install --frozen-lockfile
pnpm --dir frontend/apps/admin typecheck
pnpm --dir frontend/apps/admin build
```

Expected: 三条命令退出码均为 0；记录现有非阻断 warning，但不在导入提交中顺手清理。

- [ ] **Step 4: 检查机械导入范围并提交**

这是一次不可进一步拆分的上游快照导入，文件数量较多是预期结果。提交前必须确认只有 `frontend/apps/admin/`：

```powershell
git diff --stat
git status --short
git add frontend/apps/admin
git commit -m "引入Pure Admin Thin后台基线"
```

## Task 3：建立后台测试与质量基线

**Files:**
- Modify: `frontend/apps/admin/package.json`
- Modify: `frontend/apps/admin/pnpm-lock.yaml`
- Create: `frontend/apps/admin/vitest.config.ts`
- Create: `frontend/apps/admin/src/test/setup.ts`
- Create: `frontend/apps/admin/src/features/i18n/locale.test.ts`

- [ ] **Step 1: 添加测试依赖和命令**

Run:

```powershell
pnpm --dir frontend/apps/admin add -D vitest@3.2.4 happy-dom@18.0.1
```

在 `package.json` scripts 增加：

```json
"test": "vitest run",
"test:watch": "vitest"
```

- [ ] **Step 2: 创建测试配置**

创建 `vitest.config.ts`：

```ts
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) }
  },
  test: {
    environment: "happy-dom",
    setupFiles: ["./src/test/setup.ts"],
    clearMocks: true
  }
});
```

创建 `src/test/setup.ts`：

```ts
import { afterEach } from "vitest";

afterEach(() => {
  localStorage.clear();
  sessionStorage.clear();
});
```

- [ ] **Step 3: 写第一个失败测试**

创建 `src/features/i18n/locale.test.ts`：

```ts
import { describe, expect, it } from "vitest";
import { resolveAdminLocale } from "./locale";

describe("resolveAdminLocale", () => {
  it.each([
    ["zh-CN", "zh"],
    ["ja-JP", "ja"],
    ["en-US", "en"],
    ["fr-FR", "zh"]
  ])("maps %s to %s", (language, expected) => {
    expect(resolveAdminLocale(language)).toBe(expected);
  });
});
```

Run: `pnpm --dir frontend/apps/admin test`

Expected: FAIL，提示 `./locale` 不存在。

- [ ] **Step 4: 暂时创建最小 locale 实现使基线变绿**

创建 `src/features/i18n/locale.ts`：

```ts
export type AdminLocale = "zh" | "ja" | "en";

export function resolveAdminLocale(language: string): AdminLocale {
  const normalized = language.toLowerCase();
  if (normalized.startsWith("ja")) return "ja";
  if (normalized.startsWith("en")) return "en";
  return "zh";
}
```

Run:

```powershell
pnpm --dir frontend/apps/admin test
pnpm --dir frontend/apps/admin lint:eslint
pnpm --dir frontend/apps/admin typecheck
```

Expected: 测试、ESLint 和类型检查全部通过。

- [ ] **Step 5: 检查并提交**

```powershell
git diff --stat
git status --short
git add frontend/apps/admin/package.json frontend/apps/admin/pnpm-lock.yaml frontend/apps/admin/vitest.config.ts frontend/apps/admin/src/test frontend/apps/admin/src/features/i18n
git commit -m "建立后台测试与质量基线"
```

## Task 4：裁剪 Mock、动态路由和演示页面

**Files:**
- Delete: `frontend/apps/admin/mock/**`
- Delete: `frontend/apps/admin/src/api/routes.ts`
- Delete: `frontend/apps/admin/src/views/permission/**`
- Delete: `frontend/apps/admin/src/components/ReAuth/**`
- Delete: `frontend/apps/admin/src/components/RePerms/**`
- Delete: `frontend/apps/admin/src/directives/auth/**`
- Delete: `frontend/apps/admin/src/directives/perms/**`
- Modify: `frontend/apps/admin/build/plugins.ts`
- Modify: `frontend/apps/admin/src/router/index.ts`
- Modify: `frontend/apps/admin/src/router/utils.ts`
- Modify: `frontend/apps/admin/src/router/modules/home.ts`
- Modify: `frontend/apps/admin/src/directives/index.ts`

- [ ] **Step 1: 添加静态菜单回归测试**

创建 `src/router/static-router.test.ts`：

```ts
import { describe, expect, it } from "vitest";
import { constantMenus } from "./index";

describe("static admin routes", () => {
  it("contains dashboard without permission demo routes", () => {
    const text = JSON.stringify(constantMenus);
    expect(text).toContain("Dashboard");
    expect(text).not.toContain("PermissionPage");
    expect(text).not.toContain("PermissionButton");
  });
});
```

- [ ] **Step 2: 删除 Mock 与演示资源**

删除上面列出的目录和文件；从 `build/plugins.ts` 删除 `vite-plugin-fake-server` 注册，从 `src/directives/index.ts` 删除已移除指令的注册。

- [ ] **Step 3: 将路由初始化收口为静态路由**

在 `src/router/utils.ts` 中删除 `getAsyncRoutes`、`addAsyncRoutes` 及动态组件映射依赖，将 `initRouter` 收口为：

```ts
function initRouter(): Promise<Router> {
  usePermissionStoreHook().handleWholeMenus(constantMenus);
  return Promise.resolve(router);
}
```

从 `src/router/index.ts` 删除刷新页面时请求后端路由的分支，只在会话存在时调用同步静态菜单初始化。`home.ts` 只保留 `/dashboard`，路由名使用 `Dashboard`，标题使用 `menus.dashboard`。

- [ ] **Step 4: 运行裁剪验证**

Run:

```powershell
pnpm --dir frontend/apps/admin test
pnpm --dir frontend/apps/admin lint:eslint
pnpm --dir frontend/apps/admin typecheck
pnpm --dir frontend/apps/admin build
```

Expected: 全部通过；构建产物不再包含 `/get-async-routes`、`/login` Mock 实现或权限演示页面。

- [ ] **Step 5: 检查删除范围并提交**

```powershell
git diff --stat
git status --short
git add -A frontend/apps/admin
git commit -m "裁剪后台演示与动态路由能力"
```

## Task 5：完成中文、日文、英文界面基线

**Files:**
- Modify: `frontend/apps/admin/src/features/i18n/locale.ts`
- Modify: `frontend/apps/admin/src/features/i18n/locale.test.ts`
- Modify: `frontend/apps/admin/src/plugins/i18n.ts`
- Modify: `frontend/apps/admin/src/layout/hooks/useTranslationLang.ts`
- Modify: `frontend/apps/admin/src/views/login/index.vue`
- Create: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`

- [ ] **Step 1: 扩展语言持久化测试**

在 `locale.test.ts` 增加：

```ts
import { loadAdminLocale, saveAdminLocale } from "./locale";

it("persists an explicit locale", () => {
  saveAdminLocale("ja");
  expect(loadAdminLocale("en-US")).toBe("ja");
});

it("uses system language on first visit", () => {
  expect(loadAdminLocale("ja-JP")).toBe("ja");
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `pnpm --dir frontend/apps/admin test -- src/features/i18n/locale.test.ts`

Expected: FAIL，提示持久化函数不存在。

- [ ] **Step 3: 实现持久化并接入三语 locale**

在 `locale.ts` 增加：

```ts
const LOCALE_KEY = "myblog-admin-locale";

export function saveAdminLocale(locale: AdminLocale): void {
  localStorage.setItem(LOCALE_KEY, locale);
}

export function loadAdminLocale(systemLanguage: string): AdminLocale {
  const saved = localStorage.getItem(LOCALE_KEY);
  if (saved === "zh" || saved === "ja" || saved === "en") return saved;
  return resolveAdminLocale(systemLanguage);
}
```

在 `plugins/i18n.ts` 引入 Element Plus `ja` locale，把 `ja.yaml` 加入 `localesConfigs`，初始语言改为 `loadAdminLocale(navigator.language)`，fallback 改为 `zh`。登录页和导航语言菜单提供中文、日本語、English 三项，切换时调用 `saveAdminLocale`。

`ja.yaml` 必须完整覆盖保留页面实际使用的 `buttons`、`search`、`panel`、`menus`、`status`、`login` 键；中文和英文资源删除已裁剪权限演示键，并新增 `menus.dashboard`、`status.readOnlyDemo`、认证错误键。

- [ ] **Step 4: 运行三语验证并提交**

```powershell
pnpm --dir frontend/apps/admin test -- src/features/i18n/locale.test.ts
pnpm --dir frontend/apps/admin lint:eslint
pnpm --dir frontend/apps/admin typecheck
git diff --stat
git status --short
git add frontend/apps/admin/src/features/i18n frontend/apps/admin/src/plugins/i18n.ts frontend/apps/admin/src/layout/hooks/useTranslationLang.ts frontend/apps/admin/src/views/login/index.vue frontend/apps/admin/locales
git commit -m "建立后台三语界面基线"
```

## Task 6：建立 API 契约、错误模型与会话存储

**Files:**
- Create: `frontend/apps/admin/src/api/contract.ts`
- Create: `frontend/apps/admin/src/features/auth/model.ts`
- Create: `frontend/apps/admin/src/features/auth/session-storage.ts`
- Create: `frontend/apps/admin/src/features/auth/session-storage.test.ts`
- Create: `frontend/apps/admin/src/utils/http/error.ts`
- Create: `frontend/apps/admin/src/utils/http/error.test.ts`

- [ ] **Step 1: 写会话存储和错误映射失败测试**

`session-storage.test.ts` 覆盖保存、恢复、原子替换和清理：

```ts
import { describe, expect, it } from "vitest";
import { clearSession, loadSession, saveSession } from "./session-storage";

const session = {
  accessToken: "access",
  refreshToken: "refresh",
  accessExpiresAt: 10_000,
  refreshExpiresAt: 20_000
};

describe("session storage", () => {
  it("round-trips and clears a session", () => {
    saveSession(session);
    expect(loadSession()).toEqual(session);
    clearSession();
    expect(loadSession()).toBeNull();
  });
});
```

`error.test.ts` 断言 `10001`、`10002`、`10003`、`90001`、`90002`、`99999` 分别映射到稳定的错误 kind。

- [ ] **Step 2: 运行测试确认失败**

Run: `pnpm --dir frontend/apps/admin test -- src/features/auth/session-storage.test.ts src/utils/http/error.test.ts`

Expected: FAIL，两个实现文件不存在。

- [ ] **Step 3: 定义稳定类型**

`api/contract.ts`：

```ts
export interface ApiResponse<T> {
  code: string;
  msg: string;
  data: T;
}
```

`features/auth/model.ts`：

```ts
export type AdminRole = "ADMIN" | "DEMO";

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  accessExpiresIn: number;
  refreshExpiresIn: number;
}

export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  accessExpiresAt: number;
  refreshExpiresAt: number;
}

export interface UserProfile {
  nickname: string;
  avatarUrl: string | null;
  bioZh: string | null;
  bioJa: string | null;
  bioEn: string | null;
  location: string | null;
  website: string | null;
  emailPublic: string | null;
  githubUrl: string | null;
  twitterUrl: string | null;
  linkedinUrl: string | null;
  zhihuUrl: string | null;
  qiitaUrl: string | null;
  juejinUrl: string | null;
}

export interface CurrentUser {
  id: string;
  username: string;
  type: AdminRole;
  profile: UserProfile;
}
```

`session-storage.ts` 使用单个 key `myblog-admin-session` 保存完整 JSON；解析失败、字段缺失或 refresh 已过期时删除该 key 并返回 `null`。

`error.ts` 定义 `ApiErrorKind` 和 `ApiClientError`，业务码映射如下：`10001→badCredentials`、`10002→sessionExpired`、`10003→forbidden`、`90001→validation`、`90002→rateLimited`、`99999→server`，未知 HTTP/网络异常映射为 `network` 或 `unknown`。

- [ ] **Step 4: 运行测试并提交**

```powershell
pnpm --dir frontend/apps/admin test -- src/features/auth/session-storage.test.ts src/utils/http/error.test.ts
pnpm --dir frontend/apps/admin typecheck
git diff --stat
git status --short
git add frontend/apps/admin/src/api/contract.ts frontend/apps/admin/src/features/auth frontend/apps/admin/src/utils/http/error.ts frontend/apps/admin/src/utils/http/error.test.ts
git commit -m "建立后台接口与会话类型"
```

## Task 7：实现单飞刷新 HTTP 客户端

**Files:**
- Modify: `frontend/apps/admin/src/utils/http/index.ts`
- Create: `frontend/apps/admin/src/utils/http/index.test.ts`
- Create: `frontend/apps/admin/src/api/auth.ts`

- [ ] **Step 1: 写并发刷新与一次重放测试**

使用 Vitest mock Axios adapter，发出两个同时过期的受保护请求，断言：

```ts
expect(refreshCalls).toBe(1);
expect(protectedCalls).toBe(4); // 两次首次请求加两次重放
expect(first.data).toEqual({ value: 1 });
expect(second.data).toEqual({ value: 2 });
```

另写两个测试：refresh 返回 `10002` 时清理会话；带 `skipAuthRefresh: true` 的 refresh 请求不会递归重试。

- [ ] **Step 2: 运行测试确认旧模板实现失败**

Run: `pnpm --dir frontend/apps/admin test -- src/utils/http/index.test.ts`

Expected: FAIL，因为旧客户端使用模板 `/refresh-token` 契约，且没有 MyBlog 业务码和一次重放边界。

- [ ] **Step 3: 替换 HTTP 实现**

新实现必须满足以下接口：

```ts
export interface AuthRefreshCoordinator {
  getAccessToken(): string | null;
  refresh(): Promise<string>;
  expire(): void;
}

export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  timeout: 10_000
});
```

请求配置增加内部 `_retried?: boolean` 与 `skipAuthRefresh?: boolean`。受保护请求附加 Bearer token；`401 + 10002` 且 `_retried !== true` 时等待同一个 `refreshPromise`，成功后把 `_retried` 设为 true 并重放。refresh 失败调用 `expire()`，清空等待队列并拒绝所有原请求。响应成功但 `code !== "00000"` 时抛出 `ApiClientError`。

`api/auth.ts` 使用固定路径和明确请求类型：

```ts
export const login = (body: LoginRequest) =>
  http.post<ApiResponse<TokenPair>>("/api/auth/login", body, {
    skipAuthRefresh: true
  });

export const refresh = (refreshToken: string) =>
  http.post<ApiResponse<TokenPair>>(
    "/api/auth/refresh",
    { refreshToken },
    { skipAuthRefresh: true }
  );

export const logout = () =>
  http.post<ApiResponse<null>>("/api/auth/logout");

export const getCurrentUser = () =>
  http.get<ApiResponse<CurrentUser>>("/api/auth/me");
```

- [ ] **Step 4: 验证并提交**

```powershell
pnpm --dir frontend/apps/admin test -- src/utils/http/index.test.ts
pnpm --dir frontend/apps/admin lint:eslint
pnpm --dir frontend/apps/admin typecheck
git diff --stat
git status --short
git add frontend/apps/admin/src/utils/http frontend/apps/admin/src/api/auth.ts
git commit -m "实现后台单飞会话刷新"
```

## Task 8：接入登录、当前用户与退出编排

**Files:**
- Create: `frontend/apps/admin/src/features/auth/session.ts`
- Create: `frontend/apps/admin/src/features/auth/session.test.ts`
- Modify: `frontend/apps/admin/src/store/modules/user.ts`
- Modify: `frontend/apps/admin/src/store/types.ts`
- Modify: `frontend/apps/admin/src/views/login/index.vue`
- Modify: `frontend/apps/admin/src/layout/hooks/useNav.ts`
- Delete: `frontend/apps/admin/src/api/user.ts`
- Delete: `frontend/apps/admin/src/utils/auth.ts`

- [ ] **Step 1: 写登录原子性与退出失败测试**

`session.test.ts` 覆盖：登录成功后必须完成 `/me` 才保存完整用户态；`/me` 失败不得留下 token；服务端 logout 失败仍清理本地状态。

核心断言：

```ts
await expect(sessionService.signIn(credentials)).rejects.toBeDefined();
expect(loadSession()).toBeNull();
expect(userStore.currentUser).toBeNull();

await sessionService.signOut();
expect(loadSession()).toBeNull();
expect(userStore.currentUser).toBeNull();
```

- [ ] **Step 2: 运行测试确认失败**

Run: `pnpm --dir frontend/apps/admin test -- src/features/auth/session.test.ts`

Expected: FAIL，`session.ts` 不存在。

- [ ] **Step 3: 实现认证编排与 Pinia Store**

`userType` 改为：

```ts
export type userType = {
  currentUser: CurrentUser | null;
  initialized: boolean;
};
```

Store 提供 `SET_CURRENT_USER`、`CLEAR_USER`、`isAdmin`、`isDemo`。`session.ts` 提供 `signIn`、`restore`、`refreshAccessToken`、`signOut`；TokenPair 使用 `Date.now() + expiresIn * 1000` 转成绝对过期时间。`signIn` 先暂存 token，仅当 `/me` 成功后提交完整状态；失败路径清理 token 和 Store。

登录页删除演示默认账号、免登录天数和动态路由请求。提交成功后跳转 `/dashboard`；`10001`、`90002` 和网络错误使用三语资源显示。导航退出按钮调用服务端 logout，并在 finally 中执行本地清理和路由重置。

- [ ] **Step 4: 运行认证测试并提交**

```powershell
pnpm --dir frontend/apps/admin test -- src/features/auth/session.test.ts
pnpm --dir frontend/apps/admin lint:eslint
pnpm --dir frontend/apps/admin typecheck
git diff --stat
git status --short
git add -A frontend/apps/admin/src
git commit -m "接入后台登录与退出会话"
```

## Task 9：建立静态权限守卫与空仪表盘

**Files:**
- Create: `frontend/apps/admin/src/router/guard.ts`
- Create: `frontend/apps/admin/src/router/guard.test.ts`
- Modify: `frontend/apps/admin/src/router/index.ts`
- Modify: `frontend/apps/admin/src/router/modules/home.ts`
- Create: `frontend/apps/admin/src/features/dashboard/index.vue`
- Create: `frontend/apps/admin/src/features/dashboard/index.test.ts`
- Modify: `frontend/apps/admin/public/platform-config.json`

- [ ] **Step 1: 写守卫和只读状态失败测试**

守卫测试覆盖匿名访问跳转 `/login`、已登录访问 `/login` 跳转 `/dashboard`、未授权角色跳转 `/error/403`。仪表盘测试挂载 DEMO 用户并断言存在三语资源键对应的只读标识，不出现伪造统计数字。

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
pnpm --dir frontend/apps/admin test -- src/router/guard.test.ts src/features/dashboard/index.test.ts
```

Expected: FAIL，守卫和仪表盘组件不存在。

- [ ] **Step 3: 实现守卫和仪表盘**

守卫使用明确返回值：

```ts
export function resolveGuardTarget(
  toPath: string,
  role: AdminRole | null,
  allowedRoles?: AdminRole[]
): true | string {
  if (toPath === "/login") return role ? "/dashboard" : true;
  if (!role) return "/login";
  if (allowedRoles && !allowedRoles.includes(role)) return "/error/403";
  return true;
}
```

路由 meta 使用 `roles?: AdminRole[]`。仪表盘只展示当前昵称、用户名、角色、后端连接成功状态和 DEMO 只读横幅。`platform-config.json` 标题改为 `MyBlog Admin`，默认 vertical 布局、保留主题和多标签配置。

- [ ] **Step 4: 验证并提交**

```powershell
pnpm --dir frontend/apps/admin test -- src/router/guard.test.ts src/features/dashboard/index.test.ts
pnpm --dir frontend/apps/admin lint:eslint
pnpm --dir frontend/apps/admin typecheck
pnpm --dir frontend/apps/admin build
git diff --stat
git status --short
git add frontend/apps/admin/src/router frontend/apps/admin/src/features/dashboard frontend/apps/admin/public/platform-config.json
git commit -m "建立后台静态权限与仪表盘"
```

## Task 10：配置真实后端代理并完成阶段验收

**Files:**
- Modify: `frontend/apps/admin/vite.config.ts`
- Create: `frontend/apps/admin/.env.development`
- Create: `frontend/apps/admin/.env.production`
- Modify: `docs/project-handbook/frontend-admin/README.md`
- Modify: `docs/project-handbook/status.md`
- Modify: `docs/project-handbook/roadmap.md`

- [ ] **Step 1: 配置开发代理与生产地址**

`.env.development`：

```dotenv
VITE_API_BASE_URL=
VITE_BACKEND_PROXY_TARGET=http://localhost:8080
```

`.env.production`：

```dotenv
VITE_API_BASE_URL=/
```

`vite.config.ts` 将 `/api` 代理到 `VITE_BACKEND_PROXY_TARGET`，`changeOrigin: true`，不重写路径。不得重新启用 Mock。

- [ ] **Step 2: 运行前端完整验证**

```powershell
pnpm --dir frontend/apps/admin install --frozen-lockfile
pnpm --dir frontend/apps/admin lint
pnpm --dir frontend/apps/admin typecheck
pnpm --dir frontend/apps/admin test
pnpm --dir frontend/apps/admin build
```

Expected: 五条命令全部退出 0；记录测试数量和非阻断构建 warning。

- [ ] **Step 3: 运行后端局部与全量验证**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=AuthControllerTest,AuthLoginIntegrationTest,AuthSessionIntegrationTest,CurrentUserControllerTest test
mvn -f MyBlog-springboot-v2/pom.xml clean test
```

Expected: 局部认证测试通过；全量测试 0 failures、0 errors。Docker 不可用时只允许现有 Testcontainers 条件测试跳过，并记录数量。

- [ ] **Step 4: 使用真实后端完成浏览器验收**

依次验证：

1. ADMIN 登录后进入 `/dashboard`，刷新页面仍保持会话。
2. 中文、日本語、English 切换后刷新仍保持。
3. access token 过期时多个并发请求只触发一次 refresh。
4. refresh token 失效时清理会话并返回 `/login`。
5. DEMO 登录后显示只读横幅。
6. logout 即使服务端返回认证失效，本地仍清理完成。

若本机 `local` profile 缺少数据库或密钥环境变量，使用现有 H2 test profile 进行浏览器验收，并在文档中明确环境差异；不得提交绕过生产安全配置的代码。

- [ ] **Step 5: 更新文档并提交**

`frontend-admin/README.md` 记录技术栈、目录、启动命令、静态权限、认证流程、上游快照和后续业务模块边界；`status.md` 与 `roadmap.md` 将后台基础闭环标记完成，但文章等业务页保持未完成。

```powershell
git diff --stat
git status --short
git add frontend/apps/admin/vite.config.ts frontend/apps/admin/.env.development frontend/apps/admin/.env.production docs/project-handbook/frontend-admin/README.md docs/project-handbook/status.md docs/project-handbook/roadmap.md
git commit -m "记录后台基础工程验收结果"
```

## 最终检查

- [ ] `git status --short` 无未提交文件。
- [ ] `git log --oneline` 中每个提交只对应一个任务目的。
- [ ] Pure Admin Thin 上游许可证和 `UPSTREAM.md` 均存在。
- [ ] 后台工程不包含 `.git`、`node_modules`、`dist` 或 Mock 服务。
- [ ] 所有 API 逻辑只判断 `code`，不依赖后端中文 `msg`。
- [ ] token、密码和认证请求体不进入日志。
- [ ] 当前用户 ID 在 JSON 与 TypeScript 中均为字符串。
- [ ] ADMIN/DEMO 前端限制与后端权限矩阵一致。
