import type { PanzoomObject } from '@panzoom/panzoom'

let mermaidModule: Promise<typeof import('mermaid')> | undefined
let highlighterModule: Promise<typeof import('highlight.js/lib/common')> | undefined
let panzoomModule: Promise<typeof import('@panzoom/panzoom')> | undefined
let diagramSequence = 0
const mermaidViewerCleanup = new WeakMap<HTMLElement, () => void>()

interface MermaidViewerLabels {
  copy: string
  exitFullscreen: string
  fullscreen: string
  panDown: string
  panLeft: string
  panRight: string
  panUp: string
  reset: string
  zoomIn: string
  zoomOut: string
}

const mermaidViewerLabels: Record<string, MermaidViewerLabels> = {
  en: {
    copy: 'Copy Mermaid source',
    exitFullscreen: 'Press Esc to exit fullscreen',
    fullscreen: 'View diagram fullscreen',
    panDown: 'Pan diagram down',
    panLeft: 'Pan diagram left',
    panRight: 'Pan diagram right',
    panUp: 'Pan diagram up',
    reset: 'Reset diagram view',
    zoomIn: 'Zoom in',
    zoomOut: 'Zoom out'
  },
  ja: {
    copy: 'Mermaid ソースをコピー',
    exitFullscreen: 'Esc キーで全画面表示を終了',
    fullscreen: '図を全画面で表示',
    panDown: '図を下へ移動',
    panLeft: '図を左へ移動',
    panRight: '図を右へ移動',
    panUp: '図を上へ移動',
    reset: '図の表示をリセット',
    zoomIn: '拡大',
    zoomOut: '縮小'
  },
  zh: {
    copy: '复制 Mermaid 源码',
    exitFullscreen: '按 Esc 退出全屏',
    fullscreen: '全屏查看图表',
    panDown: '向下平移图表',
    panLeft: '向左平移图表',
    panRight: '向右平移图表',
    panUp: '向上平移图表',
    reset: '重置图表视图',
    zoomIn: '放大图表',
    zoomOut: '缩小图表'
  }
}

const loadMermaid = () => {
  mermaidModule ??= import('mermaid')
  return mermaidModule
}

const loadHighlighter = () => {
  highlighterModule ??= import('highlight.js/lib/common')
  return highlighterModule
}

const loadPanzoom = () => {
  panzoomModule ??= import('@panzoom/panzoom')
  return panzoomModule
}

const waitForDocumentFonts = async (): Promise<void> => {
  await document.fonts?.ready
}

const viewerLabelsFor = (locale: string): MermaidViewerLabels =>
  mermaidViewerLabels[locale] ?? mermaidViewerLabels.zh

const createViewerButton = (
  action: string,
  label: string,
  icon: string,
  onClick: () => unknown
): HTMLButtonElement => {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'mermaid-viewer-button'
  button.dataset.mermaidAction = action
  button.setAttribute('aria-label', label)
  button.title = label
  button.textContent = icon
  button.addEventListener('click', () => void onClick())
  return button
}

const getViewer = (block: HTMLElement): HTMLElement => {
  const existing = block.parentElement
  if (existing?.matches('figure.mermaid-viewer')) return existing

  const viewer = document.createElement('figure')
  viewer.className = 'mermaid-viewer'
  block.replaceWith(viewer)
  viewer.append(block)
  return viewer
}

const mountMermaidViewer = async (
  block: HTMLElement,
  source: string,
  labels: MermaidViewerLabels
): Promise<void> => {
  const svg = block.querySelector<SVGSVGElement>('svg')
  if (!svg) return

  mermaidViewerCleanup.get(block)?.()
  const viewer = getViewer(block)
  viewer.querySelectorAll('[data-mermaid-controls]').forEach(node => node.remove())

  const { default: Panzoom } = await loadPanzoom()
  const panzoom: PanzoomObject = Panzoom(svg, {
    canvas: true,
    cursor: 'grab',
    maxScale: 4,
    minScale: 0.5,
    panOnlyWhenZoomed: true,
    step: 0.25
  })
  const panBy = (x: number, y: number) => panzoom.pan(x, y, { relative: true })
  const topControls = document.createElement('div')
  topControls.className = 'mermaid-viewer-toolbar mermaid-viewer-toolbar-top'
  topControls.dataset.mermaidControls = 'true'
  const navigation = document.createElement('div')
  navigation.className = 'mermaid-viewer-toolbar mermaid-viewer-navigation'
  navigation.dataset.mermaidControls = 'true'

  const copySource = async () => {
    try {
      await navigator.clipboard?.writeText(source)
    } catch {
      // Clipboard access is optional; the diagram remains usable without it.
    }
  }

  const enterFullscreen = async () => {
    if (!viewer.requestFullscreen) return
    try {
      await viewer.requestFullscreen()
      const previousHint = viewer.querySelector('.mermaid-fullscreen-hint')
      previousHint?.remove()
      const hint = document.createElement('div')
      hint.className = 'mermaid-fullscreen-hint'
      hint.textContent = labels.exitFullscreen
      viewer.append(hint)
      window.setTimeout(() => hint.remove(), 2500)
    } catch {
      // Fullscreen access is optional; the inline viewer remains usable.
    }
  }

  topControls.append(
    createViewerButton('fullscreen', labels.fullscreen, '↗', enterFullscreen),
    createViewerButton('copy', labels.copy, '⧉', copySource)
  )
  navigation.append(
    document.createElement('span'),
    createViewerButton('pan-up', labels.panUp, '↑', () => panBy(0, -80)),
    createViewerButton('zoom-in', labels.zoomIn, '+', () => panzoom.zoomIn()),
    createViewerButton('pan-left', labels.panLeft, '←', () => panBy(-80, 0)),
    createViewerButton('reset', labels.reset, '↻', () => panzoom.reset()),
    createViewerButton('pan-right', labels.panRight, '→', () => panBy(80, 0)),
    document.createElement('span'),
    createViewerButton('pan-down', labels.panDown, '↓', () => panBy(0, 80)),
    createViewerButton('zoom-out', labels.zoomOut, '−', () => panzoom.zoomOut())
  )
  viewer.append(topControls, navigation)

  const onWheel = (event: WheelEvent) => {
    if (!event.ctrlKey && !event.metaKey) return
    event.preventDefault()
    panzoom.zoomWithWheel(event)
  }
  block.addEventListener('wheel', onWheel, { passive: false })
  mermaidViewerCleanup.set(block, () => {
    panzoom.destroy()
    block.removeEventListener('wheel', onWheel)
  })
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
  isDarkTheme: boolean,
  locale: string
): Promise<void> => {
  const theme = isDarkTheme ? 'dark' : 'default'
  const blocks = Array.from(
    root.querySelectorAll<HTMLElement>('pre.mermaid')
  ).filter(block => block.dataset.mermaidTheme !== theme)
  if (blocks.length === 0) return

  await waitForDocumentFonts()
  const { default: mermaid } = await loadMermaid()
  mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme })

  for (const block of blocks) {
    mermaidViewerCleanup.get(block)?.()
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
      await mountMermaidViewer(block, source, viewerLabelsFor(locale))
    } catch {
      // Preserve the source block so an article remains readable on syntax errors.
    }
  }
}

export const enhanceMarkdown = async (
  root: HTMLElement,
  isDarkTheme: boolean,
  locale = 'zh'
): Promise<void> => {
  await Promise.all([
    highlightCodeBlocks(root),
    renderMermaid(root, isDarkTheme, locale)
  ])
}
