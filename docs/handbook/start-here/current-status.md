# 当前状态

> 状态：当前有效
> 适用范围：MyBlog V2 开发与发布准备
> 最后校准：2026-07-11
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：当前进度权威源

## 总结

V2 的后端、公开博客主阅读链路和管理后台主要业务闭环已经实现。当前主线不是继续重建模块，而是确认生产拓扑并完成首次可回滚部署。

| 范围 | 当前状态 |
| --- | --- |
| 后端 | 五个业务模块与 common 已实现；Flyway V1–V4、14 张表；公开与后台 API 已覆盖 |
| 博客端 | 首页、文章、分类、标签、归档、搜索、关于、友链、留言板、文章评论、作者资料和统计已接入 V2 |
| 管理端 | 登录会话、仪表盘、文章、首页槽位、分类标签、评论、友链、附件、配置和资料已实现 |
| 文档 | 已重建为入口、handbook、governance、showcase 四部分；历史与过程材料由 Git 保留 |
| 部署 | prod profile、S3、health、CI 和 GHCR 镜像发布已验证；实际服务器预检、代理、域名、备份、回滚和首次上线尚未完成 |

## 已知产品缺口

- PASSWORD 文章没有解锁接口或文章访问凭证，公开详情与评论固定返回 `403 + 10003`。
- Spotify 播放列表 ID 已进入配置，但博客端没有 Embed。
- 完整 SEO、robots、sitemap、RSS/Atom、Open Graph 和结构化数据尚未实现。

## 已知工程风险

- 管理端 access/refresh token 存在 localStorage，安全性依赖严格控制 XSS 面。
- 登录、评论重复检查和访问打点限流使用进程内 Caffeine，不适用于无协调的多实例部署。
- 博客 build 存在 Sass 旧 API/`@import` 废弃提示和约 500 kB chunk；管理端存在浏览器数据过期提示和大 bundle。

## 最近验证

本次文档校准已运行：

- 后端 `mvn clean test` 完整验证通过：674 项测试，0 failures / 0 errors，4 项依赖 Docker 的 Testcontainers 测试因当前环境不可用而跳过。
- 博客端 23 个测试文件、69 项测试，typecheck 与 production build 通过。
- 管理端 47 个测试文件、186 项测试，typecheck 与 production build 通过。
- 本地 MySQL 合约脚本已在 Windows PowerShell 7 与 Ubuntu GitHub Actions `pwsh` 通过，覆盖凭据、数据库名、非空库和显式 `-Reset` 的安全边界。
- 主分支 `fb37fac5477183e61d3b8521ba76c2d5ffe9066b` 的 API/Web GHCR 镜像已成功构建并推送，两个镜像使用同一提交 SHA 标签。

未解决事项见 `open-issues.md`，实施顺序见 `roadmap.md`。
