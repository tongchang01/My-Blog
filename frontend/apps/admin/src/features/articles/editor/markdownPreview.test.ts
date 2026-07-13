import { describe, expect, it } from "vitest";
import { renderMarkdownPreview } from "./markdownPreview";

describe("renderMarkdownPreview", () => {
  it("renders headings and paragraphs", () => {
    const html = renderMarkdownPreview("# 标题\n\n正文");

    expect(html).toContain("<h1>标题</h1>");
    expect(html).toContain("<p>正文</p>");
  });

  it("escapes raw html before rendering markdown", () => {
    const html = renderMarkdownPreview("<script>alert(1)</script>");

    expect(html).not.toContain("<script>");
    expect(html).toContain("&lt;script&gt;alert(1)&lt;/script&gt;");
  });

  it("renders fenced code blocks with their language", () => {
    const html = renderMarkdownPreview("```html\n<div>code</div>\n```");

    expect(html).toContain('<pre class="code-block" data-language="html">');
    expect(html).toContain("&lt;div&gt;code&lt;/div&gt;");
    expect(html).toContain("</code></pre>");
  });

  it("renders the same extended Markdown syntax as the public article", () => {
    const html = renderMarkdownPreview(`| A | B |
| - | - |
| 1 | 2 |

- [x] done

文字[^note]

[^note]: 注释

$E=mc^2$

\`\`\`mermaid
flowchart TD
  A --> B
\`\`\``);

    expect(html).toContain("<table>");
    expect(html).toContain("task-list-item");
    expect(html).toContain("footnote");
    expect(html).toContain("katex");
    expect(html).toContain('<pre class="mermaid">');
  });
});
