# V2 主线候选验证记录

> 分支：`integration/v2-main-prep`  
> 日期：2026-06-27  
> 用途：记录从 `frontend-v2-clean` 与 `backend-v2-integration-ready` 整合出的 V2 主线候选验证结果。

## 1. 后端

路径：

```text
MyBlog-springboot-v2
```

命令：

```powershell
mvn test
```

结果：

```text
BUILD SUCCESS
Tests run: 641, Failures: 0, Errors: 0, Skipped: 4
Total time: 01:05 min
Finished at: 2026-06-27T10:52:51+09:00
```

## 2. 博客前台

路径：

```text
frontend/apps/blog
```

命令：

```powershell
$env:CI='true'; corepack pnpm install --frozen-lockfile
corepack pnpm run build
```

结果：

```text
pnpm install: passed
vite build: passed
modules transformed: 366
```

备注：

- 构建期间存在 Sass legacy JS API、Sass `@import` deprecation warning。
- 构建期间存在单个 chunk 超过 500 kB 的 Vite warning。
- 以上均未阻断构建。

## 3. 管理后台

路径：

```text
frontend/apps/admin
```

命令：

```powershell
$env:CI='true'; corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm run typecheck
corepack pnpm run build
```

结果：

```text
pnpm install: passed
vitest: 47 test files passed, 168 tests passed
typecheck: passed
vite build: passed
```

备注：

- 构建期间存在 `baseline-browser-mapping` 和 `Browserslist/caniuse-lite` 数据过期提示。
- 以上提示未阻断测试、类型检查或构建。

## 4. 结论

`integration/v2-main-prep` 已完成后端、博客前台、管理后台的基础验证，可以作为创建 `main` 的候选来源。
