# 文章 Markdown 能力实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让前台文章页与管理端预览稳定支持 CommonMark、GFM、代码语言、Mermaid 和 KaTeX，并保留安全的原始 Markdown 存储方式。

**Architecture:** 两个前端应用各自以 `markdown-it` 作为同步 Markdown 解析器，保持原始 HTML 关闭。前台在正文挂载后再按需动态加载 Mermaid 与代码高亮；管理端使用同一语法集预览，避免手写解析器改变语义。

**Tech Stack:** Vue 3、TypeScript、markdown-it、@mdit 插件、KaTeX、Mermaid、highlight.js、Vitest。

## 全局约束

- Markdown 正式范围：CommonMark + GFM + 任务列表、脚注、KaTeX、Mermaid、fenced code block。
- 文章正文继续保存为原始 Markdown；不新增或修改后端接口、表结构。
- 原始 HTML 必须关闭；代码、公式和图表不得执行文章提供的脚本。
- Mermaid 与 highlight.js 仅在正文实际包含对应代码块时动态加载。
- 每项任务独立验证并使用中文单目的提交；不得把现有文章卡片本地修改混入本功能提交。

---

### 任务 1：建立前台 Markdown 语法契约

**文件：**
- 修改：`frontend/apps/blog/package.json`
- 修改：`frontend/apps/blog/pnpm-lock.yaml`
- 修改：`frontend/apps/blog/src/shared/markdown/render.ts`
- 修改：`frontend/apps/blog/src/shared/markdown/render.test.ts`

**产出：** `renderMarkdown(source)` 和 `renderArticleMarkdown(source, locale)` 能渲染 GFM 表格、任务列表、脚注、KaTeX，且对所有 fenced code block 保留安全的 `language-*` 与 `data-language`。

- [ ] **步骤 1：先加入失败用例**

在 `render.test.ts` 覆盖下列输入和关键输出：

```ts
const source = `| A | B |\n| - | - |\n| 1 | 2 |\n\n- [x] done\n\n文字[^note]\n\n[^note]: 注释\n\n$E=mc^2$\n\n\`\`\`java\nclass App {}\n\`\`\`\n\n\`\`\`mermaid\nflowchart LR\nA --> B\n\`\`\``

expect(html).toContain('<table>')
expect(html).toContain('task-list-item')
expect(html).toContain('footnote')
expect(html).toContain('katex')
expect(html).toContain('data-language="java"')
expect(html).toContain('<pre class="mermaid">')
expect(renderMarkdown('<script>alert(1)</script>')).not.toContain('<script>')
```

- [ ] **步骤 2：确认测试当前失败**

运行：`corepack pnpm test -- render.test.ts`（目录：`frontend/apps/blog`）

预期：任务列表、脚注、公式和普通 fenced code 的语言属性断言失败。

- [ ] **步骤 3：最小实现**

添加 `@mdit/plugin-tasklist`、`@mdit/plugin-footnote`、`@mdit/plugin-katex` 与 `katex`。在 `createMarkdown` 中链式注册插件并保持 `html: false`；重写 fence renderer：

```ts
const language = token.info.trim().split(/\s+/)[0].toLowerCase()
if (language === 'mermaid')
  return `<pre class="mermaid">${escapeHtml(token.content)}</pre>\n`

