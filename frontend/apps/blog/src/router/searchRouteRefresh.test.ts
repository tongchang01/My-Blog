import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('../pages/post/search/index.vue', import.meta.url)),
  'utf8'
)

describe('article search route refresh', () => {
  it('reloads when path params, language or query change', () => {
    expect(source).toContain('() => route.fullPath')
  })
})
