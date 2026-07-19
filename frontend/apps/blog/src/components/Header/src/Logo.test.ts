import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Logo.vue'), 'utf8')

describe('Logo.vue', () => {
  it('uses the configured site title as the primary brand text', () => {
    expect(source).toContain('{{ appStore.siteTitle }}')
    expect(source).toContain("{{ appStore.siteSubtitle || 'BLOG' }}")
    expect(source).not.toContain('{{ themeConfig.site.author }}')
  })

  it('uses the site logo first and the author profile avatar as fallback', () => {
    expect(source).toContain('v-if="logoImage"')
    expect(source).toContain(
      'themeConfig.value.site.logo || authorProfileStore.profile.avatar'
    )
    expect(source).not.toContain('themeConfig.site.avatar')
  })
})
