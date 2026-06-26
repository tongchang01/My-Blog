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

  it("renders fenced code blocks with escaped content", () => {
    const html = renderMarkdownPreview("```html\n<div>code</div>\n```");

    expect(html).toContain("<pre><code>");
    expect(html).toContain("&lt;div&gt;code&lt;/div&gt;");
    expect(html).toContain("</code></pre>");
  });
});
