# 03 · 改 `package.json`

> 上一步 vite.config 已自洽，这一步给工程**改名字 + 删 Hexo 时代的 scripts**。
>
> **不动 `dependencies` 和 `devDependencies` 两个块本身**——卸载依赖统一在 Phase 2 做。本步只改 metadata 和 `scripts`。

---

## 改动清单

### 顶层 metadata

| 字段 | 原值（上游）| 改为 |
|---|---|---|
| `name` | `aurora` 或 `hexo-theme-aurora` | `myblog-frontend`（或你自己起的项目名）|
| `version` | `2.5.3`（上游版本号）| `0.1.0`（你的项目从 0 开始）|
| `description` | 上游介绍 | `MyBlog V2 frontend (forked from auroral-ui/hexo-theme-aurora).` |
| `author` | 上游作者 | 保留空字符串 `""` 或填你的名字 |
| `private` | 不一定有 | **加上 `"private": true`**（避免误发到 npm）|

### 删除的字段（整段删）

| 字段 | 删因 |
|---|---|
| `repository` | 指向上游 GitHub，不是你的 |
| `keywords` | 内含 `hexo-theme` / `aurora` 等 npm 关键词 |
| `files` | npm 发包用，你 `private: true` 后用不到 |
| `bugs`、`homepage`（如有）| 同 `repository` |
| `husky`（顶级 key，如有）| 配置走 `.husky/` 目录，顶级 key 是老式写法 |

### `scripts` 块

保留：

```json
"dev": "vite",
"build": "vite build",
"preview": "vite preview",
"lint": "eslint --ext .js,.vue .",
"prepare": "husky install"
```

删除：

| script | 删因 |
|---|---|
| `serve` | 是 `vite` 的旧别名，跟 `dev` 重复 |
| `postbuild` | Hexo 集成时把 `dist/` 拷到 hexo `source/` 用，已无意义 |
| `env:local` / `env:prod` / `env:pub` | 调 `build/scripts/config-script.js`，脚本上一步已删 |
| `release` / `semantic-release` | 你不发 npm 包 |
| `test` / `test:unit` 等 | 工程 0 测试文件；Phase 2 决定测试方向时再加 |
| `lint:fix`（如有）| 个人项目按需，删了也行 |

> ⚠️ **不删** `prepare`：husky 钩子 install 用，删了 commit 钩子会失效（如果你保留 commitlint 的话）。如果 Phase 0 你装依赖时已经因 husky 报过错绕过，那现在已经可以删它了——按你 Phase 0 的实际情况判断。

---

## 最终文件（参考样例）

```json
{
  "name": "myblog-frontend",
  "version": "0.1.0",
  "description": "MyBlog V2 frontend (forked from auroral-ui/hexo-theme-aurora).",
  "author": "",
  "license": "MIT",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "lint": "eslint --ext .js,.vue .",
    "prepare": "husky install"
  },
  "dependencies": {
    "axios": "^1.5.0",
    "js-cookie": "^3.0.5",
    "normalize.css": "^8.0.1",
    "nprogress": "^0.2.0",
    "pinia": "2.1.6",
    "vue": "^3.3.4",
    "vue-class-component": "^8.0.0-rc.1",
    "vue-i18n": "^9.2.2",
    "vue-router": "^4.2.4",
    "vue3-click-away": "^1.2.4",
    "vue3-lazyload": "^0.3.8"
  },
  "devDependencies": {
    "...": "保持上游原样不动，留给 Phase 2 删"
  }
}
```

> `dependencies` / `devDependencies` 两个块**完全保持上游原状**，下一步 Phase 2 单独清理。

---

## 验证

```bash
# JSON 语法正确
node -e "JSON.parse(require('fs').readFileSync('package.json','utf8'))"
# 无输出 = 通过；报 SyntaxError 就回去找逗号 / 引号

# scripts 列表清晰
node -e "console.log(Object.keys(require('./package.json').scripts))"
# 期望：[ 'dev', 'build', 'preview', 'lint', 'prepare' ]
```

---

## 完成后

```bash
git add -A
git status                      # 应当看到删除了一堆 + 改了 vite.config.js / package.json
```

不要 commit，先做 [04-verify.md](./04-verify.md) 的出口验收，全绿了再一次性 commit。
