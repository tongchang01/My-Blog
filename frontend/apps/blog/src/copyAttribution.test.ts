import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const appSource = readFileSync(
  fileURLToPath(new URL('./App.vue', import.meta.url)),
  'utf8'
)

describe('copy attribution', () => {
  it('adds localized author and source labels without a license line', () => {
    expect(appSource).toContain("t('copy-protection.author')")
    expect(appSource).toContain("t('copy-protection.link')")
    expect(appSource).not.toContain('copyLabelDefaults')
    expect(appSource).toContain('authorProfileStore.profile.name')
    expect(appSource).not.toContain('themeConfig.site.author')
    expect(appSource).not.toContain('licensePlaceholder')
  })
})
