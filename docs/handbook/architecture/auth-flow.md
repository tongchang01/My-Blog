# 认证与账号流程

> 状态：当前有效
> 适用范围：identity 与 common 安全组件
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/`
> 权威程度：架构权威说明

## 账号类型

| 类型 | 登录后台 | 后台读取 | 后台写入 |
| --- | --- | --- | --- |
| `ADMIN` | 允许 | 允许 | 允许 |
| `DEMO` | 允许 | 允许，敏感字段按用例裁剪 | 禁止 |
| `GUEST` | 禁止 | 不适用 | 禁止 |

`GUEST` 是领域枚举，不代表访客注册体系。公开访问和游客评论不创建后台账号。

## 登录

`POST /api/auth/login` 接收用户名和密码：

1. 用户名 trim 并按 `Locale.ROOT` 转小写。
2. Caffeine 按客户端 IP 与用户名执行 5 次失败、10 分钟冷却的进程内限流。
3. 持久化账号状态校验账号、类型、BCrypt 密码、失败次数和锁定时间。
4. 登录成功事务写入审计信息、创建 refresh token，并签发 access token。
5. 返回 access token、refresh token 和各自过期秒数。

未知账号、错误密码、锁定账号和 `GUEST` 登录统一返回 `401 + 10001`，不暴露内部判断。

## Access token

Access token 是 HS256 JWT，默认有效期 15 分钟。当前 claims：

| claim | 含义 |
| --- | --- |
| `iss` | profile 对应的 issuer |
| `iat` / `exp` / `jti` | 签发时间、过期时间和 token ID |
| `sub` | 字符串账号 ID |
| `typ` | 固定为 `access` |
| `ver` | `t_user_auth.token_version` |
| `username` | 登录用户名 |
| `roles` | `ADMIN` 或 `DEMO` |

每个受保护请求都会解析 JWT，并通过 `PersistentAccessTokenVerifier` 读取当前账号状态和 `token_version`。签名、issuer、时间、claims、账号或版本不符合要求时统一返回 `401 + 10002`。

## Refresh token

Refresh token 是高熵随机字符串，默认有效期 7 天。明文只返回客户端，数据库只保存 SHA-256 hash。

`POST /api/auth/refresh` 在单一事务中：

1. 计算 hash 并用 `SELECT ... FOR UPDATE` 锁定有效记录。
2. 重新读取未删除、未锁定且可登录的账号。
3. 撤销旧 refresh token。
4. 创建新 refresh token 并按最新账号状态签发 access token。

同一 refresh token 串行或并发使用最多成功一次。JWT 签发失败时事务回滚，旧 token 保持可重试状态。

## 退出与改密

- `POST /api/auth/logout`：要求有效 access token，递增账号 `token_version` 并撤销该账号全部 refresh token，实现全端退出。
- `PUT /api/auth/me/password`：只允许 `ADMIN`，行锁读取账号，校验当前密码，新密码长度为 8–128，更新 BCrypt hash、递增 `token_version`、撤销全部 refresh token，完成后重新登录。

## 当前用户与公开作者资料

- `GET /api/auth/me`：返回当前后台账号和完整资料。
- `PATCH /api/auth/me/profile`：只允许 `ADMIN` 部分更新资料。
- `GET /api/public/author-profile`：匿名返回前台作者卡片需要的公开资料。

资料包括昵称、头像、三语简介、所在地、个人主页、公开邮箱和 GitHub、Twitter/X、LinkedIn、知乎、Qiita、掘金链接。

## PASSWORD 文章边界

当前代码只支持 PASSWORD 状态、密码哈希保存和公开列表锁定标识。公开正文、文章评论读取和评论提交对 PASSWORD 文章返回 `403 + 10003`；当前没有解锁接口、article access token 或 `X-Article-Token` 处理链。

完整解锁属于遗留事项，见 `../start-here/open-issues.md`。

## 客户端 IP

登录、评论和统计共用 `ClientIpResolver`。直连请求只信任 remote address；只有远端命中 `myblog.web.trusted-proxies` 时才读取 `X-Forwarded-For` 或 `X-Real-IP`。生产反向代理必须同步配置可信代理范围。
