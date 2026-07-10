# 本地 MySQL 初始化与种子

> 状态：PowerShell 7+ 自动化入口可用；Windows 与 Ubuntu CI 已通过合约验证
> 适用范围：本机 `myblog_v2_dev`
> 最后校准：2026-07-11
> 对应代码：`MyBlog-springboot-v2/scripts/dev/mysql/`
> 权威程度：运行手册

## 运行边界

`initialize.ps1`、`verify.ps1`、`apply-demo-extra.ps1` 和 `initialize.contract-test.ps1` 只操作 `myblog_v2_dev`，并且：

- 仅支持 Windows 或 Linux 上的 PowerShell 7+，命令为 `pwsh`；不支持 Windows PowerShell 5.1。
- 脚本使用 UTF-8 BOM、当前 `pwsh` 子进程和按平台选择的 Maven/进程终止方式。
- Windows PowerShell 7 与 Ubuntu GitHub Actions `pwsh` 已通过合约验证；显式 `-Reset` 场景仍待加入合约覆盖。

## 自动初始化

1. 确认目标库是可丢弃的本地 `myblog_v2_dev`，没有需要保留的 active 账号或文章。
2. 按 `environment.md` 设置 local 数据库、JWT 和统计密钥，并确保 `mysql` 与 Maven 已在 `PATH` 中。
3. 使用 PowerShell 7+ 执行：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1
```

仅当需要重建可丢弃的本地库时，显式传入 `-Reset`：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.ps1 -Reset
```

修改脚本后，用同一运行时执行合约测试：

```powershell
pwsh -NoProfile -File MyBlog-springboot-v2/scripts/dev/mysql/initialize.contract-test.ps1
```

## 手工回退流程

1. 用 MySQL 管理工具创建空库 `myblog_v2_dev`，字符集 `utf8mb4`、排序规则 `utf8mb4_0900_ai_ci`。
2. 按 `environment.md` 设置 local 数据库、JWT 和统计密钥。
3. 启动 local 后端；Flyway 自动应用 V1–V4。
4. 需要固定演示数据时，在 `mysql` 交互终端连接 `myblog_v2_dev`，再执行：

```sql
SOURCE <仓库绝对路径>/MyBlog-springboot-v2/scripts/dev/mysql/seed.sql;
SOURCE <仓库绝对路径>/MyBlog-springboot-v2/scripts/dev/mysql/verify-seed.sql;
```

PowerShell 或 mysql 历史中不得保存真实密码。导入前必须确认目标库是可丢弃的本地开发库，且没有需要保留的 active 账号或文章。

## 本地种子

| 用户名 | 类型 | 仅本地密码 |
| --- | --- | --- |
| `admin` | ADMIN | `MyBlogDev!2026` |
| `demo` | DEMO | `MyBlogDev!2026` |

种子覆盖五种文章状态、分类、标签和大整数 ID 契约。这些凭据不得用于共享或公网环境。

`demo-extra.sql` 可在基础种子后手工导入，用于补充文章、评论、友链、统计和站点展示数据。它不替代生产数据迁移或备份恢复流程。
