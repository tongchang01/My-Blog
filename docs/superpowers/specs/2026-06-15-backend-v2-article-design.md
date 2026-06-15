# Backend V2 文章核心纵向切片设计

> **状态：待实施**

## 1. 目标

在 `content` 模块按冻结的新 schema 建立文章核心纵向切片，覆盖：

- `t_article` 与 `t_article_tag` 的领域模型和持久化。
- ADMIN、DEMO 后台文章分页、详情和筛选。
- ADMIN 创建、完整编辑、软删除和恢复文章。
- 文章五态状态机与字段约束。
- 访客文章分页、详情、分类、标签、归档月份和关键字筛选。
- `PUBLISHED` 与 `PASSWORD` 的公开列表可见性差异。
- 按 Asia/Tokyo 执行的定时发布任务。
- Security、OpenAPI、接口文档、集成测试和并发测试。

本轮不实现：

- PASSWORD 文章密码校验、article access token、失败限流和解锁后正文读取。
- PASSWORD 文章评论读取和评论提交授权。
- 评论业务、评论数增减、统计聚合、RSS、Sitemap 和 Markdown 导出。
- 文章批量导入、批量删除、物理删除和 slug 历史。
- 置顶、推荐和相关文章推荐。冻结的 `t_article` 没有对应字段。
- 逐篇关闭评论。冻结的 `t_article` 没有 `comment_enabled` 字段。

## 2. 方案选择

采用“完整核心纵向切片”方案：

1. 在同一设计中完成文章领域模型、后台管理、公开读取、软删除恢复和定时发布。
2. PASSWORD 文章可以创建并在公开列表展示锁定元数据，但正文解锁作为独立安全切片实施。
3. 所有文章状态和公开可见性共用一套领域及查询规则，不拆成相互重复的后台版和公开版。

不采用以下方案：

- 先只做后台 CRUD：会让公开可见性和状态规则在后续重复实现。
- 一次完成 PASSWORD 解锁和评论授权：会同时跨越 content、identity、comment 和限流能力，单轮范围过大。

## 3. 领域模型

### 3.1 Article

`Article` 是聚合根，核心属性为：

- `id`
- `titleZh / titleJa / titleEn`
- `summaryZh / summaryJa / summaryEn`
- `body`
- `categoryId`
- `authorId`
- `slug`
- `status`
- `accessPassword`
- `publishAt`
- `coverAttachmentId`
- `commentCount`
- `tagIds`
- 创建、更新和软删除审计信息

领域对象不依赖 Spring、MyBatis、Servlet 或 HTTP 类型。

### 3.2 ArticleStatus

状态固定为：

| 状态 | 公开列表 | 公开正文 | 分类要求 | 密码要求 | 发布时间要求 |
|---|---:|---:|---:|---:|---:|
| DRAFT | 否 | 否 | 可空 | 必须为空 | 可空 |
| PUBLISHED | 是 | 是 | 必填 | 必须为空 | 必填 |
| PRIVATE | 否 | 否 | 必填 | 必须为空 | 可空 |
| PASSWORD | 是，仅元数据 | 否，本轮固定拒绝 | 必填 | 必填 | 必填 |
| SCHEDULED | 否 | 否 | 必填 | 必须为空 | 必填且创建或编辑时不得早于当前 JST 时间 |

### 3.3 内容完整性

- DRAFT 允许中文标题、正文和分类暂未完成。
- PUBLISHED、PRIVATE、PASSWORD、SCHEDULED 的 `titleZh` 和 `body` 必填。
- `titleJa/titleEn/summaryJa/summaryEn` 可空，公开读取时 fallback 到中文。
- `titleZh` 最长 255，摘要最长 500。
- 正文保存中文 Markdown，不生成或持久化 HTML。
- `commentCount` 创建时固定为 0，本轮写接口不允许客户端修改。
- 标签数量为 `0..20`，输入 ID 必须为正数且不得重复。

### 3.4 slug

文章 slug 只增强 URL 可读性，不承担唯一身份：

1. 可空、可修改、不校验唯一。
2. 非空时 trim 并转为小写。
3. 长度为 1 到 160。
4. 只允许 `a-z`、`0-9` 和单个连字符。
5. 连字符不得位于首尾，不得连续出现。
6. 不维护 slug 历史表和跨表 slug 注册表。

公开响应返回当前 slug 和 canonical path 所需数据。后端始终按文章 ID 查询；前端路由负责将缺失或不匹配的 slug 导向当前 canonical URL。

## 4. 状态转换

### 4.1 创建

