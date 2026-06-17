# 当前进度

> 本文档回答："V2 现在做到哪了？接下来按什么主线推进？"
> 更新时机：每个里程碑完成后更新。
> 当前日期：2026-06

## ⚠️ 当前唯一主线：M3 模块重建

**DDL 冻结前的产品规格与 schema 评审已完成**。R5-R7 完成后多处与既有代码冲突（ADR-0015 审计列三件套、ADR-0017 无 FK、R5 模块边界含 stats 与 common-infra、ADR-0018 时区五层、Role 三态枚举等），继续修旧实现 = 返工。

**2026-06-17 更新：comment 评论与留言纵向切片已完成。** 公开文章评论、留言板、后台审核、Markdown 安全清洗、文章评论计数、事务后回复通知、Resend 适配和失败日志均已落地；PASSWORD 文章评论在解锁能力完成前保持 `403`。

**从现在开始，不再修改 Flyway V1__init.sql；后续 schema 变更走 V2__xxx.sql / V3__xxx.sql。**

M1 旧代码清理、M2 基础设施补齐和 M3 准入复核已经完成，identity 与 system 的首个纵向切片已落地。代码阶段的推进原则：

- 保留 `common/`、Security、ArchUnit 等横切能力
- 先补齐审计、时间、i18n、配置与新模块架构守护
- 再按冻结后的 14 张表 schema 重建业务模块

允许做的：V2 代码清理 / 基础设施补齐 / 按新 schema 重建业务模块 / 配套测试。

## 1. 文档体系状态

| 类别                                                                                                       | 状态                                                           |
| -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| 产品决策 R1-R7（`product/decisions-draft.md`）                                                                 | ✅ 完成                                                         |
| V1 功能清单（`product/feature-inventory.md`）                                                                  | ✅ ⑳ 项全部回填决策                                                  |
| ADR 0001-0018                                                                                            | ✅ 0014 部分被 0015 / 0018 超越；0015-0018 为 R5-R7 衍生               |
| 红线（`pitfalls.md`）                                                                                        | ✅ R-001 ~ R-013                                              |
| Rules（`api-response.md` / `error-handling.md` / `security-baseline.md` 等）                                | ✅ 按 R5-R7 重写完毕                                               |
| `arch/auth-flow.md`                                                                                      | ✅ 双 token 完整流程                                               |
| `arch/schema-design.md`                                                                                  | ✅ DDL 已冻结；Flyway V1__init.sql 已生成并烟测通过，后续 schema 变更另起 V2+ 迁移 |
| `product/use-cases.md` / `product/data-model.md` / `product/business-rules.md` / `product/er-diagram.md` | ✅ 已生成                                                        |
| M3 开始前全量审查                                                                                           | ✅ P1 与准入文档项已关闭；剩余 P2 按对应业务首次落地前处理                    |

## 2. V2 后端代码现状

详见 `migration/v2-code-reconciliation.md`。摘要：

