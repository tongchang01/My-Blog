# 当前状态

> 状态：当前有效
> 适用范围：MyBlog V2 开发与发布准备
> 最后校准：2026-07-16
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：当前进度权威源

## 总结

V2 的后端、公开博客主阅读链路和管理后台主要业务闭环已经实现，并已运行在生产环境。当前主线是稳定线上运行、修复体验问题和补齐明确的产品缺口，而不是重建已有模块。

| 范围 | 当前状态 |
| --- | --- |
| 后端 | 五个业务模块与 common 已实现；Flyway V1–V4、14 张表；公开与后台 API 已覆盖 |
| 博客端 | 首页、文章、分类、标签、归档、搜索、关于、友链、留言板、文章评论、作者资料和统计已接入 V2 |
| 管理端 | 登录会话、仪表盘、文章、首页槽位、分类标签、评论、友链、附件、配置和资料已实现 |
| 文档 | 当前事实由入口、handbook、governance 与 showcase 维护；`docs/superpowers/` 仅保留历史计划与设计上下文 |
| 部署 | AWS EC2、Route 53、S3、Docker Compose 与 Caddy 已运行；`main` 的 GHCR 发布和 GitHub OIDC SSH CD 已成功执行，三条公开 HTTPS 健康端点可访问 |

## 已知产品缺口

- PASSWORD 文章没有解锁接口或文章访问凭证，公开详情与评论固定返回 `403 + 10003`。
- Spotify 播放列表 ID 已进入配置，但博客端没有 Embed。
- 完整 SEO、robots、sitemap、RSS/Atom、Open Graph 和结构化数据尚未实现。

## 已知工程风险

- 公开访问统计存在静态页漏记和 canonical slug 重复 PV。
- 评论审核并发与根评论失效规则可能使文章评论计数偏离公开树；首页槽位 1/2 上限也缺少原子持久化约束。
- 三端 CI 目前分别验证，关键跨端流程还没有运行中 API 契约层。
- 管理端 access/refresh token 存在 localStorage，安全性依赖严格控制 XSS 面。
- 登录、评论重复检查和访问打点限流使用进程内 Caffeine，不适用于无协调的多实例部署。
- 博客 Sass 旧 API/`@import` 弃用提示已消除；主 chunk 仍为约 477 kB（gzip 约 182 kB），尚未建立性能预算。管理端仍存在浏览器数据过期提示和大 bundle。

## 最近验证

本次文档校准已运行：

- 后端 `mvn clean test` 完整验证通过：683 项测试，0 failures / 0 errors；主分支 CI 的真实 MySQL 集成测试也已通过。
- 2026-07-16 博客端完整测试、typecheck 与 production build 通过；分类/标签路由刷新和旧请求竞争回归已纳入测试。
- 2026-07-16 管理端完整测试、typecheck 与 production build 通过；文章草稿口令脱敏、账号隔离和会话清理回归已纳入测试。
- 本地 MySQL 合约脚本已在 Windows PowerShell 7 与 Ubuntu GitHub Actions `pwsh` 通过，覆盖凭据、数据库名、非空库和显式 `-Reset` 的安全边界。
- GitHub Actions 的最新成功 CI 为 [`29255126872`](https://github.com/tongchang01/My-Blog/actions/runs/29255126872)，对应提交 `b6d985454768c745b3dd8ec65dfb918a4784c800`。
- 同一提交的镜像发布与生产部署为 [`29255126954`](https://github.com/tongchang01/My-Blog/actions/runs/29255126954)，部署后已从公网成功请求主站、`www` 和管理端的 `/healthz`。

未解决事项见 `open-issues.md`，实施顺序见 `roadmap.md`。
