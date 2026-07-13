import { describe, expect, it } from 'vitest'
import { renderArticleMarkdown, renderMarkdown } from './render'

describe('Markdown rendering', () => {
  it('renders CommonMark headings', () => {
    expect(renderMarkdown('# Title')).toContain('<h1>Title</h1>')
  })

  it('does not render raw HTML', () => {
    expect(renderMarkdown('<script>alert(1)</script>')).not.toContain(
      '<script>'
    )
  })

  it('protects external links', () => {
    const html = renderMarkdown('[x](https://example.com)')
    expect(html).toContain('rel="nofollow noopener noreferrer"')
    expect(html).toContain('target="_blank"')
  })

  it('marks Mermaid code fences for client-side rendering', () => {
    const html = renderMarkdown('```mermaid\nflowchart TD\n  A --> B\n```')

    expect(html).toContain('<pre class="mermaid">')
    expect(html).toContain('flowchart TD')
    expect(html).not.toContain('language-mermaid')
  })

  it('renders the blog Markdown extensions without enabling raw HTML', () => {
    const html = renderMarkdown(`| A | B |
| - | - |
| 1 | 2 |

- [x] done

文字[^note]

[^note]: 注释

$E=mc^2$

\`\`\`java
class App {}
\`\`\``)

    expect(html).toContain('<div class="markdown-table-wrapper"><table>')
    expect(html).toContain('task-list-item')
    expect(html).toContain('footnote')
    expect(html).toContain('katex')
    expect(html).toContain('data-language="java"')
    expect(html).toContain('class="language-java"')
    expect(renderMarkdown('<script>alert(1)</script>')).not.toContain(
      '<script>'
    )
  })

  it('renders article html, toc, and reading stats', () => {
    const article = renderArticleMarkdown(
      '# HTML解析\n\n## CSS 计算\n\n正文内容',
      'zh'
    )

    expect(article.html).toContain('<h1 id="html解析">HTML解析</h1>')
    expect(article.html).toContain('<h2 id="css-计算">CSS 计算</h2>')
    expect(article.toc).toContain('class="toc"')
    expect(article.toc).toContain('href="#html解析"')
    expect(article.toc).toContain('href="#css-计算"')
    expect(article.wordCount).toBeGreaterThan(0)
    expect(article.readingTime).toBe('约 1 分钟')
  })
})
