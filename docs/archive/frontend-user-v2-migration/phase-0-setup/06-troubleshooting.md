# 06 — 故障排查

按"症状 → 检查 → 修法"组织，按出现频率排序。

---

## 启动阶段

### 症状：`./node_modules/.bin/vite: No such file`

**原因**：依赖没装好。

**修法**：

```bash
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

如果还不行，确认 pnpm 版本是 8.x（见 [01](./01-environment.md)）。

---

### 症状：`ERR_PNPM_IGNORED_BUILDS`

**原因**：用了 pnpm 11。

**修法**：降级到 pnpm 8：

```bash
npm install -g pnpm@8
```

详见 [02](./02-clone-and-install.md)。

---

### 症状：端口 5173 已占用

**原因**：之前的 vite 没退干净。

**修法**：

```bash
# Git Bash
netstat -ano | grep 5173
taskkill //PID <pid> //F
```

或者改用别的端口：

```bash
./node_modules/.bin/vite --port 5174
```

---

## 首页白屏

### 症状：浏览器空白，标题显示 "Development Template"，控制台报一大堆 CDN 加载失败（gitalk、valine、twikoo、waline 等）

**原因**：没在根目录建 `index.html`，vite 走了 `templates/index.html` 那条链，那个模板引了一堆评论插件 CDN。

**修法**：去 [03](./03-local-modifications.md) 在工程根目录**新建** `index.html`（注意是新建，不要去改 `templates/` 下的）。

---

### 症状：白屏，Console 报 `Unexpected token '<'`

**原因**：根 `index.html` 内容不对（残留模板标签 / 文件被改坏）。

**修法**：把根 `index.html` 重新覆盖成 [03](./03-local-modifications.md) 给的版本。

---

### 症状：白屏，Console 报 `md5 is not defined` 或 `_ is not defined`

**原因**：CDN 没加载（网络问题 / 拼写错了 / 被屏蔽）。

**修法**：

1. 打开 DevTools → Network，找 `md5.min.js` 和 `lodash.min.js`，看是否 200
2. 如果是 ERR_BLOCKED 或超时 → 换 CDN 或下载到本地 `public/vendor/` 再改 `index.html` 引用相对路径

---

### 症状：白屏，Console 报 `Cannot read properties of undefined (reading 'xxx')`

**原因**：某个 `/api/*.json` 文件缺失或格式错误。

**修法**：

1. Network 面板筛 `/api/`，看哪个请求是 404 或 500
2. 对照 [05 的页面 ↔ API 对照表](./05-start-and-verify.md#页面--api-对照)
3. 缺哪个去 [mock-data/](./mock-data/) 找补上

---

## 视觉异常

### 症状：能渲染但没有紫色渐变背景

**原因**：`index.html` 里 `<body id="body-container">` 的 id 丢了。

**修法**：去 [03](./03-local-modifications.md) 确认 body 标签有 `id="body-container"`。

---

### 症状：字体长得像 Times New Roman

**原因**：`fonts.loli.net` 加载失败。

**修法**：

- 影响不大，可以无视
- 想修：DevTools 看是不是被墙了；本地预览就改用系统字体

---

### 症状：深色模式切换无反应

**原因**：通常是 `site.json` 的 `theme.dark_mode` 被改成了 `off`。

**修法**：编辑 `public/api/site.json`，把 `dark_mode` 改回 `auto`。

---

## 运行时

### 症状：点击文章详情页 404

**原因**：mock 数据里只有 `posts/1.json`，点了不存在 ID 的文章。

**修法**：只点首页里**已经显示出来**的文章，别在地址栏自己拼 ID。

---

### 症状：切换语言后页面崩溃

**原因**：mock 数据是英文 schema，某些语言字段在中/日版本下取不到。

**修法**：

- 本阶段优先用英文跑通验收
- 切换语言只验证"切换动作本身不报错"，不要求所有页面在所有语言下都完美
- Phase 5 处理完整 i18n

---

### 症状：修改了 `public/api/xxx.json` 但页面没变

**原因**：vite 不会热更新 `public/` 下的静态文件，浏览器也可能缓存了 JSON。

**修法**：

- 浏览器硬刷新：`Ctrl + Shift + R`
- 或 DevTools → Network 勾选 "Disable cache"，正常刷新

---

## 终极武器

如果上面都试了还不对：

```bash
# 1. 干净化
cd hexo-theme-aurora-main
git stash                              # 暂存你的所有改动
git checkout .                         # 还原所有跟踪文件
rm -rf node_modules pnpm-lock.yaml public/api/
git status                             # 应该是 clean

# 2. 从头来
# 回到 02-clone-and-install.md，重新走一遍
```

把 `git stash` 的改动留着，对比看是哪一步出了错。

---

## 提交问题前

如果是要让别人帮看，至少提供：

- Node 版本、pnpm 版本、操作系统
- 哪一步出的错（链到本目录的某个 md 文件 + 章节）
- DevTools Console 和 Network 截图
- `git status` 输出

光说"跑不起来"没人能帮你。
