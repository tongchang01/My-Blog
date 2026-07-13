import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const source = (path: string) =>
  readFileSync(new URL(path, import.meta.url), 'utf8').replace(/\r\n/g, '\n')

describe('代码高亮主题', () => {
  it('使用 Highlight.js 内置的 VS 深色主题，而不是维护不完整的手写 token 配色', () => {
    expect(source('../main.ts')).toContain(
      "import 'highlight.js/styles/vs2015.css'"
    )
    expect(source('./components/article.scss')).not.toMatch(
      /\.hljs-comment,\n\s*\.hljs-quote/
    )
  })
})
