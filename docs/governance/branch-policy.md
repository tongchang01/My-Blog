# MyBlog 分支治理策略

## 1. 长期分支

| 分支                                           | 用途                                     |
| -------------------------------------------- | -------------------------------------- |
| `main`                                       | V2 默认主线，日常开发和本地启动的稳定基线                 |
| `integration/next`                           | 下一批功能的长期集成缓冲区，用于承接已完成但尚未回归到 `main` 的任务 |
| `release/v2` 或 `release/<version>`           | 发布冻结分支，用于发布前验证、修复和发布记录；进入发布期再创建        |
| `archive/v1-master-2026-06-26`               | V1 master 归档，只读保留                      |
| `archive/backend-v2-refactor-2026-06-26`     | 旧后端 V2 过程分支归档                          |
| `archive/frontend-v2-integration-2026-06-26` | 旧前端引入过程分支归档                            |

`master` 不再作为 V2 开发入口。GitHub 默认分支切换到 `main` 后，`master` 应冻结或删除。

长期分支必须有稳定职责，不得用阶段状态命名。`integration/next` 是唯一长期集成分支，不能再用 `*-clean`、`*-ready`、`*-integration-ready` 承担准主线职责。

## 2. 分支流转

日常开发按以下顺序流转：

1. 从 `main` 创建短期任务分支。
2. 任务完成并通过局部验证后，合入 `integration/next`。
3. 在 `integration/next` 上执行跨端联调、批次验证和必要的修正。
4. 验证通过后，将 `integration/next` 合回 `main`。
5. 发布期从 `main` 或已验证的 `integration/next` 创建 `release/<version>`。
6. 发布完成后，删除已合并的短期任务分支，保留必要的 `archive/*`。

`main` 不直接承接未完成任务。`integration/next` 不承接长期废弃实验；如果一个任务被取消，应删除对应短期分支，而不是长期留在 `integration/next`。

## 3. 临时开发分支命名

| 类型 | 命名 | 生命周期 |
| --- | --- |
| 功能 | `feature/<scope>-<short-name>` | 合入 `integration/next` 或 `main` 后删除 |
| 修复 | `fix/<scope>-<short-name>` | 合入 `integration/next`、`main` 或 `release/*` 后删除 |
| 重构 | `refactor/<scope>-<short-name>` | 合入后删除 |
| 文档 | `docs/<scope>-<short-name>` | 合入后删除 |
| 临时整合 | `integration/<short-name>` | 仅用于一次性复杂整合，完成后删除；长期集成统一使用 `integration/next` |
| 发布 | `release/<version>` | 发布期保留，发布完成后按需要保留或归档 |
| 归档 | `archive/<old-name>-YYYY-MM-DD` | 只读保留 |

示例：

- `feature/admin-comment-reply`
- `feature/blog-api-integration`
- `fix/backend-local-startup`
- `docs/local-startup-guide`
- `integration/next`
- `release/v2`

## 4. 不再使用的长期名称

以下名称只适合短期协作，不适合作为长期主线：

- `*-clean`
- `*-ready`
- `*-integration-ready`
- `codex/*`

已有此类远端分支应在确认归档或吸收独有提交后清理，不再追加新提交。

## 5. 提交与验证

- 每个提交只做一个明确目的。
- 提交信息使用中文。
- 提交前检查 `git status --short` 和 `git diff --stat`。
- 后端修改至少运行相关 Maven 测试。
- 前端修改至少运行对应应用的类型检查、测试或构建。
- 合入 `integration/next` 前必须完成任务范围内的局部验证。
- `integration/next` 合回 `main` 前必须完成批次级验证，并记录关键命令和结果。

## 6. 远端清理原则

清理远端旧分支必须满足：

1. `main` 已创建并验证通过。
2. GitHub 默认分支已切到 `main`。
3. 旧分支已有归档分支或标签作为回退点。
4. 删除前明确列出将删除的分支。
5. 对仍有独有提交的分支，必须先确认吸收、重做或废弃，不能只按名称删除。

常规清理节奏：

- 已合入的 `feature/*`、`fix/*`、`refactor/*`、`docs/*`：合入后删除。
- 已完成的一次性 `integration/<short-name>`：合入 `main` 后删除。
- `integration/next`：长期保留，不删除。
- `release/*`：发布完成后按版本追溯需要保留、归档或删除。
- `archive/*`：只读保留，不参与日常开发。

不得使用裸 `git push --force`。如确实需要覆盖，只能使用 `--force-with-lease`，并提前说明原因。
