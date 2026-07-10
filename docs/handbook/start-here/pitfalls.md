# 项目踩坑记录

> 状态：当前有效
> 适用范围：MyBlog V2 开发、测试、CI 与仓库维护
> 最后校准：2026-07-11
> 对应代码：`.github/workflows/ci.yml`、`MyBlog-springboot-v2/`、`frontend/apps/`
> 权威程度：可复用工程经验

本文件保留已经在项目中实际发生、并且对后续工作仍有帮助的经验。完整过程由 Git 历史追溯；这里记录可复现现象、根因和以后应采用的处理方式。

## P-001：本机测试通过不代表 CI 环境一致

- 现象：首次运行主线 CI 时，管理端通过，后端失败；GitHub annotations 只显示进程退出码。
- 根因：本机没有 Docker 时 Testcontainers 测试会跳过，GitHub runner 会真实执行；runner 默认时区也不是 `Asia/Tokyo`，触发了启动校验。
- 处理：查看完整 job logs，不根据 annotations 猜原因。后端普通测试和真实 MySQL 专项使用独立 job，两个 job 都显式设置 JVM 与 shell 时区。
- 排查命令：`gh run list --limit 5`、`gh run watch --exit-status`、`gh run view --log-failed`。
- 相关：`../ops/ci-cd.md`、`.github/workflows/ci.yml`。

## P-002：公开内容测试不能依赖当前时间

- 现象：分类标签集成测试单独运行通过，进入后端全量测试后公开列表变为空。
- 根因：测试文章使用 `CURRENT_TIMESTAMP`，而公开口径要求 `publish_at <= now`；上下文复用和不同时间源暴露了边界问题。
- 处理：需要证明“已经发布”的测试数据使用固定过去时间；涉及当前时间的业务通过注入 `Clock` 控制。
- 相关：`../rules/testing-policy.md`、`../adr/0018-timezone-asia-tokyo.md`。

## P-003：OpenAPI 嵌套类型需要稳定名称

- 现象：不同响应中的嵌套 `Item` 类型生成同名 schema，契约测试读取到错误定义，Snowflake ID 被误判为数字。
- 根因：springdoc 默认使用过于通用的嵌套类型名。
- 处理：公开响应中的嵌套类型使用 `@Schema(name = "...")` 指定唯一名称；Snowflake ID 在 HTTP 边界保持 JSON string，并由 OpenAPI 测试固定。
- 相关：`../rules/testing-policy.md`、`../api/article.md`。

## P-004：PowerShell 版本、编码和平台命令必须显式约束

- 现象：Windows PowerShell 5.1 在部分代码页下不能稳定解析 UTF-8 无 BOM 脚本；PowerShell 7 中硬编码 `powershell.exe`、`mvn.cmd`、`taskkill.exe` 或 `%TEMP%` 会破坏 Linux 运行。
- 影响：本地 MySQL 初始化和合约验证无法可靠跨平台复现。
- 处理：仅支持 PowerShell 7+；脚本使用 UTF-8 BOM、当前 `pwsh`、跨平台临时目录和 .NET 进程终止；Windows 专用启动选项必须置于 `$IsWindows` 分支。
- 状态：Windows 合约已通过，Linux 实测仍见 `open-issues.md` 的 ISSUE-001。

## P-005：旧基线分支不能直接并回主线

- 现象：文档整理分支基于较旧提交开发，直接合并会把主线已经完成的目录调整重新带回，或产生大面积重命名冲突。
- 根因：合并前没有确认 `origin/main` 是否为任务分支祖先。
- 处理：先检查 ancestry；旧基线分支只把目标提交重放到最新 `main`，再进行链接和路径验证。
- 相关：`../../governance/branch-policy.md`。

## P-006：脏工作树中的已提交成果要按提交边界迁移

- 现象：同一工作树同时存在可合并提交和数百个未提交删除，直接暂存或切分支容易把两批工作混在一起。
- 处理：先用明确 commit SHA 发布候选分支，再在独立工作树审查和修正；未提交删除保持原样，单独建任务处理。
- 相关：`../../governance/branch-policy.md`。

## P-007：本机完整验证不宜无限并行

- 现象：Maven 全量测试、Blog Vitest 和 Admin Vitest 同时运行时，Admin 在没有失败断言的情况下提前退出；同一命令单独重跑后 47 个测试文件、186 项测试全部通过。
- 根因：三套重型验证同时竞争本机 CPU 和内存，Vitest 工作进程没有稳定完成。
- 处理：安装和轻量检查可以并行；Maven 全量测试、前端完整测试和生产构建按资源情况分批或顺序执行。进程无失败断言却异常退出时，先在无资源竞争条件下复现，再判断是否需要修改代码。
- 相关：`../ops/build-and-test.md`。

## 维护规则

1. 新条目必须来自实际发生的问题，不记录猜测。
2. 已失效的命令和路径应随代码更新；经验本身仍有价值时保留根因和处理原则。
3. 未解决事项登记到 `open-issues.md`，本文件不替代任务跟踪。
4. 安全、API、测试等强制约束仍以对应 `rules/`、`api/` 和 ADR 为准。
