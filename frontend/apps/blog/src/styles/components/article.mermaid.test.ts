import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const articleStyles = readFileSync(new URL('./article.scss', import.meta.url), 'utf8')

describe('Mermaid article styles', () => {
  it('keeps Mermaid HTML labels independent from article paragraphs', () => {
    expect(articleStyles).toMatch(
      /pre\.mermaid\s+foreignObject\s+p\s*\{[\s\S]*?margin:\s*0;[\s\S]*?font-size:\s*inherit;[\s\S]*?line-height:\s*inherit;[\s\S]*?overflow-wrap:\s*normal;/
    )
  })
})
