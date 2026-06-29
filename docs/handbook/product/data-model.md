# V2 领域模型

> 本文档回答："V2 的业务实体、聚合边界和值对象是什么？"
> 范围：领域模型先于代码；表名仅作为落地参考，具体 DDL 以 `../architecture/schema-design.md` 为准。

## 1. 模块边界

| 模块 | 领域职责 | 主要聚合 |
|------|----------|----------|
| identity | 登录身份、账号资料、token 生命周期 | UserAccount、RefreshToken |
| content | 文章、分类、标签、文章访问密码 | Article、Category、Tag |
| comment | 文章评论、留言板、评论审核 | Comment |
| system | 站点配置、附件、友链 | SiteConfig、Attachment、FriendLink |
| stats | 访问明细和日聚合 | PageView、PageViewDaily |
| common-infra | 跨模块基础设施日志 | MailLog |

## 2. identity 模型

### UserAccount

聚合根。表示一个可被系统识别的账号。

核心属性：
- id
- username
- passwordHash
- type：ADMIN / DEMO / GUEST
- tokenVersion
- lastLoginAt
- lastLoginIp
- loginFailCount
- lockedUntil
- profile

业务规则：
- username 全局唯一。
- ADMIN 和 DEMO 可登录后台。
- GUEST 表示游客身份类型，不构成访客注册体系。
- 修改密码会递增 tokenVersion。
- 登录失败会累计失败次数并可能锁定账号。

落地参考：
- `t_user_auth`
- `t_user_info`

### UserProfile

UserAccount 的资料组成部分。

核心属性：
- nickname
- avatarUrl
- bioZh / bioJa / bioEn
- location
- website
- emailPublic
- githubUrl / twitterUrl / linkedinUrl / zhihuUrl / qiitaUrl / juejinUrl

业务规则：
- 与 UserAccount 1:1。
- 展示型简介支持三语。
- 社交链接可为空。

### RefreshToken

聚合根。表示一个可刷新登录态的长期凭证记录。

核心属性：
- id
- userId
- tokenHash
- expiresAt
- revoked

业务规则：
- 服务端不保存 refresh token 明文。
- tokenHash 全局唯一。
- revoked 或过期的 token 不可继续刷新。
- 清理任务可以物理删除过期或撤销记录。

## 3. content 模型

### Article

聚合根。表示一篇博客文章。

核心属性：
- id
- titleZh / titleJa / titleEn
- summaryZh / summaryJa / summaryEn
- body
- categoryId
- authorId
- slug
- status
- accessPassword
- publishAt
- coverAttachmentId
- commentCount
- tags

业务规则：
- body 是中文 Markdown 正文。
- title 和 summary 支持三语副本。
- status 只能是 DRAFT / PUBLISHED / PRIVATE / PASSWORD / SCHEDULED。
- PASSWORD 状态必须设置访问密码哈希。
- SCHEDULED 状态必须设置发布时间。
- PUBLISHED、PRIVATE、PASSWORD、SCHEDULED 必须有关联分类。
- slug 不作为唯一标识。
- commentCount 只缓存公开通过评论数。

落地参考：
- `t_article`
- `t_article_tag`

### Category

聚合根。表示文章分类。

核心属性：
- id
- nameZh / nameJa / nameEn
- slug
- sortOrder

业务规则：
- 分类平铺，不支持树。
- slug 唯一。
- 名称支持三语副本。
- 被文章引用时，删除前必须由应用层处理引用关系。

### Tag

聚合根。表示文章标签。

核心属性：
- id
- nameZh / nameJa / nameEn
- slug

业务规则：
- 标签由 ADMIN 维护。
- slug 唯一。
- 名称支持三语副本。

### ArticleAccessToken

值对象。表示 PASSWORD 文章访问授权。

核心属性：
- articleId
- expiresAt
- tokenType

业务规则：
- 只对指定文章有效。
- 过期后必须重新输入密码。
- 不能用于后台身份认证。

## 4. comment 模型

### Comment

聚合根。表示文章评论或留言板留言。

核心属性：
- id
- target：CommentTarget
- parentId
- replyToCommentId
- replyToUserId
- replyToNickname
- author：CommentAuthor
- content：CommentContent
- auditStatus

