# 模块地图

> 状态：当前有效
> 适用范围：MyBlog V2 后端
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：架构权威说明

## 顶层模块

基础包固定为 `com.tyb.myblog.v2`。

| 包 | 职责 | 主要数据 |
| --- | --- | --- |
| `identity` | 后台账号、登录会话、当前用户资料、公开作者资料 | `t_user_auth`、`t_user_info`、`t_refresh_token` |
| `content` | 文章、分类、标签、首页槽位、定时发布 | `t_article`、`t_category`、`t_tag`、`t_article_tag` |
| `comment` | 文章评论、留言板、审核、回复通知 | `t_comment` |
| `system` | 站点配置、附件、友链 | `t_site_config`、`t_attachment`、`t_friend_link` |
| `stats` | 公开打点、PV/UV 日聚合、统计看板 | `t_page_view`、`t_page_view_daily` |
| `common` | 认证端口、安全接入、响应、错误、配置、存储、邮件、时间和持久化基础设施 | `t_mail_log` |

文档中的 `common-infra` 指 Java 顶层包 `common` 承载的公共基础设施，不是第六个业务域。

## 模块内分层

五个业务模块均包含：

```text
web             HTTP 请求、校验、响应和 Web 映射
application     用例编排、事务、Command、Query、Result 和跨模块公开能力
domain          领域模型、规则和 Repository 端口
infrastructure  Repository、Mapper、外部服务和框架适配
```

依赖方向：

```text
web ──► application ──► domain
             ▲             ▲
             └── infrastructure
```

`infrastructure` 可以实现 domain/application 定义的端口，但 application 和 domain 不能反向依赖具体实现。

## 跨模块协作

业务模块只能依赖其他业务模块的 `application` 包。禁止直接访问其他模块的 `domain`、`web`、`infrastructure`、Entity 或 Mapper。

当前主要协作：

- `comment -> content.application`：文章是否允许评论、评论计数。
- `content -> system.application`：封面附件引用与公开 URL。
- `stats -> content.application`：统计看板文章标题。
- 业务模块 `-> common`：稳定公共能力。

`common` 不得依赖任何业务模块。认证通过端口反转依赖：

```text
identity.application -> common.auth.token.AccessTokenIssuer
common.security -> common.auth.token.AccessTokenVerifier
identity.application.token.PersistentAccessTokenVerifier -> AccessTokenVerifier
common.security.auth.JwtTokenService -> AccessTokenIssuer + AccessTokenDecoder
```

## ArchUnit 守护

`ArchitectureRulesTest` 当前验证：

1. domain 不依赖上层、持久化、Servlet、Spring Web、Spring Security 或 MyBatis。
2. application 不依赖 web、infrastructure 或 Servlet。
3. web 不依赖 infrastructure，只允许使用白名单领域枚举。
4. infrastructure 不依赖 web。
5. common 和 token 端口保持业务模块、框架隔离。
6. 五个业务模块只通过 application 跨模块协作且不存在循环依赖。
7. domain 不直接读取系统时间或创建旧 `Date`。
8. 不允许恢复旧顶层 `com.tyb.myblog.v2.infrastructure` 包。

新增模块或调整边界时必须同步修改该测试和相关 ADR。
