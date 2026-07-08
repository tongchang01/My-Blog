import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('./PostStats.vue', import.meta.url)),
  'utf8'
)

describe('PostStats', () => {
  it('only receives reading time and word count props', () => {
    expect(source).toContain('postWordCount')
    expect(source).toContain('postTimeCount')
    expect(source).not.toContain('currentPath')
  })
})
