# MyBlog V2 文档整理计划

> 状态：评审草案
> 日期：2026-06-29
> 适用范围：`docs/`、`frontend/apps/admin/docs/` 以及与 V2 开发相关的项目文档
> 目标：把文档整理成“当前开发可信源”，而不是历史过程材料的堆积

## 1. 背景与问题

当前 MyBlog 仓库同时保留了 V1 历史资料、V2 后端重构文档、前台迁移文档、后台开发计划、阶段 review、归档资料和项目展示文档。文档数量已经足够支撑开发，但存在几个实际问题：

1. **路径分散**：权威文档在 `docs/project-handbook/`，过程文档在 `docs/superpowers/`，后台文档在 `frontend/apps/admin/docs/`，展示文档散落在 `docs/` 根目录。
2. **命名不统一**：存在中文文件名、英文文件名、日文文件名、日期前缀文件、`.zh-CN.md` 后缀、`README.md` 多层复用等多种风格。
3. **权威源不清楚**：同一主题可能同时出现在设计文档、计划文档、规则文档、API 契约和 review 中，后续开发时不容易判断该信哪一份。
4. **文档与代码不同步**：部分文档仍描述旧阶段、旧设计或已完成事项；有些功能已经落地，但状态没有统一更新。
5. **未完成和争议项分散**：PASSWORD 解锁、DEMO 字段裁剪、后台后续页面、部署与 CI 等事项散在路线图、计划和 review 中。
6. **开发文档语言混杂**：展示文档可以多语言，但开发规范、接口契约、架构说明和计划应统一中文，降低维护成本。
7. **过程材料过多干扰当前判断**：历史计划和阶段复盘有价值，但不应继续作为当前实现依据。

## 2. 整理目标

本次整理不是简单移动文件，而是一次文档治理：

1. 建立清晰目录结构，让“当前有效、临时过程、历史归档”一眼可分。
2. 合并重复内容，减少同一结论在多个文件中各写一遍。
3. 校准文档与当前 V2 代码，修正过期描述。
4. 统一中文开发文档、文件命名、文档头部和状态标记。
5. 把已完成事项、未完成事项、争议事项分别收口到固定位置。
6. 为后续前台、后台、后端联调提供稳定的文档入口。
7. 保留历史资料，但降低其对当前开发的干扰。

## 3. 非目标

本次整理不建议同时做以下事情：

1. 不修改业务代码。
2. 不重写 V1 文档体系，只把 V1 相关资料标为历史参考。
3. 不删除有追溯价值的历史计划和 review，优先归档。
4. 不在整理过程中重新设计业务功能，争议只登记，不顺手裁决。
5. 不一次性强行修完所有文档细节，先建立结构和权威源，再分批校准。

## 4. 目标目录结构

建议把 `docs/` 调整为以下结构：

