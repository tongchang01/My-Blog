# 01 — 环境准备

## 版本基线

| 工具 | 最低版本 | 推荐版本 | 说明 |
|---|---|---|---|
| Node.js | 18.0.0 | 18.x LTS 或 20.x LTS | Aurora 用 vite 4，要求 Node ≥ 14.18，实际建议 18+ |
| pnpm | 8.x | **8.x**（不要装 11） | pnpm 11 默认拒绝执行依赖的 postinstall 脚本，会引发一连串问题，详见 02 |
| Git | 2.x | 任意近期版本 | 用来 clone |

## 安装

### Node.js

去 https://nodejs.org/ 下载 LTS 版。安装后验证：

```bash
node -v   # 期望 v18.x.x 或 v20.x.x
npm -v    # 期望 9.x 或 10.x
```

### pnpm

**强烈建议装 8.x，不要装最新的 11**：

```bash
npm install -g pnpm@8
pnpm -v   # 期望 8.x.x
```

如果已经装了 11，可以降级：

```bash
npm uninstall -g pnpm
npm install -g pnpm@8
```

### Git

去 https://git-scm.com/ 装。Windows 用户装的时候注意：

- **Line ending conversions**：选 "Checkout as-is, commit as-is"（避免 CRLF/LF 混乱）
- 其他默认即可

验证：

```bash
git --version
```

## Windows 用户额外注意

- 本文档的 shell 命令都用 **bash 语法**（forward slash 路径、`cp -r`、单引号等）。Windows 上请用：
  - Git Bash（装 Git 时自带）
  - 或者 WSL
  - **不要用 PowerShell 直接复制粘贴**，语法不兼容
- 工作目录建议放在**短路径**下，例如 `C:\tyb\`，不要塞到 `C:\Users\xxx\Documents\...` 里去（中文路径和过长路径都可能踩坑）

## 下一步

[02-clone-and-install.md](./02-clone-and-install.md) — 克隆仓库 + 装依赖
