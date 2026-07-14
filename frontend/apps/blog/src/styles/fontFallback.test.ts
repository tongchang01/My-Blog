import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('./index.scss', import.meta.url)),
  'utf8'
)
const htmlSource = readFileSync(
  fileURLToPath(new URL('../../index.html', import.meta.url)),
  'utf8'
)

describe('localized font fallback', () => {
  it('uses the matching Noto family with system fallbacks for Chinese and Japanese', () => {
    expect(source).toContain('html:lang(zh) body')
    expect(source).toContain('"Noto Sans SC"')
    expect(source).toContain('html:lang(ja) body')
    expect(source).toContain('"Noto Sans JP"')
    expect(source).toContain('sans-serif')
    expect(htmlSource).toContain('fonts.googleapis.com/css2')
    expect(htmlSource).toContain('display=swap')
  })
})