```text
docs/
├── README.md
├── handbook/
│   ├── README.md
│   ├── start-here/
│   │   ├── project-overview.md
│   │   ├── current-status.md
│   │   ├── roadmap.md
│   │   ├── open-issues.md
│   │   ├── glossary.md
│   │   └── pitfalls.md
│   ├── architecture/
│   │   ├── README.md
│   │   ├── module-map.md
│   │   ├── request-flow.md
│   │   ├── auth-flow.md
│   │   ├── persistence-strategy.md
│   │   └── schema-design.md
│   ├── rules/
│   │   ├── README.md
│   │   ├── documentation.md
│   │   ├── package-layout.md
│   │   ├── sql-placement.md
│   │   ├── comment-style.md
│   │   ├── error-handling.md
│   │   ├── api-response.md
│   │   ├── security-baseline.md
│   │   └── testing-policy.md
│   ├── adr/
│   │   ├── README.md
│   │   └── 0001-*.md
│   ├── api/
│   │   ├── README.md
│   │   ├── auth.md
│   │   ├── article.md
│   │   ├── attachment.md
│   │   ├── category-tag.md
│   │   ├── comment.md
│   │   ├── friend-link.md
│   │   ├── site-config.md
│   │   └── stats.md
│   ├── product/
│   │   ├── README.md
│   │   ├── feature-inventory.md
│   │   ├── use-cases.md
│   │   ├── business-rules.md
│   │   ├── data-model.md
│   │   └── er-diagram.md
│   ├── frontend/
│   │   ├── blog/
│   │   │   ├── README.md
│   │   │   ├── pages.md
│   │   │   ├── routing.md
│   │   │   └── integration-status.md
│   │   └── admin/
│   │       ├── README.md
│   │       ├── foundation.md
│   │       ├── auth-session.md
│   │       ├── article-management.md
│   │       └── integration-status.md
│   ├── ops/
│   │   ├── README.md
│   │   ├── local-development.md
│   │   ├── environment.md
│   │   ├── build-and-test.md
│   │   ├── release-checklist.md
│   │   └── deployment.md
│   └── workflows/
│       ├── README.md
│       ├── add-new-api.md
│       ├── add-new-module.md
│       ├── add-new-table.md
│       └── write-adr.md
├── working/
│   ├── README.md
│   ├── plans/
│   ├── reviews/
│   └── research/
├── governance/
│   ├── README.md
│   ├── branch-policy.md
│   └── repository-reorganization.md
├── showcase/
│   ├── README.md
│   ├── myblog-showcase.zh-CN.md
│   ├── myblog-showcase.en.md
│   └── myblog-showcase.ja.md
└── archive/
    ├── README.md
    ├── superpowers/
    └── frontend-user-v2-migration/
```

## 5. 三类文档状态

后续所有文档只允许落入三种大状态。

### 5.1 当前有效

路径：`docs/handbook/`

用途：写代码、联调、测试、部署时必须参考的权威资料。

要求：

- 必须中文。
- 必须和当前代码一致。
- 必须有状态头。
- 冲突时以这里为准。
- 历史计划不能直接放入这里，必须提炼成当前结论。

### 5.2 临时过程

路径：`docs/working/`

用途：近期计划、阶段 review、调研、一次性分析。

要求：

- 可以带日期前缀。
- 完成后必须处理：提炼进 `handbook/` 或移入 `archive/`。
- 不作为长期权威源。

### 5.3 历史归档

路径：`docs/archive/`

用途：保留历史上下文和追溯价值。

要求：

- 不再更新业务内容。
- 不作为当前实现依据。
- 文件开头需要标记“历史归档”。

## 6. 统一文档头部

建议所有 `handbook/` 下的文档使用统一头部：

```md
# 文档标题

> 状态：当前有效 / 草案 / 待校准 / 已废弃
> 适用范围：V2 后端 / V2 前台 / V2 后台 / 运维 / 全项目
> 最后校准：YYYY-MM-DD
> 对应代码：`MyBlog-springboot-v2/...`、`frontend/apps/...`
> 权威程度：权威源 / 参考资料

## 本文档回答什么问题

...
```

状态含义：

| 状态 | 含义 |
|------|------|
| 当前有效 | 已校准当前代码，可作为开发依据 |
| 草案 | 还在讨论，不能直接作为实现依据 |
| 待校准 | 可能有价值，但尚未和代码核对 |
| 已废弃 | 保留追溯，不能作为当前依据 |

## 7. 命名规则

### 7.1 通用规则

- 使用英文小写 + 短横线。
- 不使用空格。
- 不使用中文、日文文件名作为开发文档文件名。
- 不使用 `.zh-CN.md` 后缀；开发文档默认中文。
- `README.md` 只用于目录入口，不承载大量业务细节。

示例：

```text
current-status.md
auth-flow.md
security-baseline.md
release-checklist.md
admin-article-management.md
blog-routing.md
```

### 7.2 允许日期前缀的目录

仅以下目录允许日期前缀：

```text
docs/working/plans/
docs/working/reviews/
docs/working/research/
docs/archive/
```

示例：

```text
2026-06-29-admin-auth-review.md
2026-06-29-blog-search-plan.md
```

### 7.3 ADR 命名

ADR 保留编号：

```text
0007-jwt-via-spring-security-jose.md
0018-timezone-asia-tokyo.md
```

## 8. 权威源划分

为避免同一内容散落在多处，后续按以下规则收口。

