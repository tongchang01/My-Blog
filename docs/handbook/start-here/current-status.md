# 当前状态

> 状态：当前有效
> 适用范围：MyBlog V2 开发与发布准备
> 最后校准：2026-07-21
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：当前进度权威源

## 总结

V2 的后端、公开博客主阅读链路和管理后台主要业务闭环已经实现，并已运行在生产环境。当前主线是稳定线上运行、修复体验问题和补齐明确的产品缺口，而不是重建已有模块。

| 范围 | 当前状态 |
| --- | --- |
| 后端 | 五个业务模块与 common 已实现；Flyway V1–V6、16 张表；公开与后台 API 已覆盖 |
| 博客端 | 首页、文章、分类、标签、归档、搜索、关于、友链、留言板、文章评论、PASSWORD 解锁、作者资料和统计已接入 V2；公开页面统一使用三语前缀 |
| 管理端 | 登录会话、仪表盘、文章、首页槽位、分类标签、评论、友链、附件、配置和资料已实现 |
| 文档 | 当前事实由入口、handbook、governance 与 showcase 维护；`docs/superpowers/` 仅保留历史计划与设计上下文 |
| 部署 | AWS EC2、Route 53、S3、Docker Compose 与 Caddy 已运行；`main` 的 GHCR 发布和 GitHub OIDC SSH CD 已成功执行，三条公开 HTTPS 健康端点可访问 |

## 已知产品缺口

- 博客端核心导航、文章卡片和搜索结果尚未完整使用原生链接/按钮语义，键盘可达性与标题层级待收口。
- 已发布源码学习文章保留了旧源码提交的可追溯性，但 PASSWORD 授权等后续代码变化尚未逐篇回写到外部原稿和线上正文。
- Spotify 播放列表 ID 已进入配置，但博客端没有 Embed。
- 完整 SEO、robots、sitemap、RSS/Atom、Open Graph 和结构化数据尚未实现。

## 已知工程风险

- 管理端 access/refresh token 存在 localStorage，安全性依赖严格控制 XSS 面。
- 登录、评论重复检查和访问打点限流使用进程内 Caffeine，不适用于无协调的多实例部署。
- 博客 Sass 旧 API/`@import` 弃用提示已消除，入口 chunk 已拆至约 360 kB（gzip 约 127 kB）；Mermaid 最重的按需 chunk 约 691 kB（gzip 约 155 kB），构建边界为 700 kB。管理端 Browserslist 与 Baseline 数据已更新，不再提示过期；管理端大 bundle 和性能预算仍待处理。

## 最近验证

本次文档校准已运行：

- 后端评论应用层、H2 持久层和完整状态流定向回归通过：20 项测试，0 failures / 0 errors；根评论隐藏、重新通过、删除和恢复均覆盖回复树与文章计数。
- 后端 `mvn clean test` 完整验证通过；本机没有 Docker 时真实 MySQL 条件测试会跳过，真实方言结果以 CI 专项为准。
- 随机端口运行中 API 契约通过：4 项测试，0 failures / 0 errors；覆盖登录与 refresh 轮换、DEMO 只读、文章完整写入、multipart 附件和站点配置完整 PUT，并由后端 CI 常规测试自动执行。
- 2026-07-16 博客端完整测试、typecheck 与 production build 通过；分类/标签路由刷新、旧请求竞争、公开静态页打点和 canonical 去重回归已纳入测试。
- 2026-07-19 博客端作者资料已收口为单一数据源，全部公开路由统一三语前缀并兼容旧静态地址；本地 138 项测试、typecheck、production build 及首页—归档—语言切换真实页面回归通过。
- 2026-07-21 评论与留言板共用组件已补齐三语表单、回复、空状态、提交结果、失败提示和分页文案，语言切换会重新映射评论时间；博客端本地 139 项测试、typecheck 和 production build 通过。
- 2026-07-16 管理端完整测试、typecheck 与 production build 通过；文章草稿口令脱敏、账号隔离和会话清理回归已纳入测试。
- 本地 MySQL 合约脚本已在 Windows PowerShell 7 与 Ubuntu GitHub Actions `pwsh` 通过，覆盖凭据、数据库名、非空库和显式 `-Reset` 的安全边界。
- [`CI`](https://github.com/tongchang01/My-Blog/actions/workflows/ci.yml) 在 `main` 的 PR 与 push 执行五项检查，包含真实 MySQL 8.4 并发与迁移测试；实时结论和提交 SHA 以工作流页面为准，不在文档中复制会随下一次提交过期的“最新运行号”。
- [`Publish container images`](https://github.com/tongchang01/My-Blog/actions/workflows/images.yml) 使用同一提交 SHA 构建镜像并部署；远端发布会等待容器健康并检查 API Actuator，公网冒烟还要求三个 `/healthz` 返回固定正文 `ok`，最后始终撤销临时 SSH 入站。

未解决事项见 `open-issues.md`，实施顺序见 `roadmap.md`。
