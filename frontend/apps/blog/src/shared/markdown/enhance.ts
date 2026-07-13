let mermaidModule: Promise<typeof import('mermaid')> | undefined
let highlighterModule: Promise<typeof import('highlight.js/lib/common')> | undefined
let diagramSequence = 0

const loadMermaid = () => {
  mermaidModule ??= import('mermaid')
  return mermaidModule
}

const loadHighlighter = () => {
  highlighterModule ??= import('highlight.js/lib/common')
  return highlighterModule
}

const highlightCodeBlocks = async (root: HTMLElement): Promise<void> => {
  const blocks = Array.from(
    root.querySelectorAll<HTMLElement>('pre.code-block:not([data-highlighted])')
  )
  if (blocks.length === 0) return

  const { default: highlighter } = await loadHighlighter()
  for (const block of blocks) {
    const code = block.querySelector<HTMLElement>('code')
    const language = block.dataset.language ?? 'text'
    if (code && highlighter.getLanguage(language)) {
      code.innerHTML = highlighter.highlight(code.textContent ?? '', { language }).value
      code.classList.add('hljs')
    }
    block.dataset.highlighted = 'true'
  }
}

const renderMermaid = async (
  root: HTMLElement,
  isDarkTheme: boolean
): Promise<void> => {
  const theme = isDarkTheme ? 'dark' : 'default'
  const blocks = Array.from(
    root.querySelectorAll<HTMLElement>('pre.mermaid')
  ).filter(block => block.dataset.mermaidTheme !== theme)
  if (blocks.length === 0) return

  const { default: mermaid } = await loadMermaid()
  mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme })

  for (const block of blocks) {
    const source = block.dataset.mermaidSource ?? block.textContent ?? ''
    try {
      if (!(await mermaid.parse(source, { suppressErrors: true }))) continue
      const { svg, bindFunctions } = await mermaid.render(
        `mermaid-${diagramSequence++}`,
        source
      )
      block.dataset.mermaidSource = source
      block.dataset.mermaidTheme = theme
      block.innerHTML = svg
      bindFunctions?.(block)
    } catch {
      // Preserve the source block so an article remains readable on syntax errors.
    }
  }
}

export const enhanceMarkdown = async (
  root: HTMLElement,
  isDarkTheme: boolean
): Promise<void> => {
  await Promise.all([highlightCodeBlocks(root), renderMermaid(root, isDarkTheme)])
}
