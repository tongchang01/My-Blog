# 前台 V2 工程引入设计

## 1. 目标

将 `C:\Users\TYB\OneDrive\Desktop\前台` 中的前台工程与迁移文档引入 `frontend-v2-integration` 分支，同时满足以下约束：

- 源目录保持不变，不提交、不改写、不删除任何源文件。
- `MyBlog-vue/` 继续作为 V1 前端只读参考，不承载 V2 改动。
- 保留 Aurora 上游作者记录。
- 保留 `pristine-clone` 之后 35 个个人改造提交，并把旧身份 `aid_dou <fj2580ij@aa.jp.fujitsu.com>` 精确改写为当前身份 `TONGYIBIN <Tong-yibin@outlook.com>`。
- 源工程当前 5 个未提交修改和 1 个未跟踪文件作为一个新的独立提交保存。
- 当前只引入博客前台；后台应用尚未开始，不创建空应用或空共享包。

## 2. 目标结构

```text
frontend/
└── apps/
    └── blog/                       # 当前 Vue 3 + Vite 前台工程

docs/
└── archive/
    └── frontend-user-v2-migration/ # 原迁移文档，只作历史参考
```

后续开始后台工程时再增加 `frontend/apps/admin/`；只有两个应用出现真实复用代码后，才增加 `frontend/packages/api-client`、`types`、`config`、`ui` 等共享包。

## 3. 历史迁移策略

在临时目录克隆源工程，复制源工作区未提交内容到临时克隆并提交，然后仅对旧邮箱匹配的提交重写 author 和 committer。目标仓库通过无 squash 的 subtree 导入到 `frontend/apps/blog/`，从而保留可追溯历史。

临时克隆是唯一允许改写历史的位置。源工程 `.git` 和工作区始终只读。所有临时目录在导入与验证结束后删除。

## 4. 文档边界

`feontend-v2-docs/` 记录 Aurora 脱钩、依赖升级和组件迁移过程，其中包含旧路径、旧后端状态和阶段性结论，因此原样归档到 `docs/archive/frontend-user-v2-migration/`，不直接并入 `docs/project-handbook/`。

后续接口联调产生的现行规则继续写入：

- `docs/project-handbook/frontend-user/`
- `docs/project-handbook/api-contract/`
- 必要时新增 ADR 或规则文档

## 5. 提交拆分

1. 设计文档：只冻结引入边界。
2. 实施计划：只记录可执行步骤和验证命令。
3. 前台历史导入：subtree 合并，保留重写后的前台提交历史。
4. 迁移文档归档：只复制历史文档。
5. Workspace 规范化：根据实际构建结果决定是否提升 lockfile 和 workspace 配置，不与原始导入混合。

## 6. 验证

- 源工程迁移前后 `git status --short`、HEAD 和文件哈希保持一致。
- 目标 `frontend/apps/blog/` 与源工作区文件一致，排除 `.git/`、`node_modules/`、`dist/`。
- Aurora 上游提交作者不变，35 个个人提交使用当前身份。
- 在目标路径执行 `pnpm install --frozen-lockfile`、`pnpm lint`、`pnpm exec vue-tsc --noEmit` 和 `pnpm build`。
- 提交前检查 `git diff --stat` 与 `git status --short`。

