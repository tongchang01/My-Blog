function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function renderInline(markdown: string): string {
  return escapeHtml(markdown)
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>");
}

function flushParagraph(lines: string[], html: string[]): void {
  if (lines.length === 0) return;
  html.push(`<p>${renderInline(lines.join(" "))}</p>`);
  lines.length = 0;
}

function flushList(items: string[], html: string[]): void {
  if (items.length === 0) return;
  html.push(`<ul>${items.map(item => `<li>${renderInline(item)}</li>`).join("")}</ul>`);
  items.length = 0;
}

export function renderMarkdownPreview(markdown: string): string {
  const html: string[] = [];
  const paragraph: string[] = [];
  const listItems: string[] = [];
  const codeLines: string[] = [];
  let inCodeBlock = false;

  for (const line of markdown.split(/\r?\n/)) {
    if (line.trim().startsWith("```")) {
      if (inCodeBlock) {
        html.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
        codeLines.length = 0;
        inCodeBlock = false;
      } else {
        flushParagraph(paragraph, html);
        flushList(listItems, html);
        inCodeBlock = true;
      }
      continue;
    }

    if (inCodeBlock) {
      codeLines.push(line);
      continue;
    }

    if (!line.trim()) {
      flushParagraph(paragraph, html);
      flushList(listItems, html);
      continue;
    }

    const heading = /^(#{1,3})\s+(.+)$/.exec(line);
    if (heading) {
      flushParagraph(paragraph, html);
      flushList(listItems, html);
      html.push(`<h${heading[1].length}>${renderInline(heading[2])}</h${heading[1].length}>`);
      continue;
    }

    const listItem = /^[-*]\s+(.+)$/.exec(line);
    if (listItem) {
      flushParagraph(paragraph, html);
      listItems.push(listItem[1]);
      continue;
    }

    flushList(listItems, html);
    paragraph.push(line.trim());
  }

  if (inCodeBlock) {
    html.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
  }
  flushParagraph(paragraph, html);
  flushList(listItems, html);

  return html.join("\n");
}