- 新建文章可以直接进入五种状态之一，但必须满足目标状态约束。
- PUBLISHED 未传 `publishAt` 时，应用层写入当前 JST 时间。
- PASSWORD 未传 `publishAt` 时，应用层写入当前 JST 时间。
- PASSWORD 接收明文密码，仅在应用层短暂存在，使用 BCrypt 哈希后持久化。
- SCHEDULED 必须显式提交 `publishAt`。

### 4.2 编辑

- PUT 使用完整字段覆盖，不提供通用 PATCH。
- 首次从非公开状态进入 PUBLISHED 或 PASSWORD 且没有历史 `publishAt` 时，写入当前 JST 时间。
- 已经存在的首次公开时间不得因普通编辑被覆盖。
- 从 PASSWORD 切换到其它状态时必须清除密码哈希。
- 编辑 PASSWORD 时，密码字段为 `null` 表示保留已有哈希；非空表示重新哈希并替换。
- 非 PASSWORD 请求中的密码字段必须为 `null`。
- SCHEDULED 转为其它状态后保留 `publishAt`，作为原计划时间或后续归档依据。

### 4.3 定时发布

- 调度器按 Asia/Tokyo 获取当前时间。
- 查询 `deleted=0 AND status=SCHEDULED AND publish_at<=now` 的文章。
- 每批限制固定数量并按 `publish_at ASC, id ASC` 锁定，避免长事务。
- 到期文章转为 PUBLISHED，保留原 `publishAt`。
- 更新 `updatedAt`，`updatedBy` 写 `null`，表示系统任务。
- 不提供人工触发定时发布的 HTTP 接口。

## 5. 引用完整性

数据库不建立外键，应用层维护引用：

- 非 DRAFT 状态必须引用 active 分类。
- DRAFT 提交了分类时，该分类也必须 active。
- 所有标签必须 active。
- 封面附件必须 active、类型为图片，并通过 system application 公开能力锁定和校验。
- `authorId` 来自当前 ADMIN principal，不接受客户端提交。

创建和编辑时：

1. 锁定文章目标行，创建时跳过此步骤。
2. 按 ID 升序锁定 active 分类和标签。
3. 调用 system application 校验封面附件。
4. 校验目标状态和字段组合。
5. 保存文章。
6. 删除当前文章的全部标签关联，再批量写入新关联。

文章与标签关联必须在同一事务内全量替换。生产 SQL 全部位于 Mapper XML。

content 不直接查询 `t_attachment`。system application 需要提供：

- 按附件 ID 锁定并校验 active 图片的写入能力。
- 按一组附件 ID 批量返回公开 URL 的读取能力。

公开和后台文章查询先读取 content 投影，再批量解析封面，禁止逐条调用形成 N+1 查询。

## 6. 软删除与恢复

### 6.1 删除

删除前锁定 active 文章，显式写入：

- `deleted=1`
- `deletedAt`
- `deletedBy`
- `updatedAt`
- `updatedBy`

删除文章不删除 `t_article_tag` 关联，不修改分类、标签、附件或评论。保留关联用于回收站展示和恢复。

### 6.2 恢复

恢复前锁定已删除文章，并重新校验：

- 分类仍然 active。
- 标签仍然 active。
- 封面附件仍然 active 且仍是图片。
- 当前字段仍满足文章状态约束。

任一引用失效时返回冲突，不自动清空分类、标签或封面。恢复成功时：

- `deleted=0`
- `deletedAt=null`
- `deletedBy=null`
- 更新 `updatedAt/updatedBy`

回收站只提供查看和恢复，不提供后台物理删除。

## 7. 公开 API

### 7.1 文章列表

```http
GET /api/public/articles
```

参数：

| 参数 | 必填 | 规则 |
|---|---:|---|
| `lang` | 是 | `zh / ja / en` |
| `page` | 否 | 默认 1，最小 1 |
| `size` | 否 | 默认 10，范围 1..50 |
| `categoryId` | 否 | 正整数 |
| `tagId` | 否 | 正整数 |
| `archiveMonth` | 否 | `yyyy-MM` |
| `keyword` | 否 | trim 后 1..100 |

规则：

- 只返回 active 的 PUBLISHED 和 PASSWORD。
- 按 `publishAt DESC, id DESC` 稳定排序。
- 分类、标签和归档月份筛选可组合。
- keyword 搜索标题和摘要；每个语言字段使用“目标语言值为空则回退中文”的表达式。
- PASSWORD 返回 `locked=true`，不返回正文、密码哈希或评论内容。
- PUBLISHED 返回 `locked=false`。

