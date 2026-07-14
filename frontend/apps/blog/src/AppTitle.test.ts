import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const appSource = readFileSync(
  fileURLToPath(new URL('./App.vue', import.meta.url)),
  'utf8'
)
const htmlSource = readFileSync(
  fileURLToPath(new URL('../index.html', import.meta.url)),
  'utf8'
)

describe('document title', () => {
  it('uses the meta store as the single runtime title source', () => {
    expect(appSource).toContain('document.title = title.value')
    expect(appSource).not.toContain('<teleport to="head">')
    expect(htmlSource).not.toContain('Aurora Dev')
  })
})
