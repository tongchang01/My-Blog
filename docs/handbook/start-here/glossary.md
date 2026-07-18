# 术语表

> 状态：当前有效
> 适用范围：MyBlog V2 文档与代码
> 最后校准：2026-07-18
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/`
> 权威程度：术语表

| 术语 | 定义 |
| --- | --- |
| V1 | 已从主线移除的旧版本，源码由只读分支 `archive/v1-master-2026-06-26` 保存 |
| V2 | 当前主线：V2 后端、blog 和 admin |
| blog / 博客端 | `frontend/apps/blog/` 公开读者端 |
| admin / 管理端 | `frontend/apps/admin/` 后台应用 |
| ADMIN | 可执行后台读写的管理员账号 |
| DEMO | 可读取裁剪后后台数据、不能写入的演示账号 |
| GUEST | 匿名公开访问语义，不是可登录账号体系 |
| access token | 短期 JWT 登录令牌，`typ=access` |
| refresh token | 用于轮换登录会话的随机字符串，数据库只保存哈希 |
| token version | 账号会话版本，变化后旧 access token 失效 |
| PASSWORD 文章 | 列表可见、需文章密码解锁正文与文章评论的文章状态 |
| article access token | PASSWORD 文章解锁后签发的短期随机令牌；与后台登录会话隔离 |
| homepage slot | 首页展示位置：NONE、PINNED、FEATURED |
| common | `com.tyb.myblog.v2.common` 通用基础设施，不是第六个业务模块 |
| 逻辑引用 | 由应用校验、数据库不建立 FOREIGN KEY 的表间关系 |
| 权威源 | 对当前事实或规则负责的唯一主要文档或代码位置 |
| 开放问题 | 尚未完成或达到触发条件后需要重开的事项 |