| 内容 | 权威位置 | 其他文档处理 |
|------|----------|--------------|
| 当前进度 | `handbook/start-here/current-status.md` | 计划和 review 只保留历史细节 |
| 后续路线 | `handbook/start-here/roadmap.md` | 完成后更新状态并归档旧计划 |
| 未完成 / 争议项 | `handbook/start-here/open-issues.md` | 其他文档只链接，不重复描述 |
| 术语定义 | `handbook/start-here/glossary.md` | 其他文档使用统一术语 |
| 架构现状 | `handbook/architecture/` | 设计草案完成后提炼到这里 |
| 编码规则 | `handbook/rules/` | 不在计划文档里重复写规则 |
| 接口契约 | `handbook/api/` | 前后端文档只链接对应接口 |
| 业务规格 | `handbook/product/` | API 和实现文档不重复业务背景 |
| 前台规格 | `handbook/frontend/blog/` | 旧迁移文档归档 |
| 后台规格 | `handbook/frontend/admin/` | `frontend/apps/admin/docs/` 迁入或设跳转说明 |
| 环境变量 | `handbook/ops/environment.md` | 其他文档只引用 |
| 构建测试 | `handbook/ops/build-and-test.md` | README 只放最短入口 |
| 发布检查 | `handbook/ops/release-checklist.md` | 不散落在 roadmap/review 中 |
| 分支策略 | `governance/branch-policy.md` | 不放入开发手册主体 |
| 项目展示 | `showcase/` | 可保留中英日多语言 |

## 9. 必须新增或改写的核心文档

### 9.1 `docs/README.md`

作为总入口，说明：

- 当前开发看 `handbook/`。
- 临时计划看 `working/`。
- 历史资料看 `archive/`。
- 项目展示看 `showcase/`。
- 仓库治理看 `governance/`。

### 9.2 `handbook/start-here/current-status.md`

统一记录 V2 当前进度：

- V1：只读历史参考。
- V2 后端：六大模块完成度。
- V2 前台：已完成页面和待完成页面。
- V2 后台：已完成后台基础、文章列表、待完成编辑和其他业务页。
- 当前主线：前台读者链路 + 后台内容生产闭环。

### 9.3 `handbook/start-here/open-issues.md`

统一收口未完成和争议项。建议格式：

```md
## O-001 PASSWORD 文章完整解锁流程

- 状态：未完成
- 优先级：P1
- 影响范围：后端 content / 前台 blog / 评论
- 当前代码现状：...
- 争议点：...
- 下一步：...
- 相关文档：...
```

建议首批登记：

- PASSWORD 文章完整解锁流程。
- DEMO 敏感字段裁剪边界。
- 后台文章编辑器和附件上传体验。
- 前台搜索实现方式。
- RSS / Sitemap / SEO。
- CI/CD 与部署文档。
- 多实例部署下登录、评论、统计限流方案。
- token 存储方式是否从 localStorage 升级为 HttpOnly Cookie。

### 9.4 `handbook/start-here/glossary.md`

统一术语：

| 术语 | 建议定义 |
|------|----------|
| V1 | `MyBlog-springboot/` 与旧 `MyBlog-vue/`，只作历史参考 |
| V2 | 当前重构主线 |
| 前台 / blog | 访客端博客页面 |
| 后台 / admin | 管理端 |
| common-infra | Java 包 `com.tyb.myblog.v2.common` 对应的公共基础设施层 |
| PASSWORD 文章 | 访问受限文章，不等于后台登录密码 |
| DEMO | 后台只读演示账号 |
| 权威源 | 当前开发必须以其为准的文档 |
| 过程材料 | 阶段计划、review、调研等临时文档 |

### 9.5 `handbook/rules/documentation.md`

新增文档维护规则：

- 新功能完成后必须更新状态或对应模块文档。
- 新 API 完成后必须更新 `handbook/api/`。
- 新规则必须放入 `handbook/rules/`，不能只写在计划里。
- 计划完成后必须提炼或归档。
- 禁止复制粘贴同一段接口契约到多个文件。
- `archive/` 不作为当前实现依据。
- 冲突时以 `handbook/` 为准。

### 9.6 `handbook/ops/environment.md`

统一环境变量和运行前置条件：

