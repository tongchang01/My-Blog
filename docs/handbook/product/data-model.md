# 当前领域模型

> 状态：当前有效
> 适用范围：MyBlog V2 领域概念和模块归属
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：领域模型摘要

## 模块与聚合

| 模块 | 主要聚合 | 职责 |
| --- | --- | --- |
| identity | UserAccount、UserProfile、RefreshToken | 登录身份、公开资料和会话生命周期 |
| content | Article、Category、Tag | 文章发布、首页编排与分类标签 |
| comment | Comment | 文章评论、留言板和审核 |
| system | SiteConfig、Attachment、FriendLink | 站点内容、文件和友链 |
| stats | PageView、PageViewDaily | 访问明细、聚合和仪表盘 |
| common | MailFailureLog | 跨模块邮件失败记录与通用基础设施 |

## identity

- **UserAccount**：用户名、密码哈希、ADMIN/DEMO 类型、token version、登录失败计数、锁定时间和最近登录信息。
- **UserProfile**：昵称、头像、三语简介、所在地、主页、公开邮箱，以及 GitHub、Twitter、LinkedIn、知乎、Qiita、掘金链接。
- **RefreshToken**：用户 ID、token 哈希、过期时间、撤销状态和审计字段。刷新后旧记录被撤销并签发新记录。

UserAccount 与 UserProfile 逻辑上一对一，数据库分别落在 `t_user_auth` 和 `t_user_info`。

## content

- **Article**：三语标题和摘要、单份 Markdown 正文、分类、作者、标签、slug、五态状态、密码哈希、发布时间、封面附件、首页槽位和公开评论数。
- **Category**：三语名称、唯一 slug 和排序值，结构平铺。
- **Tag**：三语名称和唯一 slug，由后台维护。
- **HomepageSlot**：`NONE / PINNED / FEATURED`，只有 PUBLISHED 文章可占用。

文章密码目前只是 Article 的持久化属性和公开访问阻断条件，系统不存在 ArticleAccessToken 领域对象。

## comment

- **Comment**：目标、parent、reply-to、作者快照、Markdown、安全 HTML 和审核状态。
- **CommentTarget**：`ARTICLE + articleId` 或 `GUESTBOOK + 0`。
- **CommentAuthor**：可选系统用户 ID，以及昵称、邮箱、站点、IP 和 User-Agent 快照。
- **CommentContent**：用户提交的 Markdown 与后端清洗后的 HTML。

同一张 `t_comment` 表承载文章评论和留言板，两层结构由 parent 与 reply-to 共同表达。

## system

- **SiteConfig**：固定 ID 1，包含三语标题、副标题、关于 Markdown、Logo、favicon、备案号、Spotify 播放列表 ID 和建站日期。
- **Attachment**：存储类型、bucket、object key、公开 URL、MIME、大小、宽高、原文件名和 SHA-256。
- **FriendLink**：名称、URL、头像、描述、排序和 VISIBLE/HIDDEN 状态。

## stats 与通用日志

- **PageView**：可选文章 ID、语言、访客哈希、referrer 和访问时间。
- **PageViewDaily**：以文章 ID、语言和 JST 日期为复合键的 PV/UV 聚合。
- **MailFailureLog**：收件地址、模板、主题、失败状态、重试次数、脱敏错误和供应商消息 ID。

跨模块调用只通过 application 层公开能力完成，模块不直接访问其他模块的领域对象或持久化实现。
