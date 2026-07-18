# ADR-0019：PASSWORD 文章使用独立短期访问令牌

> 状态：当前有效
> 适用范围：content、comment 与 blog 公开访问
> 最后校准：2026-07-18
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/`、`frontend/apps/blog/src/features/articles/`
> 权威程度：ADR

## 背景

PASSWORD 文章此前只公开列表锁定标识，正文和文章评论完全不可用。后台登录 JWT 是账号授权，不能作为读者解锁凭证；把文章密码或永久授权写进 URL、Cookie 或 localStorage 也会扩大泄露面。

## 决策

- 使用 `POST /api/public/articles/{id}/unlock` 接收文章密码，并按客户端 IP 与文章限制每分钟 5 次尝试。
- 正确密码签发 32 字节安全随机令牌，默认有效 24 小时；明文只在响应中出现一次，数据库只保存 SHA-256 hash。
- 令牌通过 `X-Article-Access-Token` 访问对应 PASSWORD 文章的正文、评论查询和评论提交；不用于后台接口或其他文章。
- 博客端只将令牌存入当前浏览器标签页的 sessionStorage，不使用 URL、Cookie 或 localStorage。
- 密码改动、切换离开 PASSWORD 状态或软删除文章时撤销该文章全部令牌；恢复文章不恢复令牌。

## 结果

读者无需账号即可在当前标签页阅读受保护文章和参与其评论，关闭标签页即丢失本地令牌。令牌过期、撤销或缺失时需要再次提交密码。该方案延续单实例 Caffeine 限流边界；横向扩展时需随共享限流方案一起替换。
