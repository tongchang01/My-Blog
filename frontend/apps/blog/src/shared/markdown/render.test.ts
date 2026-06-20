import { describe, expect, it } from 'vitest'
import { renderMarkdown } from './render'

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
})
