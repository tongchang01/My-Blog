import { footnote } from "@mdit/plugin-footnote";
import { katex } from "@mdit/plugin-katex";
import { tasklist } from "@mdit/plugin-tasklist";
import MarkdownIt from "markdown-it";

const escapeHtml = (value: string): string =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const createMarkdown = (): MarkdownIt => {
  const markdown = new MarkdownIt({
    html: false,
    linkify: true,
    typographer: false
  })
    .use(footnote)
    .use(tasklist, { label: true })
    .use(katex);

  markdown.renderer.rules.fence = (tokens, index) => {
    const token = tokens[index];
    const language = token.info.trim().split(/\s+/)[0]?.toLowerCase() ?? "text";

    if (language === "mermaid") {
      return `<pre class="mermaid">${escapeHtml(token.content)}</pre>\n`;
    }

    const safeLanguage = /^[a-z0-9_-]+$/.test(language) ? language : "text";
    return `<pre class="code-block" data-language="${safeLanguage}"><code class="language-${safeLanguage}">${escapeHtml(token.content)}</code></pre>\n`;
  };

  return markdown;
};

export const renderMarkdownPreview = (markdown: string): string =>
  createMarkdown().render(markdown);