- 包结构 `com.tyb.myblog.v2` 符合 ADR-0002
- 已保留：`common/`（错误码 / 响应包装 / Security 链路 / Exception handler）、ArchUnit、Spring Security、Flyway V1__init.sql
- 已删除：identity / content / comment 的旧 domain / application / web / infrastructure、旧 Mapper XML、业务测试、CategoryEntity / TagEntity、动态菜单与配置式账号实现
- 已删除：`hutool-all`；公开接口白名单已移除不存在的旧业务路径
- 已完成：BaseEntity / AuditOnlyBase / AuditFieldHandler / Clock Bean；API 错误消息收口到 ApiErrorCode 中文兜底
- 已完成：Jackson 固定 Asia/Tokyo 与 ISO-8601 输出；MySQL 连接强制 session 使用 Asia/Tokyo；Knife4j 4.x 仅在 `local` / `test` 开启
- 已完成：取消默认 local profile；local / prod 必须显式激活，数据库与 JWT 密钥缺失时安全失败
- 已完成：客户端 IP 默认使用连接远端地址，仅对显式可信代理解析转发头
- 已完成：Security 探针隔离到 `local/test` profile，不进入生产扫描
- 已完成：认证 token 端口收口到 `common.auth.token`，JWT 实现与 identity 用例解除具体依赖
- 已完成：ArchUnit 按 identity / content / comment / system / stats / common-infra 重写，跨模块只允许依赖对方 application 接口
- 已完成：Maven Enforcer 锁定 Java 17 / Maven 3.9.x，并执行依赖收敛检查
- M3 已进入业务模块重建；identity 认证会话纵向切片已提供真实 login / refresh / logout 接口
- 登录链路已接入用户名规范化、可信客户端 IP、单实例 Caffeine 前置限流、BCrypt、数据库失败累计与锁定、成功审计、refresh token 哈希持久化和 JWT access token 签发
- 成功分支采用独立短事务；access token 签发失败时，成功审计与 refresh token 写入一起回滚
- ADMIN / DEMO 可登录；未知账号、GUEST、密码错误和持久化锁定统一返回 `401 + 10001`；第 6 次连续失败返回 `429 + 90002`
- 已开放 `POST /api/auth/refresh`：JSON 传入 refresh token，行锁保证旧 token 最多消费一次，账号状态和 token version 每次重新读取
- refresh 轮换采用独立事务；access token 签发失败时旧 token 撤销和新 token 写入整体回滚
- 已开放 `POST /api/auth/logout`：必须携带有效 Bearer access token，递增当前账号 token version 并撤销全部 refresh token
- 已落地 `t_user_info` 领域、XML Mapper、仓储和 V2 历史资料补齐迁移；账号与资料保持 1:1
- 已开放 `GET /api/auth/me`：ADMIN / DEMO 可查询自己的账号基础信息和完整资料
- 已开放 `PATCH /api/auth/me/profile`：仅 ADMIN 可按字段 presence 部分更新；支持显式清空可选字段，并通过行锁防止并发 PATCH 丢更新
- 当前用户资料 OpenAPI 使用独立文档模型，避免把内部 `PatchValue` 暴露给客户端；Knife4j 4.5.0 通过兼容适配器支持 springdoc 2.8.8
- 已开放 `PUT /api/auth/me/password`：仅 ADMIN 可修改本人密码；账号行锁、BCrypt 校验、密码与 token version 原子更新、refresh token 全撤销处于同一事务
- 改密成功后不签发新 token，当前账号历史 access / refresh token 全部失效；事务回滚、账号隔离和 H2 并发改密已验收，MySQL 并发测试在 Docker 可用时执行
- identity 后端模块已完成
- system 模块已建立四层结构，`t_site_config` 纵向切片已完成：匿名三语公开读取、ADMIN / DEMO 后台完整读取、ADMIN 全量更新
- 站点配置 PUT 要求 13 个业务字段全部出现；字段规范化、固定行异常、XML 完整更新、行锁串行和 OpenAPI presence 隔离均已验收
- `t_attachment` 纵向切片已完成：ADMIN 上传，ADMIN / DEMO 分页和详情查询，响应使用 `records/total/page/size`
- 附件只接受 JPEG、PNG、WebP、GIF，最大 10 MiB；服务端识别真实格式并限制尺寸、总像素与 GIF 帧数
- LOCAL 与 AWS S3 通过统一存储端口切换；S3 使用默认凭证链，不配置静态凭证或对象 ACL
- 相同 hash 的 active 附件直接复用，deleted 附件恢复原记录；物理对象缺失返回内部错误
- 真实 H2 / JWT / LOCAL 集成测试已覆盖上传、去重、恢复、权限、非法图片和并发收敛；并发结果为一行一个对象
- `t_friend_link` 纵向切片已完成：匿名读取 VISIBLE 友链，ADMIN / DEMO 后台只读，ADMIN 新增、完整编辑、状态、批量排序和显式软删除
- 友链 URL 允许重复；写入只接受绝对 HTTP/HTTPS 地址；批量排序 1..100 项并在目标缺失时整体回滚
- 友链更新、状态、排序和删除均使用 active 行锁；软删除完整写入删除和更新审计五字段
- content 分类标签纵向切片已完成：匿名按 `zh/ja/en` 读取并 fallback 中文，ADMIN / DEMO 后台只读，ADMIN 新增、完整编辑、分类排序和引用保护软删除
- 分类与标签 slug 在领域层规范化并永久唯一；数据库唯一索引处理并发兜底，冲突统一返回 `409 + 90004`
- 分类批量排序按升序 ID 锁定，任一目标缺失或更新异常时整体回滚；标签不提供排序接口
- active 文章引用阻止分类和标签删除；已删除文章不阻止删除，软删除完整写入五个审计字段
- MapStruct 1.6.3 已用于 persistence 与 web 的机械映射，业务规则保持显式
- 当前基线：`mvn clean test` 通过（485 tests，0 failures，0 errors，4 skipped；跳过项均为 Docker 不可用时的 Testcontainers MySQL 条件测试）
- comment 模块已完成：公开接口只返回 `contentHtml`，后台接口支持目标、状态、关键字、删除状态筛选；ADMIN 可审核/隐藏/删除/恢复，DEMO 只读；文章 `comment_count` 与审核/删除/恢复同事务维护
- 邮件 common-infra 已完成：Resend 默认关闭，开启缺少配置时启动失败；回复通知在事务提交后发送，失败写 `t_mail_log`，成功不入库

**当前处置方案**：identity、system、content 与 comment 已收尾，下一步进入 stats 纵向切片，再推进前台 / 后台前端骨架。

## 3. 下一步推进顺序（详见 `roadmap.md`）

1. 设计并实现 stats 纵向切片
2. 前台 / 后台前端骨架
3. 根据前端对接结果补充接口契约细节

## 4. 相关文档

- 路线图：`roadmap.md`
- 历史与红线：`pitfalls.md`
- V1 vs V2：`v1-vs-v2.md`
- 文档索引：`INDEX.md`
