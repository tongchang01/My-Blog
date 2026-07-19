import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const appSource = readFileSync(
  fileURLToPath(new URL('./App.vue', import.meta.url)),
  'utf8'
)

describe('copy attribution', () => {
  it('adds localized author and source labels without a license line', () => {
    expect(appSource).toContain("zh: { author: '作者', link: '原文链接' }")
    expect(appSource).toContain("ja: { author: '著者', link: '元リンク' }")
    expect(appSource).toContain("en: { author: 'Author', link: 'Source' }")
    expect(appSource).toContain('authorProfileStore.profile.name')
    expect(appSource).not.toContain('themeConfig.site.author')
    expect(appSource).not.toContain('licensePlaceholder')
  })
})
