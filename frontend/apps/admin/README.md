# MyBlog V2 管理端

后台管理应用，使用 Vue 3、Pinia、Element Plus、ECharts 和 Vite，通过 V2 认证与 `/api/admin/**` 接口管理博客内容。

## 本地运行

```powershell
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认访问 `http://localhost:8848`，开发代理目标为 `http://localhost:8080`。

## 验证

```powershell
corepack pnpm test
corepack pnpm typecheck
corepack pnpm lint
corepack pnpm build
```

功能与权限边界见 [`docs/handbook/frontend/admin/README.md`](../../../docs/handbook/frontend/admin/README.md)，接口契约见 [`docs/handbook/api/`](../../../docs/handbook/api/README.md)。上游框架来源和许可证记录见 [`UPSTREAM.md`](UPSTREAM.md)。
