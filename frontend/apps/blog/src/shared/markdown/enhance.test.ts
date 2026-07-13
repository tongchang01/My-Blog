// @vitest-environment happy-dom
import { describe, expect, it, vi } from 'vitest'
import { enhanceMarkdown } from './enhance'

const mermaid = vi.hoisted(() => ({
  initialize: vi.fn(),
  parse: vi.fn().mockResolvedValue(true),
  render: vi.fn().mockResolvedValue({ svg: '<svg data-diagram="true"></svg>' })
}))

const panzoom = vi.hoisted(() => ({
  create: vi.fn(),
  destroy: vi.fn(),
  pan: vi.fn(),
  reset: vi.fn(),
  zoomIn: vi.fn(),
  zoomOut: vi.fn(),
  zoomWithWheel: vi.fn()
}))

panzoom.create.mockReturnValue(panzoom)

vi.mock('mermaid', () => ({ default: mermaid }))
vi.mock('@panzoom/panzoom', () => ({ default: panzoom.create }))

describe('Markdown enhancement', () => {
  it('does nothing when the rendered article has no diagram or code block', async () => {
    const root = document.createElement('div')

    await expect(enhanceMarkdown(root, false)).resolves.toBeUndefined()
  })

  it('highlights a known code language once', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    const originalClipboard = Object.getOwnPropertyDescriptor(navigator, 'clipboard')
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText }
    })
    const root = document.createElement('div')
    root.innerHTML =
      '<pre class="code-block" data-language="java"><code class="language-java">class App {}</code></pre>'

    try {
      await enhanceMarkdown(root, false, 'ja')

      const block = root.querySelector<HTMLElement>('pre.code-block')
      const code = root.querySelector<HTMLElement>('code')
      const copyButton = root.querySelector<HTMLButtonElement>('[data-code-action="copy"]')
      expect(block?.dataset.highlighted).toBe('true')
      expect(code?.classList.contains('hljs')).toBe(true)
      expect(code?.innerHTML).toContain('hljs-title')
      expect(copyButton?.title).toBe('コードをコピー')

      copyButton?.click()
      await Promise.resolve()
      expect(writeText).toHaveBeenCalledWith('class App {}')
    } finally {
      if (originalClipboard) Object.defineProperty(navigator, 'clipboard', originalClipboard)
      else delete (navigator as { clipboard?: unknown }).clipboard
    }
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

  it('waits for document fonts before rendering a Mermaid diagram', async () => {
    let resolveFonts: (() => void) | undefined
    const fontsReady = new Promise<void>(resolve => {
      resolveFonts = resolve
    })
    const originalFonts = Object.getOwnPropertyDescriptor(document, 'fonts')
    Object.defineProperty(document, 'fonts', {
      configurable: true,
      value: { ready: fontsReady }
    })
    mermaid.render.mockClear()

    const root = document.createElement('div')
    root.innerHTML = '<pre class="mermaid">flowchart TD\nA --> B</pre>'

    try {
      const enhancement = enhanceMarkdown(root, false)
      await new Promise(resolve => setTimeout(resolve, 0))
      expect(mermaid.render).not.toHaveBeenCalled()

      resolveFonts?.()
      await enhancement
      expect(mermaid.render).toHaveBeenCalledTimes(1)
    } finally {
      if (originalFonts) Object.defineProperty(document, 'fonts', originalFonts)
      else delete (document as { fonts?: unknown }).fonts
    }
  })

  it('adds GitHub-style controls to a rendered Mermaid diagram', async () => {
    mermaid.render.mockClear()
    panzoom.create.mockClear()
    panzoom.zoomIn.mockClear()
    panzoom.reset.mockClear()
    panzoom.pan.mockClear()
    const root = document.createElement('div')
    const block = document.createElement('pre')
    block.className = 'mermaid'
    block.textContent = 'flowchart TD\nA --> B'
    root.append(block)

    await enhanceMarkdown(root, false, 'ja')

    expect(root.querySelector('figure.mermaid-viewer')).not.toBeNull()
    expect(panzoom.create).toHaveBeenCalledTimes(1)
    expect(
      root.querySelector<HTMLButtonElement>('[data-mermaid-action="copy"]')
        ?.title
    ).toBe('Mermaid ソースをコピー')
    root
      .querySelector<HTMLButtonElement>('[data-mermaid-action="zoom-in"]')
      ?.click()
    root
      .querySelector<HTMLButtonElement>('[data-mermaid-action="reset"]')
      ?.click()
    root
      .querySelector<HTMLButtonElement>('[data-mermaid-action="pan-left"]')
      ?.click()

    expect(panzoom.zoomIn).toHaveBeenCalledTimes(1)
    expect(panzoom.reset).toHaveBeenCalledTimes(1)
    expect(panzoom.pan).toHaveBeenCalledWith(-80, 0, { relative: true })
  })

  it('briefly explains how to leave fullscreen mode', async () => {
    vi.useFakeTimers()
    const root = document.createElement('div')
    const block = document.createElement('pre')
    block.className = 'mermaid'
    block.textContent = 'flowchart TD\nA --> B'
    root.append(block)

    try {
      await enhanceMarkdown(root, false, 'ja')
      const viewer = root.querySelector<HTMLElement>('figure.mermaid-viewer')
      Object.defineProperty(viewer, 'requestFullscreen', {
        configurable: true,
        value: vi.fn().mockResolvedValue(undefined)
      })

      viewer
        ?.querySelector<HTMLButtonElement>('[data-mermaid-action="fullscreen"]')
        ?.click()
      await Promise.resolve()
      expect(viewer?.querySelector('.mermaid-fullscreen-hint')?.textContent).toBe(
        'Esc キーで全画面表示を終了'
      )

      vi.advanceTimersByTime(2500)
      expect(viewer?.querySelector('.mermaid-fullscreen-hint')).toBeNull()
    } finally {
      vi.useRealTimers()
    }
  })
})
