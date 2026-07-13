import { footnote } from '@mdit/plugin-footnote'
import { katex } from '@mdit/plugin-katex'
import { tasklist } from '@mdit/plugin-tasklist'
import MarkdownIt from 'markdown-it'

interface Heading {
  level: number
  title: string
  id: string
}

interface TocNode extends Heading {
  children: TocNode[]
}

interface ArticleMarkdown {
  html: string
  toc: string
  wordCount: number
  readingTime: string
}

const createMarkdown = (headings?: Heading[]): MarkdownIt => {
  const markdown = new MarkdownIt({
    html: false,
    linkify: true,
    typographer: false
  })
    .use(footnote)
    .use(tasklist, { label: true })
    .use(katex)
  const defaultLinkOpen = markdown.renderer.rules.link_open
  const defaultHeadingOpen = markdown.renderer.rules.heading_open
  const slugCounts = new Map<string, number>()

  markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
    tokens[index].attrSet('rel', 'nofollow noopener noreferrer')
    const href = tokens[index].attrGet('href') ?? ''
    if (/^https?:\/\//i.test(href)) tokens[index].attrSet('target', '_blank')
    return defaultLinkOpen
      ? defaultLinkOpen(tokens, index, options, env, self)
      : self.renderToken(tokens, index, options)
  }

  if (headings) {
    markdown.renderer.rules.heading_open = (
      tokens,
      index,
      options,
      env,
      self
    ) => {
      const title = tokens[index + 1]?.content ?? ''
      const level = Number(tokens[index].tag.slice(1))
      const id = uniqueSlug(title, slugCounts)
      tokens[index].attrSet('id', id)
      headings.push({ level, title, id })
      return defaultHeadingOpen
        ? defaultHeadingOpen(tokens, index, options, env, self)
        : self.renderToken(tokens, index, options)
    }
  }

  markdown.renderer.rules.fence = (tokens, index, options, env, self) => {
    const token = tokens[index]
    const language = token.info.trim().split(/\s+/)[0]?.toLowerCase() ?? 'text'

    if (language === 'mermaid') {
      return `<pre class="mermaid">${escapeHtml(token.content)}</pre>\n`
    }

    const safeLanguage = /^[a-z0-9_-]+$/.test(language) ? language : 'text'
    return `<pre class="code-block" data-language="${safeLanguage}"><code class="language-${safeLanguage}">${escapeHtml(token.content)}</code></pre>\n`
  }

  return markdown
}

export const renderMarkdown = (source: string): string =>
  createMarkdown().render(source)

export const renderArticleMarkdown = (
  source: string,
  locale: 'en' | 'ja' | 'zh'
): ArticleMarkdown => {
  const headings: Heading[] = []
  const html = createMarkdown(headings).render(source)
  const wordCount = countReadableCharacters(source)
  return {
    html,
    toc: renderToc(headings),
    wordCount,
    readingTime: formatReadingTime(wordCount, locale)
  }
}

const uniqueSlug = (title: string, counts: Map<string, number>): string => {
  const base =
    title
      .trim()
      .toLowerCase()
      .replace(/[^\p{L}\p{N}\s_-]/gu, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-') || 'heading'
  const count = counts.get(base) ?? 0
  counts.set(base, count + 1)
  return count === 0 ? base : `${base}-${count + 1}`
}

const countReadableCharacters = (source: string): number =>
  source
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
    .replace(/[>#*_~`[\]()\-+.!|:]/g, ' ')
    .replace(/\s+/g, '').length

const formatReadingTime = (
  wordCount: number,
  locale: 'en' | 'ja' | 'zh'
): string => {
  const minutes = Math.max(1, Math.ceil(wordCount / 500))
  if (locale === 'en') return `${minutes} min`
  if (locale === 'ja') return `約 ${minutes} 分`
  return `约 ${minutes} 分钟`
}

const renderToc = (headings: Heading[]): string => {
  const root: TocNode = { level: 0, title: '', id: '', children: [] }
  const stack = [root]

  for (const heading of headings) {
    const node: TocNode = { ...heading, children: [] }
    while (stack[stack.length - 1].level >= heading.level) stack.pop()
    stack[stack.length - 1].children.push(node)
    stack.push(node)
  }

  return renderTocItems(root.children, true)
}

const renderTocItems = (items: TocNode[], root = false): string => {
  if (items.length === 0) return ''
  const className = root ? ' class="toc"' : ''
  return `<ol${className}>${items
    .map(
      item =>
        `<li class="toc-item"><a href="#${escapeHtml(item.id)}">${escapeHtml(
          item.title
        )}</a>${renderTocItems(item.children)}</li>`
    )
    .join('')}</ol>`
}

const escapeHtml = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
