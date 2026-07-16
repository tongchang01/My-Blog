# 安全基线

> 状态：当前有效
> 适用范围：V2 认证、授权、匿名入口、上传和前端会话
> 最后校准：2026-07-16
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/`、`frontend/apps/admin/src/features/auth/`
> 权威程度：规则

## 认证与会话

- 密码使用 BCrypt，默认 strength 10；禁止明文、可逆加密和日志输出。
- JWT access token 使用 HS256，默认 15 分钟；必须校验签名、issuer、过期、`typ=access` 和 token version。
- JWT secret 由 `MYBLOG_JWT_SECRET` 注入，UTF-8 至少 32 字节；缺失、默认值或过短时启动失败。
- refresh token 是 32 字节安全随机值，默认 7 天；明文只返回客户端，数据库只保存 SHA-256。
- refresh 使用行锁并轮换 token；同一旧 token 并发刷新最多一次成功。
- 退出和改密递增 token version，并撤销用户全部 refresh token；改密后重新登录。
- 认证错误统一为 `401 + 10002`，账号密码错误为 `401 + 10001`，授权错误为 `403 + 10003`。

## 权限

- 公开白名单必须同时声明 method 与 path。
- ADMIN 可以执行后台读写；DEMO 只能访问列出的后台 GET，并由 application 层裁剪敏感字段。
- DEMO 写入必须由后端拒绝，前端禁用按钮不构成授权。
- 未匹配白名单或角色规则的接口默认要求认证。
- PASSWORD 文章当前没有解锁凭证；不得把登录 access token 当作文章访问授权。

## 网络与浏览器

- API 使用 Bearer token，当前关闭 CSRF；若改用 Cookie 会话必须重新设计 CSRF 防护。
- CORS origins 必须显式配置，不使用 `*`；生产反向代理需验证同源转发。
- 只有远端地址命中 `myblog.web.trusted-proxies` 时才读取 `X-Forwarded-For` 或 `X-Real-IP`。
- 管理端当前把 access/refresh token 放在专用 localStorage session 中；不得写入 URL、console、第三方 SDK 或其他任意 key。
- 编辑器草稿不得持久化密码、token 等认证秘密；包含未发布内容的草稿必须按当前用户隔离，并在退出、会话失效和改密时清理该用户草稿。
- 未清洗 HTML 不得进入管理端和博客端 DOM，降低 localStorage token 被 XSS 窃取的风险。

## 匿名入口与上传

- 评论、留言和访问打点必须校验目标、限制频率，并使用统一客户端 IP 解析。
- 评论保存 Markdown 原文，但公开端只渲染后端清洗后的 HTML；重复内容需短期拦截。
- 访客哈希使用每日轮换 HMAC，不把原始 IP/User-Agent 当作公开标识。
- 附件上传仅 ADMIN 可用，最大 10 MiB，只接受通过真实内容识别的 JPEG、PNG、WebP、GIF，并限制尺寸、像素和 GIF 帧数。
- S3 使用默认凭证链；代码、配置和文档不得保存云访问密钥。

## 配置与扩展边界

安全相关环境变量见 `../ops/environment.md`。登录和访问打点限流当前使用进程内 Caffeine，多实例部署前必须替换为共享方案或明确接受实例间不共享。

安全变更至少覆盖密钥启动校验、JWT、refresh 轮换、角色边界、白名单、CORS、可信代理、限流、内容清洗和上传校验。
