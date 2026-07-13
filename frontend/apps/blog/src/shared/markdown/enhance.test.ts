// @vitest-environment happy-dom
import { describe, expect, it, vi } from 'vitest'
import { enhanceMarkdown } from './enhance'

const mermaid = vi.hoisted(() => ({
  initialize: vi.fn(),
  parse: vi.fn().mockResolvedValue(true),
  render: vi.fn().mockResolvedValue({ svg: '<svg data-diagram="true"></svg>' })
}))

vi.mock('mermaid', () => ({ default: mermaid }))

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

  it('stores Mermaid source and theme after rendering the diagram', async () => {
    const root = document.createElement('div')
    const block = document.createElement('pre')
    block.className = 'mermaid'
    block.textContent = 'flowchart TD\nA --> B'
    root.append(block)

    await enhanceMarkdown(root, false)

    expect(mermaid.parse).toHaveBeenCalledWith('flowchart TD\nA --> B', {
      suppressErrors: true
    })
    expect(block?.dataset.mermaidSource).toBe('flowchart TD\nA --> B')
    expect(block?.dataset.mermaidTheme).toBe('default')
    expect(block?.innerHTML).toContain('data-diagram="true"')
  })
})
