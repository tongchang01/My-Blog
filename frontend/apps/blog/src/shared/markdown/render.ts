import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: false
})

const defaultLinkOpen = markdown.renderer.rules.link_open

markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index].attrSet('rel', 'nofollow noopener noreferrer')
  const href = tokens[index].attrGet('href') ?? ''
  if (/^https?:\/\//i.test(href)) tokens[index].attrSet('target', '_blank')
  return defaultLinkOpen
    ? defaultLinkOpen(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
}

export const renderMarkdown = (source: string): string =>
  markdown.render(source)
