import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const source = (path: string) =>
  readFileSync(new URL(path, import.meta.url), 'utf8').replace(/\r\n/g, '\n')

describe('代码高亮主题', () => {
  it('按站点明暗主题加载 Highlight.js 内置的 GitHub 配色', () => {
    expect(source('./index.scss')).toContain(
      "meta.load-css('./components/code-highlight')"
    )
    expect(source('../main.ts')).not.toContain('highlight.js/styles/')

    const themeStyles = source('./components/code-highlight.scss')
    expect(themeStyles).toContain(
      "body.theme-dark {\n  @include meta.load-css('highlight.js/styles/github-dark')"
    )
    expect(themeStyles).toContain(
      "body.theme-light {\n  @include meta.load-css('highlight.js/styles/github')"
    )
    expect(themeStyles).toContain('background: #f6f8fa')
    expect(themeStyles).toContain('border-color: #d0d7de')
  })
})
