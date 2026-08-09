# 当前状态

> 状态：当前有效
> 适用范围：MyBlog V2 开发与发布准备
> 最后校准：2026-08-09
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
| 部署   | AWS EC2、Route 53、S3、Docker Compose 与 Caddy 已运行；`main` 使用 GHCR、GitHub OIDC 和受限 SSH 部署同一 SHA，部署后检查三条 HTTPS 健康端点；MySQL 每周逻辑备份写入私有 S3 前缀并保留 30 天 |

## 已合并并部署的审查修复

`fix/review-validated-migration` 的修复已通过 [PR #61](https://github.com/tongchang01/My-Blog/pull/61) 合并到 `main`，合并提交为 `2334655e`：

- 博客端不再从 unpkg 运行时加载 lodash/md5，改由 Vite 打包本地 `lodash-es`；未使用的 md5 脚本已删除。
- 作者三语简介按纯文本渲染并保留换行，评论 HTML 在后端白名单清洗后再经 DOMPurify 输出兜底。
- 删除未被引用的旧认证/HTTP 工具文件；后端日志加入请求关联 ID，并通过响应头回传。

该批修复已通过完整 CI、发布 GHCR 镜像并部署生产，三条公开 HTTPS 健康端点均返回 `ok`。基础安全响应头随后由 `main` 合并提交 `30d75ee1` 完成同 SHA 部署和独立生产验收；令牌 Cookie 化、CSP、自动回滚和管理端模板瘦身等审查建议尚未实现，不因本次修复而视为完成。

## 已知产品缺口

- 博客端核心导航、文章卡片、搜索结果和 404 恢复入口已使用原生链接/按钮语义；标题层级仍待单独确认：首页文章卡片重复使用 `h1`，文章详情存在两个 `h1`，留言板没有页面级 `h1`。
- 外部 `blog-series` 的 58 篇源码学习文章及后台填写单已按当前 V2 代码校准；系列校验已覆盖编号、frontmatter、填写单、链接和源码路径。
- Spotify 播放列表 ID 已进入配置，但博客端没有 Embed。
- 完整 SEO、robots、sitemap、RSS/Atom、Open Graph 和结构化数据尚未实现。

## 已知工程风险

- 管理端 access/refresh token 存在 localStorage，安全性依赖严格控制 XSS 面。
- 登录、评论重复检查和访问打点限流使用进程内 Caffeine，不适用于无协调的多实例部署。

## 最近验证

2026-08-09 已完成 Caddy 基础安全响应头的仓库与生产收口：

- Caddy 对三个站点的 HTTPS 应用响应统一设置一年期 HSTS；静态博客、`www` 和管理端直接设置 `nosniff`、Referrer Policy、`X-Frame-Options: DENY` 和未使用浏览器能力限制。HSTS 不扩大到 `includeSubDomains` 或 `preload`。
- `/api` 不再由 Caddy 补齐页面策略，Spring Security 负责 API 的 `nosniff` 等安全默认值；后端测试固定 `nosniff` 契约。
- Caddy 2.11.4 配置语法以及真实静态/反向代理行为已在本地验证；CI 会以仓库固定的同版本镜像、三域静态夹具和模拟上游复验，而不是只匹配配置文本。
- `main` 合并提交 `30d75ee1` 已由同 SHA CD 部署；独立生产检查确认三个站点从 HTTP 精确跳转到 HTTPS、`/healthz` 返回 `ok`，五项基础响应头各出现一次；公开 API 返回有效 JSON，保留 Spring Security 的 `nosniff` 和 `X-Frame-Options: DENY`，且未被 Caddy 叠加页面策略。CSP Report-Only 与强制策略仍未实现。

2026-08-07 已启用最小 MySQL 数据备份：

- `myblog-mysql-backup` 通过 systemd timer 每周日 03:30（JST）执行；首次手工运行及脚本更新后的复验均成功。
- 备份对象及 SHA-256 校验文件写入私有 `recovery/mysql/` 前缀，S3 默认服务端加密；匿名读取返回拒绝，本地临时文件已清理。
- 个人博客不实施自动回滚或生产应用回退演练；数据库迁移前或真实恢复时才另行验证隔离环境恢复。

2026-08-02 `fix/review-validated-migration` 已完成并合并：

- 本地后端 `mvn clean test` 通过（721 项测试，0 failures / 0 errors）；本机无 Docker，8 项 Testcontainers 条件测试跳过。
- 博客端 lint、typecheck、测试与 production build 通过；管理端未修改，但 CI 仍执行其 typecheck、测试、构建和入口预算检查。
- [`CI`](https://github.com/tongchang01/My-Blog/actions/workflows/ci.yml) 的 [main 运行 30729121845](https://github.com/tongchang01/My-Blog/actions/runs/30729121845) 对合并提交 `2334655e` 的六项检查全部通过：后端测试、真实 MySQL 8.4 集成测试、Linux MySQL 初始化、blog 前端、admin 前端和部署工作流合约。
- [发布运行 30729121840](https://github.com/tongchang01/My-Blog/actions/runs/30729121840) 已构建同 SHA 的 GHCR 镜像、完成生产部署与三条公开 HTTPS 健康检查，并回收临时 SSH 入站规则。

未解决事项见 `open-issues.md`，实施顺序见 `roadmap.md`。
