# 领域与数据关系图

> 状态：当前有效
> 适用范围：MyBlog V2 模块协作与 14 张业务表
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/`、`MyBlog-springboot-v2/src/main/resources/db/migration/`
> 权威程度：关系视图

## 模块协作

```mermaid
flowchart LR
  identity["identity<br/>账号、资料、会话"]
  content["content<br/>文章、分类、标签"]
  comment["comment<br/>评论、留言板、审核"]
  system["system<br/>站点配置、附件、友链"]
  stats["stats<br/>访问统计"]
  common["common<br/>安全、存储、邮件等基础设施"]

  content -->|"校验封面附件"| system
  comment -->|"校验文章与评论策略"| content
  comment -->|"查询通知对象"| identity
  stats -->|"读取文章标题"| content
  identity --> common
  content --> common
  comment --> common
  system --> common
  stats --> common
```

箭头表示通过 application 能力或 common 抽象形成的允许依赖，不表示直接访问其他模块的 infrastructure。

## 表关系

```mermaid
erDiagram
  T_USER_AUTH ||--o| T_USER_INFO : "profile"
  T_USER_AUTH ||--o{ T_REFRESH_TOKEN : "sessions"
  T_USER_AUTH ||--o{ T_ARTICLE : "authors"

  T_CATEGORY ||--o{ T_ARTICLE : "categorizes"
  T_ATTACHMENT ||--o{ T_ARTICLE : "cover"
  T_ARTICLE ||--o{ T_ARTICLE_TAG : "tagged"
  T_TAG ||--o{ T_ARTICLE_TAG : "labels"

  T_ARTICLE ||--o{ T_COMMENT : "ARTICLE target"
  T_COMMENT ||--o{ T_COMMENT : "parent and reply"
  T_ARTICLE ||--o{ T_PAGE_VIEW : "viewed"
  T_ARTICLE ||--o{ T_PAGE_VIEW_DAILY : "aggregated"
```

`t_site_config`、`t_friend_link` 和 `t_mail_log` 是独立表。所有关系都是应用维护的逻辑引用，不是数据库 `FOREIGN KEY`；实际列、索引和例外规则见 `../architecture/schema-design.md`。