列表项包含：

- id、当前语言标题、当前语言摘要
- slug、status、locked
- 分类、标签
- coverAttachmentId 和公开封面 URL
- publishAt、commentCount

### 7.2 文章详情

```http
GET /api/public/articles/{id}?lang=zh
```

- active PUBLISHED 返回正文和完整公开元数据。
- active PASSWORD 固定返回 `403`，等待后续解锁切片。
- DRAFT、PRIVATE、SCHEDULED、已删除和不存在文章统一返回 `404`。
- 不返回密码哈希、authorId 和审计字段。

## 8. 后台 API

```http
GET    /api/admin/articles
GET    /api/admin/articles/{id}
POST   /api/admin/articles
PUT    /api/admin/articles/{id}
DELETE /api/admin/articles/{id}

GET    /api/admin/articles/recycle-bin
POST   /api/admin/articles/{id}/restore
```

权限：

| 操作 | ADMIN | DEMO |
|---|---:|---:|
| 后台列表、详情、回收站 | 允许 | 允许 |
| 创建、编辑、删除、恢复 | 允许 | 禁止 |

后台列表支持：

- page、size
- status
- categoryId、tagId
- titleKeyword
- createdAt 或 publishAt 时间范围

后台详情返回完整三语字段、正文、状态、分类、标签、封面、发布时间、评论数和非敏感审计信息，但永不返回 `accessPassword` 哈希。

## 9. 写入契约

POST 和 PUT 使用相同的完整业务字段：

```json
{
  "titleZh": "Spring Security JWT",
  "titleJa": null,
  "titleEn": "Spring Security JWT",
  "summaryZh": "文章摘要",
  "summaryJa": null,
  "summaryEn": null,
  "body": "# 正文",
  "categoryId": 101,
  "tagIds": [201, 202],
  "slug": "spring-security-jwt",
  "status": "PUBLISHED",
  "password": null,
  "publishAt": null,
  "coverAttachmentId": 301
}
```

- 所有字段必须出现在 JSON 中，允许为空的字段必须显式传 `null`。
- 未知字段、字段缺失和非法组合返回 `400 + 90001`。
- 客户端不得提交 authorId、commentCount 或审计字段。
- password 是只写字段，不进入 Result、VO、日志或 OpenAPI 响应 schema。
- 创建 PASSWORD 时 password 不得为 `null`；编辑已有 PASSWORD 时 `null` 才表示保留原密码。

## 10. 分层与持久化

```text
content/
├── web/
│   ├── PublicArticleController
│   ├── AdminArticleController
│   └── Request / VO / Mapping
├── application/
│   └── article/
├── domain/
│   └── article/
└── infrastructure/
    └── persistence/
        ├── entity/
        ├── mapper/
        ├── mapping/
        └── repository/
```

- web 只依赖 application。
- application 负责权限、事务、审计、引用校验和异常翻译。
- domain 负责字段规范化、状态约束和状态转换。
- infrastructure 负责 Entity、Mapper XML、Repository 和查询投影。
- application/domain 不依赖 Entity、Mapper 或 web request。
- MapStruct 只处理无业务判断的机械映射。
- 多语言 fallback、状态可见性、密码处理和引用校验不得隐藏在 MapStruct 中。
- 不修改冻结的 `V1__init.sql`，不新增数据库外键。
- content 不联表读取 system 表，跨模块数据只通过 application 公开能力访问。

复杂公开分页允许使用专用查询投影，不强制先组装完整 Article 聚合。写入和状态转换必须通过 Article 聚合规则。

## 11. 并发与事务

- 编辑、删除和恢复前使用 `SELECT ... FOR UPDATE` 锁定目标文章。
- 分类和标签按 ID 升序锁定，避免不同请求以不同顺序拿锁。
- 编辑文章与替换标签关联在同一事务内完成。
- 软删除和恢复使用带当前 deleted 状态的条件更新，更新行数异常视为并发冲突。
- 定时发布按固定批次锁定，多个实例并发执行时同一文章最多发布一次。
- 所有业务时间来自注入的 `Clock`，应用层和领域层不直接读取系统时间。

## 12. Security

新增匿名白名单：

```yaml
- method: GET
  path: /api/public/articles
- method: GET
  path: /api/public/articles/*
```

在通用 `/api/admin/**` ADMIN 规则前，为文章后台 GET 路径开放 ADMIN、DEMO。应用服务继续执行角色校验，避免绕开 Web Security 后丢失业务边界。

