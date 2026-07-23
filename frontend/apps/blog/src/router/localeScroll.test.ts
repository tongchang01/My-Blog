import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const routerSource = readFileSync(resolve(currentDir, 'index.ts'), 'utf8')
const homeSource = readFileSync(
  resolve(currentDir, '../pages/index.vue'),
  'utf8'
)

describe('locale switching scroll behavior', () => {
  it('preserves the current position when only the locale changes', () => {
    expect(routerSource).toContain(
      'to.name === from.name && to.params.lang !== from.params.lang'
    )
    expect(routerSource).toContain('return false')
    expect(homeSource).not.toContain('backToArticleTop')
  })
})
