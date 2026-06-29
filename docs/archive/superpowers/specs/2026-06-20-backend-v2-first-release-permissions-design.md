# 后端 V2 首版权限与 PASSWORD 范围设计

## 1. 目标

冻结后端 V2 首版的 PASSWORD 文章范围与公开 DEMO 账号读取边界，消除产品规则、安全基线、API 契约和当前实现之间的冲突。

本设计保持 ADMIN 现有行为不变，并保留 DEMO 的后台只读演示价值，同时阻止公开演示账号读取未公开文章正文和评论审计敏感字段。

## 2. 首版范围裁决

### 2.1 PASSWORD 文章

PASSWORD 完整解锁链路不进入首版发布范围。

首版行为固定为：

- 公开文章列表可以返回 PASSWORD 文章的标题、摘要、锁标识和公开元数据。
- PASSWORD 文章详情不返回正文，继续返回当前约定的 `403 + 10003`。
- PASSWORD 文章评论列表和评论提交入口继续返回 `403 + 10003`。
- 首版不提供 `/api/public/articles/{id}/unlock`。
- article access token、解锁限流、正文授权和评论授权作为上线后独立增量设计与实现。

长期产品需求仍保留 PASSWORD 完整解锁能力，但所有描述必须明确区分“目标能力”和“首版已实现能力”。

## 3. DEMO 账号读取边界

DEMO 按公开演示账号处理。DEMO 可以读取后台常规列表和详情，但响应必须按角色裁剪敏感字段。

### 3.1 后台文章

| 文章状态 | ADMIN `body` | DEMO `body` |
|---|---|---|
| PUBLISHED | 完整正文 | 完整正文 |
| DRAFT | 完整正文 | `null` |
| PRIVATE | 完整正文 | `null` |
| PASSWORD | 完整正文 | `null` |
| SCHEDULED | 完整正文 | `null` |

后台文章列表当前不返回正文，因此列表结构和行为保持不变。文章详情的其他非密码字段保持现状；访问密码哈希继续对所有角色不可见。

### 3.2 后台评论

DEMO 可以继续读取评论管理列表，但以下字段固定返回 `null`：

- `authorEmail`
- `authorIp`
- `authorUserAgent`

ADMIN 继续获得完整审计字段。评论昵称、站点、Markdown 原文、清洗后 HTML、审核状态和时间字段保持现状。

### 3.3 写权限

DEMO 的所有后台写操作继续返回 403；本设计不改变现有 ADMIN/DEMO 写权限规则。

## 4. 应用边界与数据流

字段裁剪在 application 层完成，不在 Controller 或 JSON 序列化阶段临时遮盖。

```text
Controller
  -> application query service(principal, query)
      -> repository 读取完整后台数据
      -> role-aware response policy 裁剪敏感字段
  -> Web mapping
  -> JSON response
```

具体约束：

- content application 根据当前身份和文章状态决定 `AdminArticleDetailResult.body`。
- comment application 根据当前身份决定 `AdminCommentPageResult.Item` 的三个审计字段。
- Web mapping 只映射 application 已裁决的结果，不再次判断角色。
- Repository 和数据库投影保持单一查询，不为 DEMO 增加重复 SQL。
- 角色判断继续使用 `AuthenticatedPrincipal.roles()`，不引入新的权限框架或 `@PreAuthorize` 强制依赖。

## 5. 错误与兼容性

- 本设计不新增错误码。
- DEMO 访问允许的 GET 接口仍返回 200；敏感字段使用 JSON `null` 表示不可见，字段不省略。
- ADMIN 响应保持向后兼容。
- PASSWORD 公开详情和评论继续使用当前 `403 + 10003`，避免首版前端误以为解锁能力已经存在。

## 6. 文档同步

实施时必须同步以下有效文档：

- `docs/project-handbook/product/use-cases.md`：标记 PASSWORD 解锁为目标能力，并写明首版只提供锁定态。
- `docs/project-handbook/product/business-rules.md`：增加首版 PASSWORD 例外说明。
- `docs/project-handbook/product/decisions-draft.md`：记录 DEMO 公开演示时的字段裁剪裁决，并区分 PASSWORD 长期方案与首版边界。
- `docs/project-handbook/api-contract/article.md`：明确 DEMO 对非公开状态正文得到 `null`。
- `docs/project-handbook/api-contract/comment.md`：明确 DEMO 的三个审计字段为 `null`。
- `docs/project-handbook/rules/security-baseline.md`：将敏感读取规则改为字段级裁剪，不要求特定授权注解。
- `docs/project-handbook/roadmap.md`：保持 PASSWORD 完整 token 流程位于上线后增量。

## 7. 测试设计

### 7.1 content

- ADMIN 查询 DRAFT、PRIVATE、PASSWORD、SCHEDULED 详情时正文保持完整。
- DEMO 查询 PUBLISHED 详情时正文保持完整。
- DEMO 查询 DRAFT、PRIVATE、PASSWORD、SCHEDULED 详情时正文为 `null`。
- 后台文章列表结构和现有权限测试继续通过。

### 7.2 comment

- ADMIN 查询评论列表时邮箱、IP、User-Agent 保持完整。
- DEMO 查询同一评论列表时三个字段均为 `null`。
- DEMO 仍能读取其他评论管理字段。
- DEMO 写操作继续返回 403。

### 7.3 回归

- 运行 content 与 comment 定向单元/集成测试。
- 运行 `SecurityConfigTest` 和 ArchUnit。
- 阶段结束运行 fresh H2 全量测试。
- 使用本地 MySQL 广泛回归确认字段裁剪不改变持久化和查询兼容性。

## 8. 非目标

本批不实现：

- PASSWORD 解锁接口和 article access token。
- PASSWORD 解锁失败限流。
- PASSWORD 正文或评论的授权缓存。
- 独立 DEMO 数据库、租户或专用 API。
- 评论内容额外脱敏、删除记录隐藏或审计数据物理裁剪。
- Web 与 Domain 类型依赖的架构守护修复。
- 全量 OpenAPI/Javadoc 补齐。

这些事项继续按发布前审查的后续独立批次处理。