- 后端 local/prod/test 必填变量。
- 前台和后台 Vite 变量。
- JWT、数据库、统计 hash、CORS、trusted proxies、邮件、对象存储。
- 哪些变量可以默认，哪些生产必须显式设置。

## 10. 现有文档迁移建议

### 10.1 根目录散落文档

| 当前路径 | 建议去向 | 处理方式 |
|----------|----------|----------|
| `docs/local-development.md` | `docs/handbook/ops/local-development.md` | 合并进 ops 体系，去重环境变量 |
| `docs/deep-research-report.md` | `docs/working/research/` 或 `docs/archive/research/` | 先标为待校准，再决定是否提炼 |
| `docs/refactor-plan.zh-CN.md` | `docs/archive/` | 历史重构计划，当前结论提炼到 roadmap/status |
| `docs/MyBlog-项目展示.md` | `docs/showcase/myblog-showcase.zh-CN.md` | 展示文档保留 |
| `docs/MyBlog-Project-Showcase.en.md` | `docs/showcase/myblog-showcase.en.md` | 展示文档保留 |
| `docs/MyBlog-プロジェクト紹介.ja.md` | `docs/showcase/myblog-showcase.ja.md` | 展示文档保留 |

### 10.2 `docs/project-handbook/`

建议更名为 `docs/handbook/`，并做内容重整：

| 当前目录 | 建议去向 | 处理方式 |
|----------|----------|----------|
| `CLAUDE.md` | `handbook/start-here/ai-entry.md` 或根 `AGENTS.md` 引用 | 保留 AI 入口，但避免和总入口重复 |
| `INDEX.md` | `handbook/README.md` | 更新为新目录索引 |
| `overview.md` | `handbook/start-here/project-overview.md` | 校准前后端现状 |
| `status.md` | `handbook/start-here/current-status.md` | 作为进度权威源 |
| `roadmap.md` | `handbook/start-here/roadmap.md` | 去掉已完成阶段细节，已完成转 status |
| `pitfalls.md` | `handbook/start-here/pitfalls.md` | 保留红线和踩坑 |
| `arch/` | `handbook/architecture/` | 保持主题命名 |
| `rules/` | `handbook/rules/` | 新增 documentation.md |
| `decisions/` | `handbook/adr/` | 只改目录名，文件名保持 |
| `api-contract/` | `handbook/api/` | 校准真实接口 |
| `product/` | `handbook/product/` | 保留业务规格 |
| `frontend-user/` | `handbook/frontend/blog/` | 拆出页面、路由、联调状态 |
| `migration/` | `handbook/migration/` 或 `archive/migration/` | 若仍会执行则保留，否则归档 |
| `plans/` | `working/plans/` 或 `archive/` | 完成的归档，未完成的重写成 open issue |
| `specs/` | `archive/` 或提炼入 architecture/product/api | 不直接作为权威源 |

### 10.3 `docs/superpowers/`

建议整体归档：

```text
docs/archive/superpowers/
```

处理原则：

- `plans/`：大多是一次性过程材料，归档。
- `reviews/`：近期仍有价值的结论提炼进 `pitfalls.md`、`current-status.md` 或 `open-issues.md`，原文归档。
- `specs/`：已落地的提炼进 `architecture/`、`api/`、`rules/`；未落地的转为 `open-issues.md`。

### 10.4 `frontend/apps/admin/docs/`

建议迁入：

```text
docs/handbook/frontend/admin/
```

处理原则：

- 后台架构与会话设计：提炼为 `foundation.md`、`auth-session.md`。
- 后台文章相关：提炼为 `article-management.md`。
- 仍在推进的计划：放 `working/plans/`。
- 应用代码目录下可保留一个很短的 `README.md`，指向 `docs/handbook/frontend/admin/`。

### 10.5 `docs/repository-governance/`

建议迁入：

```text
docs/governance/
```

内容包括：

- 分支策略。
- 仓库重组计划。
- v2 main 准备验证。

## 11. 内容校准规则

整理时每个权威文档都要做一次校准，不能只移动路径。

### 11.1 校准代码现状

每个主题文档要能回答：

