import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const sources = ['ArticleCard.vue', 'HorizontalArticle.vue'].map(file =>
  readFileSync(resolve(currentDir, file), 'utf8')
)
const featureSources = [
  resolve(currentDir, '../../Feature/src/Feature.vue'),
  resolve(currentDir, '../../Feature/src/FeatureList.vue')
].map(file => readFileSync(file, 'utf8'))
const articleStyles = readFileSync(
  resolve(currentDir, '../../../styles/components/article.scss'),
  'utf8'
)

describe('article card comment labels', () => {
  it('uses the localized comments label in every card variant', () => {
    for (const source of sources) {
      expect(source).toContain("t('settings.comments')")
      expect(source).not.toMatch(/commentCount\}\}\s+comments/)
    }
  })

  it('uses the existing hot icon for homepage feature cards', () => {
    for (const source of sources) {
      expect(source).toContain('icon-class="hot"')
      expect(source).toContain('settings.pinned')
      expect(source).toContain('settings.featured')
    }
    expect(sources[1]).not.toContain('class="-mb-0.5 mr-1"')
    expect(sources[1]).toContain('class="inline-flex items-center"')
    expect(featureSources[0]).toContain(':badge="badge"')
    expect(featureSources[1]).toContain('badge="featured"')
  })

  it('aligns badges with categories and keeps feature titles within the card', () => {
    expect(articleStyles).toContain('align-middle')
    expect(articleStyles).toContain('lg:text-3xl')
    expect(articleStyles).not.toContain('lg:text-4xl font-extrabold')
  })

  it('collapses localized badge text without a fixed negative margin', () => {
    expect(articleStyles).toContain('grid-template-columns: auto 0fr')
    expect(articleStyles).toContain('grid-template-columns: auto 1fr')
    expect(articleStyles).not.toContain('-mr-[28px]')
    expect(articleStyles).not.toContain('-mr-[30px]')
  })
})
