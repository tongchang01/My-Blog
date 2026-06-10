# 模块地图

> 本文档回答："V2 现在有哪些模块？模块之间能怎么调？谁守护规则？"
> 适用范围：V2 R5 修订后的目标结构（DDL 冻结后落地）。
> 相关 ADR：ADR-0001、ADR-0003、ADR-0004（2026-06 修订）、ADR-0012

## 1. 模块总览（R5 修订）

```
com.tyb.myblog.v2
├── common               common-infra：跨模块基础设施
│   ├── auth.token       token 签发 / 验证端口与稳定载荷
│   ├── security         JWT 实现、过滤器、Spring Security 接入
│   ├── (响应封装 / 异常 / Knife4j / Clock / i18n / ArchUnit)
│   └── infrastructure   MyBatis-Plus / Flyway / DataSource 配置
├── identity             用户、登录、JWT 双 token（access + refresh）
├── content              文章、分类、标签
├── comment              评论、留言板（复用 t_comment）、审核
├── system               站点配置、附件、友链申请
└── stats                访客统计（自研日聚合 + 原始打点）
```

| 模块 | 核心表 | 错误码段 |
|------|------|------|
| identity | t_user_auth / t_user_info / t_refresh_token | 10xxx |
| content | t_article / t_article_tag / t_category / t_tag | 20xxx |
| comment | t_comment | 30xxx |
| system | t_site_config / t_attachment / t_friend_link | 40xxx |
| stats | t_page_view / t_page_view_daily（AuditOnlyBase 例外） | 50xxx |
| common-infra | — | 90xxx（兜底 99999） |

## 2. 业务模块四层结构

每个业务模块（identity / content / comment / system / stats）内部固定四层：

```
{module}
├── web                Controller、入参 Request、出参 Response、Mapper 转换
├── application        ApplicationService（用例编排）、Command/Query/Result
├── domain             Entity、值对象、Domain Service、Repository 接口
└── infrastructure
    └── persistence    Repository 实现、MyBatis-Plus Mapper、PO（如有分离）
```

## 3. 层间依赖方向

```
  web ──► application ──► domain
                 │              ▲
                 └──────────────┤
                                │
       infrastructure ─────────┘ (实现 domain.repository)
```

- `domain` 是核心，不依赖任何其它层
- `application` 在模块内依赖本模块 `domain`
- 跨模块协作时，`application` 只依赖对方模块公开的 `application` 契约
- 使用公共能力时，只依赖 `common` 的稳定 API，不依赖 `common.security` 等具体实现
- `web` 依赖 `application`，不直接访问 `infrastructure`
- `infrastructure` 实现 `domain` 中的仓储接口

认证能力边界：

```text
identity.application ──► common.auth.token.AccessTokenIssuer
common.security filter ──► common.auth.token.AccessTokenVerifier
common.security JWT implementation ── implements both ports
```

- identity 拥有登录、refresh token、用户状态、`token_version` 和签发用例
- common 拥有无业务状态的 token 端口、JWT 编解码实现与 Spring Security 接入
- common 不依赖 identity Entity、Mapper 或 Repository 实现

## 4. 跨模块依赖

| 调用方 | 被调方 | 允许方式 |
|--------|--------|----------|
| `comment` | `identity` | 通过 `identity.application` 公开接口（如获取用户名 / 头像） |
| `comment` | `content` | 通过 `content.application` 公开接口（如校验文章存在） |
| `content` | `system` | 通过 `system.application` 公开接口（如校验 attachment 存在） |
| `stats` | `content` / `identity` | 通过对应 application 公开接口（聚合时按文章 id 反查标题） |
| 任意业务模块 | `common-infra` | 允许直接依赖 |
| 业务模块 A | 业务模块 B 的 `infrastructure.persistence` | 🔴 禁止 |
| 业务模块 A | 业务模块 B 的 `domain` 内部实体 | 🔴 禁止 |
| `common-infra` | 任何业务模块 | 🔴 禁止（防反向依赖） |

## 5. ArchUnit 守护规则

位置：`src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`（DDL 冻结后按 R5 模块清单重写）

| # | 规则 | 含义 |
|---|------|------|
| 1 | `..domain..` 不依赖其它层及 Spring Web、MyBatis、Servlet API | 领域层保持纯净 |
| 2 | `..web..` 不依赖 `..infrastructure..` | HTTP 接入层不能访问 Entity、Mapper 等实现 |
| 3 | `..application..` 不依赖 `..web..` / `..infrastructure..` | 应用层通过 domain 端口访问技术实现 |
| 4 | `..common..` 不依赖业务模块 | 公共层不能反向依赖 |
| 5 | 跨业务模块只允许依赖对方 `application` | 禁止访问对方 domain、web、infrastructure |
| 6 | `..domain..` 不直接 `LocalDateTime.now()` / `new Date()` | 必须用注入的 Clock（ADR-0018 / R-011） |
| 7 | Flyway 脚本不出现 `FOREIGN KEY` | 禁 DB FK（ADR-0017 / R-012）—— 由 Flyway review 守护，非 ArchUnit |
| 8 | 业务模块不依赖 `common.security`，token 端口不依赖框架或业务模块 | 冻结认证所有权边界 |
| 9 | 顶层业务模块之间不存在循环依赖 | 防止模块边界逐步坍塌 |

任何 ArchUnit 违反 → `mvn test` 失败。

## 6. 状态

**当前 V2 实现与本图不完全一致**：旧实现按"common / infrastructure / identity / content / comment / system"6 模块写，stats 未建，common / infrastructure 是两个顶层包。DDL 冻结后按 `roadmap.md` M1 清理 + M2 基础设施补齐 + M3 模块重建。

## 7. 相关文档

- ADR：`../decisions/0001-modular-monolith.md`、`../decisions/0003-four-layer-architecture.md`、`../decisions/0004-six-business-modules.md`（R5 修订）、`../decisions/0012-archunit-guards.md`
- 规则：`../rules/package-layout.md`、`../rules/error-handling.md`
- 关联决定：`../product/decisions-draft.md` R5 B1
