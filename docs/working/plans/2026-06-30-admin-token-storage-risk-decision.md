# 后台 Token 存储风险接受记录

> 状态：暂缓 / 风险已接受。本文记录 O-008 的决策依据，不代表 localStorage 是更安全方案。

## 当前方案

后台 admin 当前把会话保存到 localStorage：

- key：`myblog-admin-session`
- 内容：`accessToken`、`refreshToken`、`accessExpiresAt`、`refreshExpiresAt`
- refresh 失败、session 解析失败或 refresh token 过期时，前台会清理本地会话。

后端认证机制：

- access token 是 JWT，默认 15 分钟有效。
- refresh token 是随机字符串，默认 7 天有效。
- 数据库只保存 refresh token hash，不保存明文。
- refresh 时旧 refresh token 会被撤销并轮换新 token。
- 旧 refresh token 重放、过期、撤销、账号锁定或账号删除都会失败。
- logout 和改密会撤销相关 token。

## 风险在哪里

localStorage 的核心风险是 XSS。

如果后台页面存在 XSS，攻击脚本可以直接读取：

```text
localStorage["myblog-admin-session"]
```

拿到 token 后，攻击者可以在 token 过期或被撤销前冒用后台会话。因为 refresh token 也在 localStorage 中，风险窗口会长于单纯 access token 泄漏。

典型触发来源：

- 后台渲染未清洗的文章 HTML、评论 HTML 或站点配置 HTML。
- 后台引入不可信第三方脚本、统计 SDK、CDN 脚本。
- 前端依赖供应链被污染。
- 浏览器插件或共享设备环境不可信。
- 调试代码把 token 输出到 console、URL、错误日志或第三方上报。

localStorage 还会跨浏览器重启保留会话。设备丢失、共享设备或未锁屏场景下，风险高于纯内存 token。

## 为什么暂不升级

HttpOnly Cookie 可以降低脚本直接读取 refresh token 的风险，但不是免费升级。

改造会同时引入：

- CSRF 防护设计。
- SameSite 策略选择。
- 同源 / 跨域部署差异。
- `withCredentials` 和 CORS credentials 配置。
- refresh cookie path、过期、撤销和清理策略。
- 本地开发、反向代理和生产部署一致性校准。
- 前端 refresh 流程和后端认证接口契约调整。
- 更多回归测试面。

当前项目是个人博客后台，后台使用者主要是站长本人，部署目标是单体单实例。现有后端 token 轮换、TTL、hash 存储和撤销机制已经覆盖主要会话生命周期风险。相比立即引入 Cookie + CSRF 体系，优先控制 XSS 面更符合当前项目复杂度。

## 当前约束

继续使用 localStorage 的前提：

- 后台不得渲染未清洗 HTML。
- 文章预览、评论展示、About Markdown 等 HTML 必须走可信清洗或安全渲染链路。
- token 不得写入 URL query、localStorage 之外的随意 key、console、日志或第三方 SDK。
- 减少后台第三方脚本和 CDN 依赖。
- 依赖升级需要关注供应链风险。
- DEMO 敏感字段裁剪必须由后端完成，不能依赖前端隐藏。

## 重开条件

出现以下任一情况，应重开 O-008：

- 后台开放给多人长期使用。
- 后台引入大量第三方脚本、插件或外部 SDK。
- 项目部署改为稳定同源架构，具备引入 HttpOnly Cookie 和 CSRF 防护的条件。
- 有更高安全要求，例如公开演示后台、客户环境、多人协作。
- 发生 XSS、token 泄漏或相关安全事件。

## 后续升级方向

若未来升级，倾向方案：

- refresh token 放 HttpOnly、Secure Cookie。
- access token 只保存在内存中。
- refresh 接口基于 Cookie 轮换 refresh token。
- 写请求引入 CSRF token 或双提交 Cookie 策略。
- 同源部署优先；跨域部署必须显式配置 credentials、SameSite 和 CORS。

该升级必须作为独立安全改造任务处理，不能夹在普通前台或后台功能开发中顺手修改。
