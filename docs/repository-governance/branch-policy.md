# MyBlog 分支治理策略

## 1. 长期分支

| 分支 | 用途 |
| --- | --- |
| `main` | V2 默认主线，日常开发和本地启动的稳定基线 |
| `archive/v1-master-2026-06-26` | V1 master 归档，只读保留 |
| `archive/backend-v2-refactor-2026-06-26` | 旧后端 V2 过程分支归档 |
| `archive/frontend-v2-integration-2026-06-26` | 旧前端引入过程分支归档 |

`master` 不再作为 V2 开发入口。GitHub 默认分支切换到 `main` 后，`master` 应冻结或删除。

## 2. 临时开发分支命名

| 类型 | 命名 |
| --- | --- |
| 功能 | `feature/<scope>-<short-name>` |
| 修复 | `fix/<scope>-<short-name>` |
| 重构 | `refactor/<scope>-<short-name>` |
| 文档 | `docs/<scope>-<short-name>` |
| 整合 | `integration/<short-name>` |
| 归档 | `archive/<old-name>-YYYY-MM-DD` |

示例：

- `feature/admin-comment-reply`
- `feature/blog-api-integration`
- `fix/backend-local-startup`
- `docs/local-startup-guide`

## 3. 不再使用的长期名称

以下名称只适合短期协作，不适合作为长期主线：

- `*-clean`
- `*-ready`
- `*-integration-ready`
- `codex/*`

## 4. 提交与验证

- 每个提交只做一个明确目的。
- 提交信息使用中文。
- 提交前检查 `git status --short` 和 `git diff --stat`。
- 后端修改至少运行相关 Maven 测试。
- 前端修改至少运行对应应用的类型检查、测试或构建。

## 5. 远端清理原则

清理远端旧分支必须满足：

1. `main` 已创建并验证通过。
2. GitHub 默认分支已切到 `main`。
3. 旧分支已有归档分支或标签作为回退点。
4. 删除前明确列出将删除的分支。

不得使用裸 `git push --force`。如确实需要覆盖，只能使用 `--force-with-lease`，并提前说明原因。
