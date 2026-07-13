declare module '@mdit/plugin-footnote' {
  import type MarkdownIt from 'markdown-it'

  export const footnote: (markdown: MarkdownIt) => void
}

declare module '@mdit/plugin-katex' {
  import type MarkdownIt from 'markdown-it'

  export const katex: (markdown: MarkdownIt) => void
}

declare module '@mdit/plugin-tasklist' {
  import type MarkdownIt from 'markdown-it'

  export const tasklist: (
    markdown: MarkdownIt,
    options?: { label?: boolean }
  ) => void
}
