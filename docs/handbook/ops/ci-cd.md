# CI/CD 规则

> 状态：当前有效
> 适用范围：MyBlog V2 CI/CD 边界
> 最后校准：2026-07-01
> 权威程度：运维规则

## 本文档回答什么问题

本文档说明 MyBlog V2 现阶段自动化检查做什么、不做什么，以及后续什么时候扩大范围。

## 当前阶段

当前只做 CI，不做 CD。

CD 等上线部署拓扑确定后再设计。上线前再补充部署目标、密钥管理、回滚方式、数据库迁移策略和发布审批规则；在此之前不得把生产部署密钥、服务器操作或镜像发布塞进 CI。

## CI 触发

CI 覆盖以下入口：

- 向 `main` 发起 Pull Request。
- 向 `main` 推送提交。
- 手动触发 `workflow_dispatch`。

PR 合并前必须保证 CI 通过。CI 失败时，先修复失败原因；不得为了合并而跳过检查。

## 当前最小检查

当前 CI 只覆盖已经进入主线维护面的最小稳定检查：

| 范围 | 命令 |
|------|------|
| V2 后端 | `mvn -f MyBlog-springboot-v2/pom.xml test "-Dtest=!MySqlFlywayMigrationTest,!MySqlChangePasswordConcurrencyTest,!MySqlLoginFailureConcurrencyTest"` |
| 后台前端 | `pnpm --dir frontend/apps/admin typecheck` |
| 后台前端 | `pnpm --dir frontend/apps/admin test` |

CI runner 是干净环境，因此后端 CI 使用 Maven 测试，并显式设置 `Asia/Tokyo` JVM 时区，但暂不跑 `MySql*Test` 真实 MySQL 专项。本地阶段结束或发布前仍按 [构建与测试 SOP](build-and-test.md) 执行更完整的验证。

## 暂不纳入

以下事项现阶段不进入 CI：

- CD、部署、服务器操作和生产密钥。
- Docker 镜像构建与发布。
- 覆盖率上传、制品归档和质量门禁平台。
- 前台 blog 的强制 gate。
- 前端 production build。
- 需要外部私密环境的检查。

这些不是永久不做，而是等对应模块或发布链路进入主线维护后再加。

## 扩展规则

新增 CI 检查必须满足以下条件：

1. 能在干净 runner 上稳定复现。
2. 不依赖生产密钥、个人本机配置或不可控外部服务。
3. 能覆盖已经进入主线维护的代码面。
4. 失败结果能指导修复，而不是只制造噪音。

模块进入主线开发后，应同步把它的最小稳定检查加入 CI。例如前台 blog 正式接入公开接口后，再加入对应的 typecheck、test 或 build。

## 与发布检查的关系

CI 只能证明提交级检查通过，不能替代发布前验证。发布前仍必须执行 [发布检查清单](release-checklist.md)，尤其是 CORS、反向代理路径、客户端 IP、真实 MySQL 方言、附件存储和上线冒烟。