业务规则：
- target 决定评论挂载位置。
- auditStatus 只能是 PASS / PENDING / HIDDEN。
- 顶层评论没有 parentId 和 replyToCommentId。
- 回复评论必须挂到同一目标下。
- 不允许回复待审、隐藏或已删除评论。
- 公开展示只使用清洗后的 HTML。

落地参考：
- `t_comment`

### CommentTarget

值对象。表示评论挂载目标。

核心属性：
- type：ARTICLE / GUESTBOOK
- id

业务规则：
- ARTICLE 的 id 是文章 id。
- GUESTBOOK 的 id 固定为 0。
- 不允许跨 target 回复。

### CommentAuthor

值对象。表示评论作者快照。

核心属性：
- userId
- nickname
- email
- site
- ip
- userAgent

业务规则：
- 游客评论 userId 为空。
- nickname 和 email 必填。
- site 只能使用 http 或 https。
- ip 和 userAgent 仅用于审计和防刷，不公开展示。

### CommentContent

值对象。表示评论内容。

核心属性：
- markdown
- safeHtml

业务规则：
- markdown 是用户提交原文。
- safeHtml 是后端解析并清洗后的安全 HTML。
- 前台不得直接渲染 markdown。

## 5. system 模型

### SiteConfig

聚合根。表示全站配置。

核心属性：
- id
- siteTitleZh / siteTitleJa / siteTitleEn
- siteSubtitleZh / siteSubtitleJa / siteSubtitleEn
- aboutMdZh / aboutMdJa / aboutMdEn
- logoUrl
- faviconUrl
- icpNo
- spotifyPlaylistId

业务规则：
- 全站只有一份配置。
- id 固定为 1。
- 展示型字段支持三语副本。
- 非展示型字段保持单值。

### Attachment

聚合根。表示一个上传文件。

核心属性：
- id
- storageType
- bucket
- objectKey
- publicUrl
- contentType
- fileSize
- width
- height
- originalFilename
- hashSha256

业务规则：
- 上传前必须校验 MIME 和大小。
- hashSha256 用于内容去重。
- 软删除附件被再次上传时优先恢复。
- 文章封面和正文图片通过附件引用或 URL 引用使用。

### FriendLink

聚合根。表示一个友链。

核心属性：
- id
- name
- url
- avatarUrl
- description
- sortOrder
- status

业务规则：
- status 只能是 VISIBLE / HIDDEN。
- description 是单中文字段。
- 友链申请不属于当前聚合。

## 6. stats 模型

### PageView

聚合根。表示一次页面访问明细。

核心属性：
- id
- articleId
- lang
- visitorHash
- referrer
- createdAt

业务规则：
- articleId 为空表示非文章页。
- visitorHash 不可逆。
- 明细只保留短期。

### PageViewDaily

聚合根。表示按日聚合的访问统计。

核心属性：
- articleId
- lang
- statDate
- pv
- uv

业务规则：
- statDate 按 JST 日期计算。
- articleId 为 0 表示首页或非文章页汇总。
- 聚合数据用于仪表盘和前台热图。

## 7. common-infra 模型

### MailLog

聚合根。表示邮件发送失败记录。

核心属性：
- id
- toEmail
- template
- subject
- status
- retryCount
- errorMessage
- providerMessageId
- paramsJson
- createdAt

业务规则：
- V2 起点只写失败日志。
- errorMessage 必须脱敏。
- 清理任务保留 90 天。

## 8. 跨模块协作

| 场景 | 协作规则 |
|------|----------|
| 评论文章 | comment 通过 content 的应用能力校验文章存在、可见且允许评论 |
| 评论通知 | comment 读取被回复评论作者信息，必要时通过 identity 查询系统用户邮箱 |
| 文章封面 | content 通过 system 的应用能力校验附件存在 |
| 统计标题展示 | stats 通过 content 的应用能力读取文章标题 |
| 后台当前用户 | 各模块通过 identity 提供的当前用户能力取得操作者 |

跨模块只能走 application 能力，不直接访问对方的持久化实现或内部领域实体。
