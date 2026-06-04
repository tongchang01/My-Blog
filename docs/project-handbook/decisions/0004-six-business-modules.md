# ADR-0004: 划分为六大业务模块

- 状态：accepted（**2026-06 R5 修订**：模块清单从 common / infrastructure / identity / content / comment / system → 改为 identity / content / comment / system / stats / common-infra）
- 日期：2026-04（2026-06 R5 修订）
- 决策者：项目负责人

## 背景

V1 表结构按功能聚集（user/article/comment/config/file 等）。V2 模块化时需决定划分粒度。过粗失去模块化意义；过细增加跨模块沟通成本。

2026-06 R5 重新梳理：引入访客统计独立模块 `stats`；`common` + 顶层 `infrastructure` 合并为 `common-infra` 统一概念，避免"是 Java package 还是 Maven module 还是顶层包"的混乱。

## 决定（2026-06 R5 修订）

划分为六大模块（按业务边界，非技术边界）：

| 模块 | 职责 | 核心表 |
|------|------|------|
| `identity` | 用户、角色、登录、JWT 签发与刷新 | `t_user_auth` / `t_user_info` / `t_refresh_token` |
| `content` | 文章、分类、标签 | `t_article` / `t_article_tag` / `t_category` / `t_tag` |
| `comment` | 评论、留言板（复用 t_comment）、审核 | `t_comment` |
| `system` | 站点配置、附件、友链申请 | `t_site_config` / `t_attachment` / `t_friend_link` |
| `stats` | 访客统计（自研日聚合） | `t_page_view` / `t_page_view_daily` |
| `common-infra` | 跨模块基础设施：响应封装、异常体系、Security 链路、MyBatis-Plus / Flyway / Knife4j 配置、Clock Bean、i18n、ArchUnit 规则 | — |

**`system` 模块职责变化**：不含 V1 的"字典 / 后台菜单 / 操作日志"。
- 字典：用 Java enum + 前端 i18n 取代（R5）
- 后台菜单：前端静态路由（⑩）
- 操作日志：走 logback 文件，不入库（⑮）

**`common-infra` 命名约定**：实际 Java 包路径为 `com.tyb.myblog.v2.common` 顶层包，`infrastructure` 是 `common` 下的子目录（不是另一个顶层模块）。文档中统一写 `common-infra` 指代这一整层。

## 理由

- 按业务边界，非技术边界
- 评论独立模块：评论场景复杂（Markdown 双存 / 审核 / Resend 邮件 / 留言板复用），独立后职责清晰
- `stats` 独立：访客统计的写入路径（每请求打点）与读取路径（日聚合 + 报表）跟其他业务表生命周期完全不同，独立模块降低耦合（其 `t_page_view` 是 ADR-0015 例外，用 AuditOnlyBase 7 列）
- `common-infra` 命名收敛：明确文档语义，避免"common / infrastructure / 顶层包"三套说法

## 后果

正面：
- 模块边界与 R5-R7 一致
- 错误码模块号有清晰映射：identity=10 / content=20 / comment=30 / system=40 / stats=50 / common-infra=90 / fallback=99（rules/error-handling.md）
- Knife4j API 分组按模块切分
- ArchUnit 守护按模块依赖方向约束

负面：
- 跨模块查询（如评论显示用户头像）需要明确通信方式（见 `rules/package-layout.md` § 跨模块通信）
- 旧 ADR / arch / overview 中"common + infrastructure"二分需要清理

后续需关注：
- DDL 冻结后按本模块清单建立 `arch/module-map.md` 详尽包路径与依赖图
- ArchUnit 规则按本清单调整

## 相关

- 相关 rules：`rules/package-layout.md` / `rules/error-handling.md`
- 相关 ADR：ADR-0001（模块化单体）/ ADR-0015（审计列例外条款涉及 stats）
- 关联决定：`product/decisions-draft.md` R5 B1
