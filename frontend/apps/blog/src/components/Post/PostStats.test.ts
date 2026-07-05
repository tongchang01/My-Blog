import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('./PostStats.vue', import.meta.url)),
  'utf8'
)

describe('PostStats', () => {
  it('does not render third-party page view or comment counters', () => {
    expect(source).not.toContain('waline-pageview-count')
    expect(source).not.toContain('waline-comment-count')
    expect(source).not.toContain('twikoo_visitors')
    expect(source).not.toContain('leancloud_visitors')
    expect(source).not.toContain('useCommentPlugin')
  })
})
