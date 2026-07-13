// @vitest-environment happy-dom
import { describe, expect, it } from 'vitest'
import { enhanceMarkdown } from './enhance'

describe('Markdown enhancement', () => {
  it('does nothing when the rendered article has no diagram or code block', async () => {
    const root = document.createElement('div')

    await expect(enhanceMarkdown(root, false)).resolves.toBeUndefined()
  })

  it('highlights a known code language once', async () => {
    const root = document.createElement('div')
    root.innerHTML =
      '<pre class="code-block" data-language="java"><code class="language-java">class App {}</code></pre>'

    await enhanceMarkdown(root, false)

    const block = root.querySelector<HTMLElement>('pre.code-block')
    const code = root.querySelector<HTMLElement>('code')
    expect(block?.dataset.highlighted).toBe('true')
    expect(code?.classList.contains('hljs')).toBe(true)
    expect(code?.innerHTML).toContain('hljs-title')
  })
})
