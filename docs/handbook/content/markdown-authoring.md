# 文章 Markdown 写作与历史恢复

> 状态：当前有效
> 适用范围：文章正文与管理端文章预览
> 最后校准：2026-07-13
> 对应代码：`frontend/apps/blog/src/shared/markdown/`、`frontend/apps/admin/src/features/articles/editor/`
> 权威程度：写作规范

## 正式语法范围

文章正文保存为原始 Markdown。管理端预览和博客端都使用 CommonMark、GitHub Flavored Markdown（GFM）以及以下固定扩展：

- 标题、段落、引用、链接、图片、列表、分割线、粗体、斜体、删除线和行内代码。
- GFM 表格与任务列表。
- 脚注、KaTeX 公式、带语言名的 fenced code block 和 Mermaid 图表。
- 原始 HTML 不会被渲染；不要把脚本、样式或 HTML 组件作为文章能力依赖。

## 推荐写法

````md
| 组件 | 状态 |
| --- | --- |
| Markdown | 已启用 |

- [x] 管理端预览
- [ ] 发布后确认

行内公式：$E=mc^2$。

块级公式：

$$
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
$$

Java 示例：

```java
public record Article(String title) {}
```

Mermaid 示例：

```mermaid
flowchart LR
  Writer[作者] --> Admin[管理端预览]
  Admin --> Blog[公开文章]
```

脚注示例[^markdown]。

[^markdown]: 脚注定义放在正文任意位置。
````

代码围栏的语言名决定高亮类型。常用语言包括 `java`、`ts`、`js`、`json`、`yaml`、`sql`、`bash`、`text` 和 `mermaid`。图表必须使用 `\`\`\`mermaid` 围栏；缩进四个空格的普通代码块没有语言信息，不会被当成图表。

## 编辑与发布流程

1. 在管理端文章编辑页输入或粘贴原始 Markdown。
2. 在右侧预览核对表格、代码、公式和 Mermaid；非法 Mermaid 保留为代码，先修正语法再保存。
3. 保存后从公开文章页确认最终显示。文章正文不会在保存时转换为 HTML。
4. 原始 HTML 即使在预览中也不会生效；需要图片时使用 Markdown 图片语法和已上传的公开地址。

## 历史文章恢复

早期导入曾把 fenced code block 转成四空格缩进代码，导致 `mermaid`、`java` 等语言名丢失。前端无法可靠推断一个普通代码块原本是什么语言或图表，因此必须以写作原稿为准恢复。

1. 从公开文章接口导出当前正文，确认受影响文章和语言版本。
2. 按文章标题或 slug 找到对应的原始 Markdown 写作稿。
3. 在管理端以原稿正文替换当前正文，重点检查所有 `\`\`\`语言名` 围栏是否完整闭合。
4. 在管理端预览核对图表、代码和公式后，用既有保存功能提交。
5. 再次读取公开文章接口，确认 `body` 仍包含 `\`\`\`mermaid`、`\`\`\`java` 等围栏；最后在公开文章页复查显示。

恢复只改文章内容，不需要新增后端接口、表字段或迁移脚本。
