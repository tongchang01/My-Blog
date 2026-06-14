# 当前进度

> 本文档回答："V2 现在做到哪了？接下来按什么主线推进？"
> 更新时机：每个里程碑完成后更新。
> 当前日期：2026-06

## ⚠️ 当前唯一主线：M3 模块重建

**DDL 冻结前的产品规格与 schema 评审已完成**。R5-R7 完成后多处与既有代码冲突（ADR-0015 审计列三件套、ADR-0017 无 FK、R5 模块边界含 stats 与 common-infra、ADR-0018 时区五层、Role 三态枚举等），继续修旧实现 = 返工。

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
- 当前基线：`mvn clean test` 通过（329 tests，0 failures，0 errors，4 skipped；跳过项均为 Docker 不可用时的 Testcontainers MySQL 条件测试）

**当前处置方案**：identity 已收尾，system 的 `t_site_config` 已完成，继续按 `t_attachment` → `t_friend_link` → content → comment → stats / common-infra 推进。

## 3. 下一步推进顺序（详见 `roadmap.md`）

1. 设计并实现 system 的 `t_attachment` 纵向切片
2. 继续完成 system 的 `t_friend_link`，再进入 content → comment → stats / common-infra
3. 前台 / 后台前端骨架

## 4. 相关文档

- 路线图：`roadmap.md`
- 历史与红线：`pitfalls.md`
- V1 vs V2：`v1-vs-v2.md`
- 文档索引：`INDEX.md`
