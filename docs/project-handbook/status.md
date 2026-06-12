# 当前进度

> 本文档回答："V2 现在做到哪了？接下来按什么主线推进？"
> 更新时机：每个里程碑完成后更新。
> 当前日期：2026-06

## ⚠️ 当前唯一主线：M3 模块重建

**DDL 冻结前的产品规格与 schema 评审已完成**。R5-R7 完成后多处与既有代码冲突（ADR-0015 审计列三件套、ADR-0017 无 FK、R5 模块边界含 stats 与 common-infra、ADR-0018 时区五层、Role 三态枚举等），继续修旧实现 = 返工。

**从现在开始，不再修改 Flyway V1__init.sql；后续 schema 变更走 V2__xxx.sql / V3__xxx.sql。**

M1 旧代码清理、M2 基础设施补齐和 M3 准入复核已经完成，当前从 identity 最小纵向切片开始模块重建。代码阶段的推进原则：

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
- M3 已允许进入；下一步实现 identity 后台登录最小纵向切片
- 当前基线：`mvn clean test` 通过（108 tests，0 failures，0 errors，1 skipped）

**当前处置方案**：按 identity → system → content → comment → stats / common-infra 顺序重建。

## 3. 下一步推进顺序（详见 `roadmap.md`）

1. identity 后台登录最小纵向切片
2. 按模块重建：identity → system → content → comment → stats / common-infra
3. 前台 / 后台前端骨架

## 4. 相关文档

- 路线图：`roadmap.md`
- 历史与红线：`pitfalls.md`
- V1 vs V2：`v1-vs-v2.md`
- 文档索引：`INDEX.md`