- 当前代码是否已经实现？
- 实现路径在哪里？
- 测试是否存在？
- 文档是否还描述旧行为？
- 是否存在与 API 契约不一致的字段、状态码或错误码？

### 11.2 状态标记规则

功能状态统一使用：

| 状态 | 含义 |
|------|------|
| 已完成 | 代码已实现，有基本验证，文档已校准 |
| 进行中 | 正在开发，代码和文档可能变化 |
| 未开始 | 已纳入范围，但尚未实现 |
| 暂缓 | 暂不做，但保留后续可能 |
| 有争议 | 方案未定，必须登记到 open-issues |
| 已废弃 | 不再做，保留原因 |

### 11.3 完成事项处理

已完成事项不应继续主要停留在计划文档中。处理方式：

1. 在 `current-status.md` 标记完成。
2. 在对应 `architecture/`、`api/`、`frontend/` 或 `ops/` 文档沉淀当前行为。
3. 原计划移动到 `archive/`。

### 11.4 未完成和争议项处理

未完成或有争议的事项统一进入 `open-issues.md`，其他文档只引用 issue 编号。

示例：

```md
PASSWORD 文章完整解锁流程见 O-001，不在本文档重复展开。
```

## 12. API 文档校准重点

`handbook/api/` 是前后端联调的重点，需要逐个核对：

1. 请求路径是否真实存在。
2. HTTP method 是否和 SecurityConfig 白名单一致。
3. 请求字段是否和 Controller request model 一致。
4. 响应字段是否和 VO/DTO 一致。
5. ID 是否统一按 string 返回给前端。
6. 时间格式是否统一。
7. 分页参数和分页响应是否统一。
8. 枚举值是否和代码一致。
9. 错误码是否真实存在于后端错误码定义。
10. ADMIN / DEMO / GUEST 权限边界是否清楚。
11. 废弃接口是否已经移除或标记。

## 13. 前端文档校准重点

### 13.1 前台 blog

需要统一记录：

- 已完成：站点配置、公开文章列表、文章详情、语言路由、错误态。
- 待完成：分类、标签、归档、友链、关于、搜索、评论、留言、统计、PASSWORD 解锁、Spotify Embed。
- 哪些配置仍来自 frontend defaults，哪些来自后端 site-config。
- 路由规则和 ID/slug 策略。
- Markdown 渲染和安全策略。

### 13.2 后台 admin

需要统一记录：

- 已完成：登录、会话刷新、静态权限、三语、仪表盘基础、文章列表。
- 待完成：文章详情/编辑、Vditor、附件上传、分类标签、评论、友链、站点配置、统计仪表盘。
- token 存储现状和后续安全升级候选。
- DEMO 只读交互与后端 403 边界。
- 前端请求重试和 refresh 单飞机制。

## 14. 运维文档校准重点

`handbook/ops/` 建议收口这些内容：

1. 本地启动步骤。
2. local/test/prod profile 区别。
3. 环境变量清单。
4. MySQL 初始化和 Flyway 行为。
5. Maven / pnpm / Node 版本。
6. 后端测试命令。
7. 前台和后台质量检查命令。
8. CORS 与 trusted proxies 配置。
9. 生产 HTTPS 和反向代理注意事项。
10. 发布检查清单。
11. 备份策略。
12. CI/CD 待办。

## 15. 分阶段执行计划

### P0：冻结整理规则

目标：先确定目录结构和命名规则，避免边整理边改方向。

产物：

- 新目录结构确认。
- 文件命名规则确认。
- 文档状态规则确认。
- 权威源划分确认。

建议提交：

```text
docs: 确定文档整理规则和目标结构
```

### P1：建立新入口和维护规则

目标：先搭好新文档骨架，不大规模搬内容。

产物：

- `docs/README.md`
- `docs/handbook/README.md`
- `docs/handbook/rules/documentation.md`
- `docs/handbook/start-here/glossary.md`
- `docs/handbook/start-here/open-issues.md`

验证：

- 从 `docs/README.md` 能找到所有主要入口。
- `open-issues.md` 至少登记首批未完成和争议项。

建议提交：

```text
docs: 建立文档入口和维护规则
```

### P2：迁移权威手册主体

