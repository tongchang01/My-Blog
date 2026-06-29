# 模块地图

> 状态：当前有效
> 适用范围：MyBlog V2 后端
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：架构权威源

## 本文档回答什么问题

本文档说明 V2 后端有哪些模块、每个模块负责什么、模块内部如何分层、模块之间允许怎样依赖，以及当前由哪些规则守护边界。

## 模块总览

```text
com.tyb.myblog.v2
├── common               common-infra：跨模块基础设施
│   ├── auth             当前认证主体、Bearer token 解析、MVC 参数解析
│   ├── auth.token       access token 签发 / 解码 / 验证端口
│   ├── security         Spring Security、JWT 实现、认证过滤器
│   ├── config           配置属性
│   ├── error            API 错误码、异常、统一响应
│   ├── storage          附件存储端口和实现
│   └── infrastructure   MyBatis-Plus / Flyway / DataSource 等基础设施
├── identity             用户、登录、JWT 双 token、当前用户资料
├── content              文章、分类、标签
├── comment              评论、留言板、审核、回复通知
├── system               站点配置、附件、友链
└── stats                访问统计、日聚合、后台统计看板
```

| 模块 | 核心职责 | 核心表 | 错误码段 |
|------|----------|--------|----------|
| `identity` | 登录、refresh、logout、改密、当前用户资料 | `t_user_auth`、`t_user_info`、`t_refresh_token` | 10xxx |
| `content` | 文章、分类、标签、定时发布、回收站 | `t_article`、`t_article_tag`、`t_category`、`t_tag` | 20xxx |
| `comment` | 文章评论、留言板、审核、评论计数、回复邮件 | `t_comment` | 30xxx |
| `system` | 站点配置、附件、友链 | `t_site_config`、`t_attachment`、`t_friend_link` | 40xxx |
| `stats` | 匿名打点、日 PV/UV 聚合、后台 dashboard | `t_page_view`、`t_page_view_daily` | 50xxx |
| `common-infra` | 响应、异常、安全、配置、存储、邮件、时间、架构守护 | 无独立业务表 | 90xxx / 99999 |

## 业务模块四层结构

`identity`、`content`、`comment`、`system`、`stats` 均采用四层结构：

```text
{module}/
├── web/             Controller、请求模型、响应模型、Web 层转换
├── application/     用例编排、Command、Query、Result、事务边界
├── domain/          领域模型、值对象、领域服务、Repository 端口
└── infrastructure/  Repository 实现、Mapper、存储或外部服务适配
```

当前代码目录已按该结构存在：

| 模块 | web | application | domain | infrastructure |
|------|-----|-------------|--------|----------------|
| identity | 已存在 | 已存在 | 已存在 | 已存在 |
| content | 已存在 | 已存在 | 已存在 | 已存在 |
| comment | 已存在 | 已存在 | 已存在 | 已存在 |
| system | 已存在 | 已存在 | 已存在 | 已存在 |
| stats | 已存在 | 已存在 | 已存在 | 已存在 |

## 层间依赖方向

```text
web ──► application ──► domain
                 │          ▲
                 │          │
infrastructure ──┴──────────┘
```

规则：

- `domain` 不依赖 Spring Web、Servlet、MyBatis、Mapper、Controller 等技术层。
- `application` 编排用例和事务，不依赖 `web` 或 `infrastructure` 具体实现。
- `web` 只负责 HTTP 接入、参数校验和响应模型，不直接访问 Mapper 或 Entity。
- `infrastructure` 实现 `domain` 中定义的端口。
- 业务模块之间禁止跨过 application 层直接访问对方内部模型。

## 跨模块依赖规则

| 调用方 | 被调方 | 允许方式 |
|--------|--------|----------|
| `comment` | `content` | 通过 `content.application` 公开接口校验文章或维护计数 |
| `comment` | `identity` | 通过 `identity.application` 公开接口读取后台用户展示信息 |
| `content` | `system` | 通过 `system.application` 公开接口校验附件引用 |
| `stats` | `content` | 通过 `content.application` 公开接口聚合文章标题等展示信息 |
| 任意业务模块 | `common` | 允许依赖稳定公共能力 |
| 业务模块 A | 业务模块 B 的 `domain` / `web` / `infrastructure` | 禁止 |
| `common` | 任意业务模块 | 禁止 |

## 认证边界

认证能力拆分为两部分：

```text
identity.application ──► common.auth.token.AccessTokenIssuer
common.security filter ──► common.auth.token.AccessTokenVerifier
common.security JwtTokenService ── implements AccessTokenIssuer / AccessTokenDecoder
identity.application PersistentAccessTokenVerifier ── implements AccessTokenVerifier
```

含义：

- `identity` 拥有登录、refresh token、用户状态、`token_version` 和认证用例。
- `common` 拥有无业务状态的 token 端口、JWT 编解码实现和 Spring Security 接入。
- 过滤器不直接依赖 identity 的 Entity、Mapper 或 Repository。
- access token 校验时通过端口完成 JWT 解码和用户状态校验。

## ArchUnit 守护规则

位置：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`

当前应守护的核心规则：

1. `domain` 不依赖 web、application、infrastructure 和框架接入层。
2. `web` 不依赖 `infrastructure`。
3. `application` 不依赖 `web` 或 `infrastructure`。
4. `common` 不依赖业务模块。
5. 跨业务模块只允许依赖对方 `application`。
6. `domain` 不直接获取当前时间，必须通过应用层传入或使用统一 Clock。
7. 业务模块不依赖 `common.security` 具体实现。
8. 顶层业务模块之间不得形成循环依赖。

任何架构规则违反都应让 `mvn test` 失败。

## 相关文档

- 术语表：`../start-here/glossary.md`
- 文档维护规则：`../rules/documentation.md`
- 包结构规则：`../rules/package-layout.md`
- ADR：`../adr/0001-modular-monolith.md`、`../adr/0003-four-layer-architecture.md`、`../adr/0004-six-business-modules.md`、`../adr/0012-archunit-guards.md`