PASSWORD 文章详情当前返回业务 403，不签发或校验 article access token。

## 13. 错误处理

| 场景 | HTTP | code |
|---|---:|---:|
| 字段、分页、月份、slug 或状态组合非法 | 400 | `90001` |
| 后台 token 缺失或失效 | 401 | `10002` |
| DEMO 写操作 | 403 | `10003` |
| PASSWORD 正文未解锁 | 403 | `10003` |
| 文章或 active 引用目标不存在 | 404 | `90003` |
| 恢复引用失效、条件更新失败或并发状态冲突 | 409 | `90004` |
| 未知持久化失败 | 500 | `99999` |

后端错误消息使用稳定中文兜底，不返回 SQL、索引名、密码哈希或内部异常。

## 14. 测试策略

### 14.1 领域测试

- 五态字段约束。
- 中文标题、正文、摘要和 slug 边界。
- PASSWORD 密码必填、保留、替换和离开状态清除。
- PUBLISHED/PASSWORD 首次公开时间。
- SCHEDULED 时间边界和到期发布。
- 多语言 fallback。

### 14.2 Repository 测试

- Article 与 ArticleEntity 映射。
- ASSIGN_ID、审计字段和显式软删除。
- 文章详情、后台分页、公开分页和筛选。
- PUBLISHED/PASSWORD 列表可见性及正文隔离。
- 标签关联全量替换和保留删除文章关联。
- 回收站和恢复条件更新。
- 所有生产 SQL 位于 Mapper XML。

### 14.3 Application 测试

- ADMIN、DEMO 权限。
- 分类、标签和附件引用校验。
- 创建、完整编辑、软删除和恢复事务。
- PASSWORD 明文不进入结果和日志。
- 定时发布批处理。
- 并发编辑、删除、恢复和调度竞争。

### 14.4 Web、Security 与 OpenAPI

- 匿名公开列表和 PUBLISHED 详情。
- PASSWORD 列表锁标识和详情 403。
- 非公开状态统一 404。
- 后台 ADMIN/DEMO 读权限和 DEMO 写拒绝。
- 缺失字段、未知字段及非法筛选 400。
- OpenAPI 不暴露 Entity、Mapper、密码哈希或删除审计字段。

### 14.5 全量验证

- `mvn clean test`
- Maven Enforcer dependency convergence
- ArchUnit
- `git diff --check`
- content 模块无注解 SQL
- content 模块无 `deleteById/removeById`
- application/domain 无 Entity、Mapper 和 web 依赖
- 冻结的 V1 DDL 未修改

Docker 不作为本轮通过前提；仅允许既有 Testcontainers MySQL 条件测试在 Docker 不可用时 skipped。

## 15. 提交拆分

实施阶段保持五个中文本地提交：

1. `建立文章领域模型与持久化基础`
   - Article 聚合、五态、Entity、Mapper XML、Repository 和标签关联。
2. `实现后台文章查询与编辑`
   - 后台分页、详情、创建、完整编辑、引用校验和密码哈希。
3. `实现公开文章查询与筛选`
   - 多语言列表、详情、分类标签归档筛选、关键字搜索和 PASSWORD 元数据。
4. `实现文章定时发布与回收站`
   - 调度器、显式软删除、回收站、恢复和并发测试。
5. `完成文章核心纵向切片`
   - 真实 HTTP 集成、Security、OpenAPI、接口文档、状态文档和全量验证。

每个提交只包含对应范围，执行 RED → GREEN → 定向回归 → 静态检查后再提交。

## 16. 验收标准

- 文章五态和字段组合由统一领域规则约束。
- ADMIN 可创建、编辑、删除和恢复文章；DEMO 只能读。
- 分类、标签和封面逻辑引用在应用层校验，无数据库外键。
- PUBLISHED 和 PASSWORD 可进入公开列表，只有 PUBLISHED 在本轮返回正文。
- 公开列表支持分类、标签、归档月份和关键字组合筛选。
- PASSWORD 明文只用于 BCrypt 校验或生成哈希，哈希不通过 API 返回。
- 定时任务按 JST 将到期 SCHEDULED 转为 PUBLISHED，并保留计划发布时间。
- 文章软删除保留标签关联；恢复前重新校验所有引用。
- 不增加 `comment_enabled`、置顶或推荐字段，不修改冻结 DDL。
- 所有生产 SQL 位于 Mapper XML，并包含必要中文业务注释。
- 五个实施批次分别形成中文本地提交。
- Maven、Enforcer、ArchUnit、OpenAPI 契约和静态检查全部通过。