const safeLanguage = /^[a-z0-9_-]+$/.test(language) ? language : 'text'
return `<pre class="code-block" data-language="${safeLanguage}"><code class="language-${safeLanguage}">${escapeHtml(token.content)}</code></pre>\n`
```

- [ ] **步骤 4：确认契约通过**

运行：`corepack pnpm test -- render.test.ts`（目录：`frontend/apps/blog`）

预期：全部通过。

- [ ] **步骤 5：提交**

```powershell
git add frontend/apps/blog/package.json frontend/apps/blog/pnpm-lock.yaml frontend/apps/blog/src/shared/markdown/render.ts frontend/apps/blog/src/shared/markdown/render.test.ts
git commit -m "完善前台 Markdown 解析能力"
```

### 任务 2：在前台正文按需增强图表和代码

**文件：**
- 创建：`frontend/apps/blog/src/shared/markdown/enhance.ts`
- 修改：`frontend/apps/blog/src/pages/post/[slug].vue`
- 修改：`frontend/apps/blog/src/components/PageContent.vue`
- 修改：`frontend/apps/blog/src/styles/components/article.scss`
- 修改：`frontend/apps/blog/package.json`
- 修改：`frontend/apps/blog/pnpm-lock.yaml`

**产出：** 已挂载的 `.post-html` 在包含 Mermaid 时绘制 SVG，在包含 `language-*` 代码块时高亮；无效 Mermaid 保留原代码，主题切换后图表可按当前主题重绘。

- [ ] **步骤 1：加入失败用例**

为增强器写一个 DOM 测试，验证没有 `.mermaid` 或 `.code-block` 时不触发动态加载；验证 Mermaid 节点保存原文后才以 `data-mermaid-theme` 标记完成。

- [ ] **步骤 2：确认测试失败**

运行：`corepack pnpm test -- markdown`（目录：`frontend/apps/blog`）

预期：找不到 `enhanceMarkdown`。

- [ ] **步骤 3：最小实现**

使用一个共享函数避免文章页和关于页重复：

```ts
export const enhanceMarkdown = async (root: HTMLElement, isDark: boolean) => {
  await Promise.all([
    renderMermaidBlocks(root, isDark),
    highlightCodeBlocks(root)
  ])
}
```

`renderMermaidBlocks` 只查询 `pre.mermaid`，先 `mermaid.parse(..., { suppressErrors: true })` 再替换为 SVG，失败时不修改节点。`highlightCodeBlocks` 只查询 `pre.code-block > code.language-*`，语言未知时保留纯代码。两个函数均先检查节点数量，再执行 `import()`。

在文章页和 `PageContent.vue` 的 `nextTick()` 后调用 `enhanceMarkdown(postHtml.value, appStore.theme === 'theme-dark')`；监听主题变化以重绘已经保存原文的 Mermaid 节点。

- [ ] **步骤 4：补齐样式**

为 `.code-block` 添加语言角标、横向滚动和 token 颜色；为 `.mermaid` 添加居中、窄屏横向滚动与 SVG 自适应。导入 KaTeX CSS，且不覆盖既有图片、表格和行内代码样式。

- [ ] **步骤 5：验证与提交**

运行：

```powershell
corepack pnpm test -- markdown
corepack pnpm typecheck
corepack pnpm build
```

提交：

```powershell
git add frontend/apps/blog/src/shared/markdown frontend/apps/blog/src/pages/post/[slug].vue frontend/apps/blog/src/components/PageContent.vue frontend/apps/blog/src/styles/components/article.scss frontend/apps/blog/package.json frontend/apps/blog/pnpm-lock.yaml
git commit -m "增强文章图表与代码块渲染"
```

### 任务 3：让管理端预览使用标准 Markdown 解析器

**文件：**
- 修改：`frontend/apps/admin/package.json`
- 修改：`frontend/apps/admin/pnpm-lock.yaml`
- 替换：`frontend/apps/admin/src/features/articles/editor/markdownPreview.ts`
- 创建：`frontend/apps/admin/src/features/articles/editor/markdownPreview.test.ts`
- 修改：`frontend/apps/admin/src/features/articles/editor/index.vue`
- 修改：`frontend/apps/admin/src/features/articles/editor/index.vue` 的预览样式

**产出：** 管理端 textarea 原样提交 Markdown；预览支持与前台相同的 CommonMark、GFM、代码语言、公式和 Mermaid 语法。

- [ ] **步骤 1：写失败测试**

使用与任务 1 相同的 Markdown 样本，断言 `renderMarkdownPreview` 输出表格、任务列表、脚注、KaTeX class、`language-java`、`pre.mermaid`，并断言脚本标签不会输出。

- [ ] **步骤 2：确认失败**

运行：`corepack pnpm test -- markdownPreview.test.ts`（目录：`frontend/apps/admin`）

预期：当前手写解析器无法满足上述断言。

- [ ] **步骤 3：替换手写解析器**

删除逐行正则解析，改用 `MarkdownIt({ html: false, linkify: true })` 与任务 1 同类插件；保留一个 Mermaid fence renderer，输出 `<pre class="mermaid">`。不在 `form.body` 上调用 replace、trim 以外的格式转换。

- [ ] **步骤 4：在预览容器挂载后增强**

为预览 HTML 容器增加 `ref`，监听 `previewHtml`；在 `nextTick()` 后调用本应用的 Mermaid 和代码高亮增强器。增强器只处理预览容器，不能查询全局 `document`。

- [ ] **步骤 5：验证与提交**

运行：

```powershell
corepack pnpm test -- markdownPreview.test.ts
corepack pnpm typecheck
corepack pnpm build
```

提交：

```powershell
git add frontend/apps/admin/package.json frontend/apps/admin/pnpm-lock.yaml frontend/apps/admin/src/features/articles/editor
git commit -m "统一管理端 Markdown 预览"
```

### 任务 4：记录历史文章恢复流程并做端到端验证

**文件：**
- 创建：`docs/handbook/content/markdown-authoring.md`
- 修改：`docs/README.md`

**产出：** 作者可查阅的语法范围和旧文章恢复步骤；当前系列文章可从 `C:\Users\TYB\OneDrive\Desktop\blog-series` 原稿恢复，而无需改后端代码。

- [ ] **步骤 1：编写写作与迁移说明**

文档必须包含 fenced code、Mermaid、公式、表格、任务列表、脚注示例；并明确恢复步骤为“导出线上原文 → 标题/slug 映射原稿 → 管理端预览核对 → 用既有管理端保存 → 公开接口复查”。

- [ ] **步骤 2：端到端验证首篇文章**

在管理端打开文章 `2076239691205115905`，以 `00-project-structure.md` 的正文替换并保存；通过 `GET /api/public/articles/2076239691205115905?lang=ja` 确认 `body` 含 ` ```mermaid`，再在前台确认图表、代码和公式可显示。

- [ ] **步骤 3：提交**

```powershell
git add docs/handbook/content/markdown-authoring.md docs/README.md
git commit -m "补充文章 Markdown 写作规范"
```

## 完成验证

- [ ] `corepack pnpm test`（前台博客）通过。
- [ ] `corepack pnpm typecheck && corepack pnpm build`（前台博客）通过。
- [ ] `corepack pnpm test`（管理端）通过。
- [ ] `corepack pnpm typecheck && corepack pnpm build`（管理端）通过。
- [ ] 当前系列首篇文章通过管理端保存后，公开接口保留 fenced code 的语言名。
- [ ] `git diff --check`、`git diff --stat`、`git status --short` 已检查，文章卡片修复与 Markdown 提交相互隔离。