目标：把 `project-handbook` 中当前有效内容迁移到 `handbook`。

范围：

- start-here
- architecture
- rules
- adr
- api
- product
- ops
- workflows

处理：

- 移动后同步改内部链接。
- 对明显过期的描述直接修正。
- 不能确认的标记为“待校准”。

验证：

- 旧 `project-handbook` 不再作为入口。
- 新 `handbook` 内部链接可读。
- `current-status.md` 与当前代码大体一致。

建议提交：

```text
docs: 迁移当前权威开发手册
```

### P3：收口前后台文档

目标：把前台和后台相关文档统一到 `handbook/frontend/`。

范围：

- `docs/project-handbook/frontend-user/`
- `frontend/apps/admin/docs/`
- 相关 superpowers specs/plans/reviews 中仍有效的结论

产物：

- `handbook/frontend/blog/README.md`
- `handbook/frontend/blog/integration-status.md`
- `handbook/frontend/admin/README.md`
- `handbook/frontend/admin/auth-session.md`
- `handbook/frontend/admin/article-management.md`
- `handbook/frontend/admin/integration-status.md`

验证：

- 前后台当前完成度能从 integration-status 找到。
- 后台应用目录内不再沉淀长期文档，只保留指向 docs 的说明。

建议提交：

```text
docs: 收口前后台开发文档
```

### P4：归档过程材料

目标：降低历史计划和 review 对当前开发的干扰。

范围：

- `docs/superpowers/`
- `docs/project-handbook/plans/`
- `docs/project-handbook/specs/`
- 已完成的迁移过程文档

处理：

- 已完成计划归档。
- 仍有价值结论提炼到 handbook。
- 未完成事项转 open-issues。
- 历史文档头部加“归档说明”。

验证：

- `docs/working/` 只保留近期仍有用的过程材料。
- `docs/archive/` 入口说明清楚“不作为当前依据”。

建议提交：

```text
docs: 归档历史计划和阶段材料
```

### P5：校准 API、状态和运维文档

目标：让最容易影响开发的文档与代码对齐。

范围：

- `handbook/api/`
- `handbook/start-here/current-status.md`
- `handbook/start-here/roadmap.md`
- `handbook/ops/environment.md`
- `handbook/ops/build-and-test.md`
- `handbook/ops/release-checklist.md`

验证：

- API 契约与 Controller/VO/错误码一致。
- 当前完成、未完成、有争议事项不互相混杂。
- 本地开发和发布检查有唯一权威入口。

建议提交：

```text
docs: 校准接口状态和运维文档
```

### P6：清理旧入口和链接

目标：处理重定向、旧路径说明和失效链接。

处理：

- 旧目录入口保留短说明或移动到 archive。
- 更新根 `AGENTS.md` 中的文档路径。
- 更新 README 中指向旧 docs 的链接。
- 全仓搜索旧路径，逐步替换。

验证：

- 新入口路径清楚。
- 旧入口不会误导人继续看过期资料。

建议提交：

```text
docs: 更新旧文档入口和引用路径
```

## 16. 建议首批 open issues

