import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const sources = ['ArticleCard.vue', 'HorizontalArticle.vue'].map(file =>
  readFileSync(resolve(currentDir, file), 'utf8')
)

describe('article card comment labels', () => {
  it('uses the localized comments label in every card variant', () => {
    for (const source of sources) {
      expect(source).toContain("t('settings.comments')")
      expect(source).not.toMatch(/commentCount\}\}\s+comments/)
    }
  })
})
