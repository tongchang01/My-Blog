# MyBlog V2 博客端

公开读者端，使用 Vue 3、Pinia、Vue Router、vue-i18n 和 Vite，通过 V2 `/api/public/**` 接口读取数据。

## 本地运行

```powershell
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认访问 `http://localhost:5173`，`/api` 代理到 `http://localhost:8080`。可通过 `VITE_API_PROXY_TARGET` 覆盖代理目标。

## 验证

```powershell
corepack pnpm test
corepack pnpm typecheck
corepack pnpm lint
corepack pnpm build
```

功能范围见 [`docs/handbook/frontend/blog/README.md`](../../../docs/handbook/frontend/blog/README.md)，接口契约见 [`docs/handbook/api/`](../../../docs/handbook/api/README.md)。界面最初源自 [Hexo Theme Aurora](https://github.com/auroral-ui/hexo-theme-aurora)，当前应用不再作为 Hexo 主题运行。