```md
# 未完成和争议项

> 状态：当前有效
> 最后校准：2026-06-29
> 权威程度：未完成事项权威登记表

## O-001 PASSWORD 文章完整解锁流程

- 状态：未完成
- 优先级：P1
- 影响范围：后端 content / 前台 blog / 评论
- 当前现状：首版已有 PASSWORD 锁定态，完整 unlock 流程仍未作为前台主链路完成。
- 争议点：Article Access Token 的前端保存位置、评论是否复用文章 token。
- 下一步：补 API 契约、后端接口、前台解锁状态和测试。

## O-002 DEMO 敏感字段裁剪边界

- 状态：有争议
- 优先级：P1
- 影响范围：后台 admin / 后端 application 层
- 当前现状：DEMO 读权限和写权限已有基础规则，但具体字段裁剪需要逐页确认。
- 下一步：按后台页面列字段清单，明确 ADMIN 与 DEMO 差异。

## O-003 后台文章编辑和附件上传闭环

- 状态：未完成
- 优先级：P0
- 影响范围：后台 admin / 后端 content / system attachment
- 当前现状：后台文章列表已完成，详情和编辑仍是后续主线。
- 下一步：优先完成文章详情、编辑、Vditor、附件上传和 DEMO 禁用交互。

## O-004 前台读者主链路补齐

- 状态：未完成
- 优先级：P0
- 影响范围：前台 blog
- 当前现状：首页、文章列表、详情已接入；分类、标签、归档、友链、关于、搜索仍待补齐。
- 下一步：按页面垂直切片推进。

## O-005 上线前 SEO 与发布准备

- 状态：未完成
- 优先级：P1
- 影响范围：前台 blog / ops
- 当前现状：RSS、Sitemap、meta、部署文档、备份、CI/CD 仍需补齐。
- 下一步：先建立 release checklist，再逐项实现。

## O-006 后台 token 存储方式升级

- 状态：暂缓 / 有争议
- 优先级：P2
- 影响范围：后台 admin / 后端 auth
- 当前现状：后台 token 存在 localStorage，个人项目可接受，但 XSS 风险较高。
- 下一步：评估是否升级 refresh token 到 HttpOnly Cookie，access token 改为内存保存。
```

## 17. 文档盘点表模板

正式整理前建议先建盘点表：

```md
# 文档盘点表

| 当前路径 | 类型 | 当前状态 | 建议动作 | 目标路径 | 备注 |
|----------|------|----------|----------|----------|------|
| docs/project-handbook/status.md | 当前进度 | 当前有效但需校准 | 改写并迁移 | docs/handbook/start-here/current-status.md | 作为状态权威源 |
| docs/superpowers/plans/... | 计划 | 历史过程 | 归档 | docs/archive/superpowers/plans/... | 完成事项提炼 |
```

建议动作只使用以下值：

- 保留
- 改写
- 合并
- 迁移
- 归档
- 废弃
- 待确认

## 18. 验收标准

本次整理完成后，应满足以下标准：

1. 新人或 AI 从 `docs/README.md` 进入，5 分钟内能知道该看哪些文档。
2. 当前进度只需要看 `current-status.md`。
3. 未完成和争议项只需要看 `open-issues.md`。
4. 接口契约只需要看 `handbook/api/`。
5. 环境变量只需要看 `handbook/ops/environment.md`。
6. 历史计划不会被误认为当前实现依据。
7. 主要开发文档统一中文。
8. 文件命名基本统一为英文小写短横线。
9. 已完成事项在状态文档中有明确标记。
10. 旧入口和旧链接不会继续误导。
11. `AGENTS.md`、README 和文档索引中的路径已更新。
12. 整理过程拆成小提交，方便回滚。

## 19. 风险与处理

| 风险 | 处理 |
|------|------|
| 一次性移动太多导致链接大面积失效 | 分阶段迁移，每阶段更新入口和链接 |
| 历史文档仍被误读为当前规则 | archive 入口和文件头明确“不作为当前依据” |
| 权威文档内容过大 | 单文件超过 10KB 时拆分 |
| API 文档校准工作量大 | 先校准认证、文章、评论、附件等高频接口 |
| 不确定某设计是否仍有效 | 标记“待校准”，登记到 open-issues |
| 前后台文档和代码一起变动 | 文档重组单独提交，不混业务代码 |

## 20. 推荐执行顺序

建议按这个顺序执行：

1. 先确认本计划。
2. 建文档盘点表。
3. 建新入口、新规则、新 open issues。
4. 迁移并改写 `project-handbook` 当前有效内容。
5. 收口前后台文档。
6. 归档 `superpowers` 和历史计划。
7. 校准 API、状态、环境变量和发布检查。
8. 更新旧链接和 AI 入口。
9. 最后做一次全局路径搜索和人工抽查。

## 21. 我的建议

这次整理的核心不是“目录更漂亮”，而是建立一个长期规则：

- 当前事实只放在 `handbook/`。
- 临时过程只放在 `working/`。
- 历史资料只放在 `archive/`。
- 未完成和争议项统一登记。
- 完成功能必须回写状态和权威文档。

这样后续开发 V2 前台和后台时，文档会真正帮忙，而不是增加判断成本。
