# 发布检查清单

> 目标：在 H2/MySQL 测试通过后，按实际部署拓扑验证入口路径、CORS 和客户端 IP
> 信任边界。以下场景只选择与本次部署匹配的项，反向代理检查可与同源或跨域场景
> 组合执行。

## 1. 通用检查

- [ ] `mvn clean test` 通过，并完成真实 MySQL 方言验证。
- [ ] 前台执行 lint、typecheck 和 production build。
- [ ] CI/CD 至少覆盖后端测试、前台 lint/typecheck/build，并保存失败日志。
- [ ] 生产环境显式激活 `prod` profile，必填密钥和数据库变量均由环境注入。
- [ ] 所有 `LocalDateTime` 请求与响应按 `Asia/Tokyo` 本地时间解释，格式为
  `yyyy-MM-dd'T'HH:mm:ss`，不携带 offset。
- [ ] OpenAPI、Swagger UI、管理后台和公开前台的生产暴露范围符合部署策略。

## 2. SEO 与公开索引

- [ ] 公开页面输出正确的 `<title>`、description、canonical URL 和必要 Open Graph 元数据。
- [ ] 文章详情 canonical 与当前公开 URL 策略一致。
- [ ] `robots.txt` 可访问，并只允许抓取应公开收录的页面。
- [ ] `sitemap.xml` 可访问，只包含允许公开收录的页面。
- [ ] RSS / Atom 可访问，只包含允许公开展示的文章。
- [ ] PASSWORD 文章可以输出入口页元数据，但不得在 meta、RSS 或 Sitemap 扩展字段中暴露正文。

## 3. 同源反向代理

- [ ] 前端与 API 同源时，`MYBLOG_CORS_ALLOWED_ORIGINS` 可保持空列表。
- [ ] 请求 `/api/**` 后，代理转发到 Spring Boot 的路径仍包含 `/api` 前缀；禁止
  因 rewrite 或 `proxy_pass` 末尾斜杠误配置而剥离前缀。
- [ ] 浏览器正常请求公开接口和需要认证的后台接口。

## 4. 跨域前端

- [ ] `MYBLOG_CORS_ALLOWED_ORIGINS` 设置为实际前端的具体 origin，不使用 `*`，
  不只填写域名片段。
- [ ] 使用实际 origin 验证 OPTIONS 预检及后续请求均成功。
- [ ] 使用未授权 origin 验证预检被拒绝。

## 5. 反向代理与客户端 IP

- [ ] `MYBLOG_WEB_TRUSTED_PROXIES` 只包含实际代理 IP / CIDR；禁止默认信任整个
  私网网段。
- [ ] 代理覆盖而不是追加外部传入的 `X-Forwarded-For` 和 `X-Real-IP`。
- [ ] 使用两台客户端发起请求，确认后端解析出不同客户端 IP，并产生不同限流键。
- [ ] 绕过可信代理直连应用时，伪造转发头不会改变解析出的客户端 IP。

## 6. 直连 Spring Boot

- [ ] 没有反向代理时，`MYBLOG_WEB_TRUSTED_PROXIES` 保持空列表。
- [ ] 客户端 IP 直接来自连接远端地址，所有转发头均被忽略。

## 7. 附件存储

- [ ] S3 模式确认 `/media/**` 未注册；若显式开启历史 LOCAL 兼容，确认目录存在并
  记录关闭 `MYBLOG_STORAGE_LOCAL_WEB_ENABLED` 的时间点。
- [ ] 上传、读取、删除附件均在目标存储类型下验证通过。
- [ ] 公开附件 URL 不暴露本地磁盘路径、对象存储密钥或内部凭证。

## 8. 数据备份与恢复

- [ ] 明确数据库备份方式、保留周期和备份存放位置。
- [ ] 至少完成一次从备份恢复到临时库的演练。
- [ ] 确认附件存储备份或对象存储版本/生命周期策略。
- [ ] 记录恢复步骤和负责人，避免只有自动备份没有可执行恢复流程。

## 9. 上线冒烟

- [ ] 访问首页、文章详情、分类、标签、归档、关于页和搜索，确认公开主链路可用。
- [ ] PASSWORD 文章按当前实现状态展示锁定态或完整解锁流程。
- [ ] 后台登录、refresh、退出可用。
- [ ] DEMO 账号只读，敏感字段裁剪符合 O-002。
- [ ] 公开访问打点写入成功，后台 dashboard 可看到统计变化或聚合后的变化。
