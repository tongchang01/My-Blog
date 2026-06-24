# MyBlog Admin

MyBlog V2 后台管理端位于 `frontend/apps/admin/`。当前已完成可运行的后台基础闭环，以及文章、分类、标签、评论和友链管理闭环。

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
src/features/articles/   文章列表、编辑表单、请求状态和写入页面
src/features/friend-links/ 友链列表、表单、排序和状态管理
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

## 文章管理验收（2026-06-21）

- ADMIN 与 DEMO 均可进入 `/articles/list`；只有 ADMIN 可看到并进入新建、编辑入口，DEMO 保持只读。
- 独立筛选卡片当前支持标题关键词和状态，保留分类、标签、日期的后续扩展位置。
- 文章、分类和标签初始并行加载；主列表采用 latest-request-wins，字典失败不阻断文章展示。
- 支持查询、重置、刷新、分页、筛选收起/展开、loading、empty、error 和 retry。
- `/articles/new` 与 `/articles/:id/edit` 支持三语标题/摘要、Markdown 正文、slug、状态、分类、标签、定时发布时间和密码访问设置。
- 所有非草稿状态在前端即时校验分类；定时文章校验发布时间；新建密码文章校验访问密码，编辑时留空则保留原密码。
- 新建文章不设置封面附件；编辑文章会保留后端已有的封面附件 ID，本阶段不提供封面选择或上传。
- 后台文章、分类、标签的 Snowflake ID 在 Web 响应和 OpenAPI 中统一为 `string/int64`，Java 内部仍使用 `long`。
- 使用本地 `myblog_v2_dev` 完成真实浏览器新增、编辑、发布和列表回显联调；联调文章随后已彻底清理，数据库恢复为原有 5 篇活动文章。
- 前端完整验证：52 tests、typecheck、生产构建通过。
- 后端完整验证：637 tests，0 failures，0 errors，4 skipped。
- 视觉验收：`../design-qa.md` 最终结果为 `passed`；同视口对比证据位于 `article-list-qa-comparison-final.png`。

## 分类标签管理验收（2026-06-21）

- `/categories/list` 与 `/tags/list` 作为内容管理菜单的独立页面，支持三语名称和 slug 的本地筛选。
- ADMIN 可新增、编辑和二次确认删除；DEMO 不渲染写控件。
- 分类支持表格内调整排序值，仅批量提交已修改项；标签明确不提供排序能力。
- slug 冲突与活动文章引用保护按 `90004` 和当前操作映射三语提示，失败时保留页面与表单状态。
- 真实 MySQL 浏览器联调覆盖分类新增、编辑、排序、删除、引用冲突，以及标签新增、编辑、删除、引用冲突。
- 联调数据已彻底清理；数据库保留 3 个活动分类、3 个活动标签和 5 篇活动文章。
- 前端完整验证：22 个测试文件、71 tests、typecheck、生产构建通过。
- 构建仅保留既有 Browserslist 数据过期提示，不阻断产物生成。

## 文章生命周期管理（2026-06-22）

- ADMIN 可在文章列表二次确认后软删除单篇文章；删除后刷新当前页，尾页清空时自动回退一页。
- `/articles/recycle-bin` 是内容管理下的独立可见页面，ADMIN 与 DEMO 均可读取，只有 ADMIN 渲染恢复操作。
- 恢复操作会提示原状态继续生效：原状态为 `PUBLISHED` 的文章恢复后可能立即重新公开。
- 后端继续校验分类、标签和封面引用；引用失效时前端按 `90004` 显示专用冲突提示。
- 当前不提供永久删除、批量删除、批量恢复或回收站关键词筛选。

## 友链管理（2026-06-24）

- `/friend-links/list` 作为内容管理下的独立页面，ADMIN 和 DEMO 均可访问。
- 页面采用独立筛选卡片，支持关键词和显示状态筛选；列表展示名称、URL、描述、状态、排序值和更新时间。
- ADMIN 可新增、编辑、显示/隐藏、保存排序和删除；DEMO 只读，不渲染写操作入口。
- 后端友链响应中的 `id`、`createdBy`、`updatedBy` 使用字符串契约，避免浏览器端 Snowflake ID 精度损失。
- 当前不提供拖拽排序、批量删除、图片上传选择器或友链可用性检测。

## 后续边界

下一阶段可继续完善留言、附件、站点配置、真实统计仪表盘，以及 Markdown 编辑器与预览、自动保存和离开页面确认。
