import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = (path: string) =>
  readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8').replace(
    /\r\n/g,
    '\n'
  )

describe('build optimization', () => {
  it('uses the modern Sass API and avoids deprecated Sass imports', () => {
    expect(source('../vite.config.mjs')).toContain("api: 'modern'")
    expect(source('./styles/index.scss')).not.toContain('@import')
  })

  it('loads the image lightbox only when it is visible', () => {
    const appSource = source('./App.vue')
    expect(appSource).toContain("() => import('vue-easy-lightbox')")
    expect(appSource).toContain('<VueEasyLightbox\n    v-if="lightBoxVisible"')
  })
})
