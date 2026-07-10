# 本地 MySQL 初始化与种子

> 状态：脚本存在 PowerShell 兼容问题，手工流程可用
> 适用范围：本机 `myblog_v2_dev`
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/scripts/dev/mysql/`
> 权威程度：运行手册

## 当前限制

`initialize.ps1`、`verify.ps1` 和 `initialize.contract-test.ps1` 设计为只操作 `myblog_v2_dev`，但当前不能作为跨 PowerShell 版本的可靠入口：

- UTF-8 无 BOM 文件在 Windows PowerShell 5.1 的部分系统代码页下会解析失败；
- PowerShell 7 环境中，脚本使用 `$PSHOME\powershell.exe` 启动子脚本，而该路径不存在。

修复并通过合约测试前，不使用 `initialize.ps1 -Reset` 重建数据库。该问题登记在 `../start-here/open-issues.md`。

## 手工可用流程

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
