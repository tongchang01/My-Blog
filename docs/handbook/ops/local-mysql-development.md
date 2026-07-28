# 本地 MySQL 初始化与种子

> 状态：当前有效；自动化入口存在已登记的初始化账号冲突
> 适用范围：本机 `myblog_v2_dev`
> 最后校准：2026-07-28
> 对应代码：`MyBlog-springboot-v2/scripts/dev/mysql/`
> 权威程度：运行手册

## 运行边界

`initialize.ps1`、`verify.ps1`、`apply-demo-extra.ps1` 和 `initialize.contract-test.ps1` 只允许操作 `myblog_v2_dev`。当前 `initialize.ps1` 启动 local profile 时没有关闭默认管理员初始化，会先创建 `admin`，随后与固定种子中的同名账号冲突；修复 ISSUE-022 前不要把该入口作为可用的全自动初始化方式。

现有脚本仍有以下边界：

- 仅支持 Windows 或 Linux 上的 PowerShell 7+，命令为 `pwsh`；不支持 Windows PowerShell 5.1。
- 脚本使用 UTF-8 BOM、当前 `pwsh` 子进程和按平台选择的 Maven/进程终止方式。
- Windows PowerShell 7 与 Ubuntu GitHub Actions `pwsh` 的脚本合约已验证凭据、数据库名、非空库和显式 `-Reset` 的安全边界；该合约使用进程替身，不证明真实 local 启动与种子导入能够串联成功。

## 自动初始化（暂不可用）

以下命令保留为修复后的目标入口；ISSUE-022 关闭前不得用于重建本地库：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1
```

修复后，仅当需要重建可丢弃的本地库时才显式传入 `-Reset`：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1 -Reset
```

修改脚本后，必须用同一运行时执行合约测试，并以真实空 MySQL 库补充端到端初始化验证：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1
```

## 手工回退流程

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
