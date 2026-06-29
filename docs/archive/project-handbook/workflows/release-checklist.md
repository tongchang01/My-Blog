# 发布检查清单

> 目标：在 H2/MySQL 测试通过后，按实际部署拓扑验证入口路径、CORS 和客户端 IP
> 信任边界。以下场景只选择与本次部署匹配的项，反向代理检查可与同源或跨域场景
> 组合执行。

## 1. 通用检查

- [ ] `mvn clean test` 通过，并完成真实 MySQL 方言验证。
- [ ] 生产环境显式激活 `prod` profile，必填密钥和数据库变量均由环境注入。
- [ ] 所有 `LocalDateTime` 请求与响应按 `Asia/Tokyo` 本地时间解释，格式为
  `yyyy-MM-dd'T'HH:mm:ss`，不携带 offset。

## 2. 同源反向代理

- [ ] 前端与 API 同源时，`MYBLOG_CORS_ALLOWED_ORIGINS` 可保持空列表。
- [ ] 请求 `/api/**` 后，代理转发到 Spring Boot 的路径仍包含 `/api` 前缀；禁止
  因 rewrite 或 `proxy_pass` 末尾斜杠误配置而剥离前缀。
- [ ] 浏览器正常请求公开接口和需要认证的后台接口。

## 3. 跨域前端

- [ ] `MYBLOG_CORS_ALLOWED_ORIGINS` 设置为实际前端的具体 origin，不使用 `*`，
  不只填写域名片段。
- [ ] 使用实际 origin 验证 OPTIONS 预检及后续请求均成功。
- [ ] 使用未授权 origin 验证预检被拒绝。

## 4. 反向代理与客户端 IP

- [ ] `MYBLOG_WEB_TRUSTED_PROXIES` 只包含实际代理 IP / CIDR；禁止默认信任整个
  私网网段。
- [ ] 代理覆盖而不是追加外部传入的 `X-Forwarded-For` 和 `X-Real-IP`。
- [ ] 使用两台客户端发起请求，确认后端解析出不同客户端 IP，并产生不同限流键。
- [ ] 绕过可信代理直连应用时，伪造转发头不会改变解析出的客户端 IP。

## 5. 直连 Spring Boot

- [ ] 没有反向代理时，`MYBLOG_WEB_TRUSTED_PROXIES` 保持空列表。
- [ ] 客户端 IP 直接来自连接远端地址，所有转发头均被忽略。

## 6. 附件存储

- [ ] S3 模式确认 `/media/**` 未注册；若显式开启历史 LOCAL 兼容，确认目录存在并
  记录关闭 `MYBLOG_STORAGE_LOCAL_WEB_ENABLED` 的时间点。
