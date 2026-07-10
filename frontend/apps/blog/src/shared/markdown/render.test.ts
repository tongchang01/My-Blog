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
