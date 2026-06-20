# MyBlog Admin

MyBlog V2 后台管理端位于 `frontend/apps/admin/`。当前阶段完成可运行的后台基础闭环，业务管理页面尚未开始实现。

## 技术基线

- Node.js 24、pnpm 9.15.9
- Vue 3、TypeScript、Vite 7
- Pure Admin Thin i18n 6.2.0、Element Plus、Pinia、Vue Router、vue-i18n
- Axios、Vitest、Vue Test Utils、happy-dom
- 中文、日文、英文三语界面

上游来源和固定提交见 `../UPSTREAM.md`。该目录是无上游 Git 历史的固定快照，后续升级必须重新固定 commit 并单独评审。

## 目录

```text
src/api/                 API 契约和认证请求
src/features/auth/       会话模型、存储和认证编排
src/features/dashboard/  当前空仪表盘
src/features/i18n/       系统语言映射和语言持久化
src/router/              静态路由和权限守卫
src/store/               Pinia 状态
src/utils/http/          Axios、错误模型、单飞刷新和一次重放
locales/                 zh / ja / en 资源
docs/                    后台设计、计划和工程说明
```

## 本地启动

后端默认监听 `http://localhost:8080`，开发服务器将 `/api` 原路径代理到后端。

```powershell
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认后台地址为 `http://localhost:8848/`。生产构建使用同源 `/api`：

```powershell
corepack pnpm typecheck
corepack pnpm test
corepack pnpm build
```

## 认证与权限

- 登录顺序为 `/api/auth/login` → `/api/auth/me`；只有两步都成功才保留完整会话。
- token 只存储在 `localStorage` 的后台专用会话键中，不使用旧 Cookie 会话。
- access token 失效时，并发请求共享一次 `/api/auth/refresh`，成功后各自只重放一次。
- refresh 失败会原子清理 token 和当前用户。
- logout 调用服务端全端退出；即使接口失败，本地会话也会在 `finally` 中清理。
- 路由由前端静态维护。ADMIN 可进入管理写操作页；DEMO 只读。后端授权仍是最终安全边界。
- 当前仪表盘只显示真实当前用户和连接状态，不展示伪造统计数字。

## 阶段验收（2026-06-20）

- 前端：冻结安装成功；ESLint、Prettier、Stylelint 通过；28 tests 通过；typecheck 和生产构建通过。
- 后端认证局部测试：27 tests，0 failures，0 errors。
- 后端全量：637 tests，0 failures，0 errors，4 skipped；跳过项是本机无 Docker 时的既有 Testcontainers 条件测试。
- H2 test profile 真实 HTTP 链路：ADMIN/DEMO 登录与 `/me`、refresh token 轮换、logout、旧 access token 撤销以及 Vite `/api` 代理均通过。
- 应用内浏览器控制在本次运行环境中不可用，因此可视化点击、刷新和三语切换的浏览器验收仍需本机复验；相关状态与并发行为已有自动化测试覆盖。
- 构建存在上游 `baseline-browser-mapping` 与 Browserslist 数据过期提示，不阻断构建。

## 后续边界

下一阶段再引入文章、分类/标签、评论审核、留言、友链、附件、站点配置和真实统计仪表盘。Vditor、写操作二次确认、DEMO 按钮禁用和各业务错误码应随对应模块实现，不在基础工程中预造空壳。
