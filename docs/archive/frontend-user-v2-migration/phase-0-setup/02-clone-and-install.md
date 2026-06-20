# 02 — 克隆仓库 + 安装依赖

## 克隆

选一个工作目录（建议 `C:\tyb\` 或 `~/workspace/`），执行：

```bash
git clone https://github.com/auroral-ui/hexo-theme-aurora.git hexo-theme-aurora-main
cd hexo-theme-aurora-main
```

> **注**：
> - 仓库归属是 `auroral-ui` 组织（不是某些旧文档里写的 `chanshiyucx`，那个已不存在）
> - 默认分支是 `main`
> - 目录名 `hexo-theme-aurora-main` 是本系列文档约定的本地名字，Phase 3 才会改成 myblog 相关命名

确认你 clone 到的版本：

```bash
git log -1 --oneline
```

预期能看到合并到 `main` 的最新 PR commit。仓库虽不频繁更新，但接受 PR，所以 HEAD 不一定是 v2.5.3 tag——本系列文档以 2026 年 6 月时点的 `main` HEAD 为基线。

## 安装依赖

```bash
pnpm install
```

可能看到的输出：

- ✅ **正常**：`Done in xxs` + 一堆 warnings
- ⚠️ **husky install 失败**：因为当前目录不是 git 子模块挂载点之类的——**忽略**，本地预览不需要 husky
- ⚠️ **ERR_PNPM_IGNORED_BUILDS**（pnpm 11 才有）：这就是为什么 [01](./01-environment.md) 让你用 pnpm 8

如果是 pnpm 11 装的，会看到大概这样的报错：

```
ERR_PNPM_IGNORED_BUILDS  Ignored build scripts: @parcel/watcher, core-js, ...
```

解决方法两选一：

- **方案 A（推荐）**：卸载 pnpm 11，装 pnpm 8（见 01）
- **方案 B（临时）**：在工程根目录加 `.npmrc`，写 `node-linker=hoisted`，但不推荐——这会绕过 pnpm 的隔离机制

## `pnpm-lock.yaml` 的处理

装完后 `git status` 可能看到 `pnpm-lock.yaml` 有改动。这是因为：

- 仓库里的 lockfile 是用某个特定 pnpm 版本生成的
- 你本地的 pnpm 版本不同，install 时会重新求解依赖树并改写 lockfile

**本阶段不要 commit 这个改动**——它属于"本地适配"，Phase 1 会统一处理依赖锁定策略。

## 如果根目录出现 `pnpm-workspace.yaml`

干净 clone 的仓库**不应该有**这个文件。如果你的工作目录里出现了，多半是：

- 历史污染（之前手工误建过）
- 某次 `pnpm` 命令自动生成

打开看看，如果内容大致是这种**占位文本**：

```yaml
allowBuilds:
  '@parcel/watcher': set this to true or false
  ...
```

直接删掉：

```bash
rm pnpm-workspace.yaml
```

理由：`allowBuilds` 不是 pnpm 的有效 key（正确的是 `onlyBuiltDependencies`），那些 `set this to true or false` 是占位字符串不是布尔。留着没有任何效果。

## 检查点

- [ ] `node_modules/` 已生成（应该有几百 MB）
- [ ] `node_modules/.bin/vite` 存在（这是下一步要用的启动器）
- [ ] 没有 fatal 错误（husky warning 可以无视）

```bash
ls node_modules/.bin/vite   # 应该返回路径，不是 "No such file"
```

## 下一步

[03-local-modifications.md](./03-local-modifications.md) — 改 `index.html` 和 `vite.config.js`
