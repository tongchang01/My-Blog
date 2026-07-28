# 当前状态

> 状态：当前有效
> 适用范围：MyBlog V2 开发与发布准备
> 最后校准：2026-07-28
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：当前进度权威源

## 总结

V2 的后端、公开博客主阅读链路和管理后台主要业务闭环已经实现，并已运行在生产环境。当前主线是稳定线上运行、修复体验问题和补齐明确的产品缺口，而不是重建已有模块。

| 范围   | 当前状态                                                                                                                                  |
| ------ | ----------------------------------------------------------------------------------------------------------------------------------------- |
| 后端   | 五个业务模块与 common 已实现；Flyway V1–V6、16 张表；公开与后台 API 已覆盖                                                                |
| 博客端 | 首页、文章、分类、标签、归档、搜索、关于、友链、留言板、文章评论、PASSWORD 解锁、作者资料和统计已接入 V2；公开页面统一使用三语前缀        |
| 管理端 | 登录会话、仪表盘、文章、首页槽位、分类标签、评论、友链、附件、配置和资料已实现                                                            |
| 文档   | 当前事实由入口、handbook、governance 与 showcase 维护；任务计划和评审材料完成迁移后删除，由 Git 历史追溯                                        |
| 部署   | AWS EC2、Route 53、S3、Docker Compose 与 Caddy 已运行；`main` 使用 GHCR、GitHub OIDC 和受限 SSH 部署同一 SHA，部署后检查三条 HTTPS 健康端点 |

## 已知产品缺口

- 博客端核心导航、文章卡片、搜索结果和 404 恢复入口已使用原生链接/按钮语义；标题层级仍待单独确认：首页文章卡片重复使用 `h1`，文章详情存在两个 `h1`，留言板没有页面级 `h1`。
- 已发布源码学习文章保留了旧源码提交的可追溯性，但 PASSWORD 授权等后续代码变化尚未逐篇回写到外部原稿和线上正文。
- Spotify 播放列表 ID 已进入配置，但博客端没有 Embed。
- 完整 SEO、robots、sitemap、RSS/Atom、Open Graph 和结构化数据尚未实现。

## 已知工程风险

- 管理端 access/refresh token 存在 localStorage，安全性依赖严格控制 XSS 面。
- 登录、评论重复检查和访问打点限流使用进程内 Caffeine，不适用于无协调的多实例部署。
- 本地 MySQL 自动初始化脚本与 local 默认管理员初始化存在账号冲突；修复前使用手工回退流程，见 ISSUE-022。

## 最近验证

2026-07-28 当前修复分支已完成：

- 后端 `mvn clean test`：705 项测试，0 failures / 0 errors；本机无 Docker，8 项 Testcontainers 条件测试跳过，真实 MySQL 结果继续以 CI 专项为准。
- 博客端：148 项测试、lint、typecheck 与 production build 通过。
- 管理端：测试、typecheck、production build 与首屏预算检查通过；入口 JS 338.44 KiB、CSS 63.18 KiB、合计 401.62 KiB，低于 350 / 70 / 420 KiB 预算。
- 部署工作流合约通过，持续校验镜像发布、OIDC、临时 SSH、同 SHA 部署、三域名公网冒烟和规则撤销。
- 本地 MySQL PowerShell 合约继续覆盖输入与安全边界，但真实 local 启动和种子串联存在 ISSUE-022，不再把替身合约写成端到端通过。
- [`CI`](https://github.com/tongchang01/My-Blog/actions/workflows/ci.yml) 在 `main` 的 PR、push 与手动触发定义六项检查，包含真实 MySQL 8.4 并发与迁移测试；实时结论和提交 SHA 以工作流页面为准。
- [`Publish container images`](https://github.com/tongchang01/My-Blog/actions/workflows/images.yml) 使用同一提交 SHA 构建镜像并部署；远端发布会等待容器健康并检查 API Actuator，公网冒烟还要求三个 `/healthz` 返回固定正文 `ok`，最后始终撤销临时 SSH 入站。

未解决事项见 `open-issues.md`，实施顺序见 `roadmap.md`。
