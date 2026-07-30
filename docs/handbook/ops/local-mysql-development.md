# 本地 MySQL 初始化与种子

> 状态：当前有效
> 适用范围：本机 `myblog_v2_dev`
> 最后校准：2026-07-30
> 对应代码：`MyBlog-springboot-v2/scripts/dev/mysql/`
> 权威程度：运行手册

## 运行边界

`initialize.ps1`、`verify.ps1`、`apply-demo-extra.ps1` 和 `initialize.contract-test.ps1` 只允许操作 `myblog_v2_dev`。`initialize.ps1` 启动迁移子进程时会临时关闭 local profile 的默认管理员初始化，并在进程结束后恢复调用方原有环境变量，避免与固定种子中的同名 `admin` 冲突。

现有脚本仍有以下边界：

- 仅支持 Windows 或 Linux 上的 PowerShell 7+，命令为 `pwsh`；不支持 Windows PowerShell 5.1。
- 脚本使用 UTF-8 BOM、当前 `pwsh` 子进程和按平台选择的 Maven/进程终止方式。
- Windows PowerShell 7 与 Ubuntu GitHub Actions `pwsh` 的脚本合约验证凭据、数据库名、非空库、显式 `-Reset`、默认管理员开关和 JVM 时区。
- CI 的 `Linux MySQL initialization` job 还会在真实 MySQL 8.4 服务上验证基础初始化、`-Reset`、`-SkipSeed`、重复执行拒绝和最终固定种子。

## 自动初始化

空库或没有活动账号、文章的本地库可直接执行：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1
```

仅当需要重建可丢弃的本地库时才显式传入 `-Reset`：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1 -Reset
```

只应用 Flyway、不导入固定种子时使用：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1 -Reset -SkipSeed
```

修改脚本后，必须用同一运行时执行合约测试；真实 MySQL 8.4 端到端场景由 CI 持续验证：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1
```

## 手工初始化（备用）

1. 用 MySQL 管理工具创建空库 `myblog_v2_dev`，字符集 `utf8mb4`、排序规则 `utf8mb4_0900_ai_ci`。
2. 按 `environment.md` 设置 local 数据库、JWT 和统计密钥。
3. 在当前 PowerShell 会话设置 `$env:MYBLOG_BOOTSTRAP_ADMIN_ENABLED = "false"`，再启动 local 后端；Flyway 自动应用 V1–V6，但不创建默认管理员。
4. 需要固定演示数据时，在 `mysql` 交互终端连接 `myblog_v2_dev`，再执行：

```sql
SOURCE <仓库绝对路径>/MyBlog-springboot-v2/scripts/dev/mysql/seed.sql;
SOURCE <仓库绝对路径>/MyBlog-springboot-v2/scripts/dev/mysql/verify-seed.sql;
```

PowerShell 或 mysql 历史中不得保存真实密码。导入前必须确认目标库是可丢弃的本地开发库，且没有需要保留的 active 账号或文章。导入完成后删除当前会话的覆盖变量，恢复 local profile 的默认行为：

```powershell
Remove-Item Env:MYBLOG_BOOTSTRAP_ADMIN_ENABLED
```

## 本地种子

| 用户名 | 类型 | 仅本地密码 |
| --- | --- | --- |
| `admin` | ADMIN | `MyBlogDev!2026` |
| `demo` | DEMO | `MyBlogDev!2026` |

种子覆盖五种文章状态、分类、标签和大整数 ID 契约。这些凭据不得用于共享或公网环境。

`demo-extra.sql` 可在基础种子后手工导入，用于补充文章、评论、友链、统计和站点展示数据。它不替代生产数据迁移或备份恢复流程。
