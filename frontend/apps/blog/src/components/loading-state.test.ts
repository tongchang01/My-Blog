import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = dirname(fileURLToPath(import.meta.url))
const files = [
  '../pages/index.vue',
  '../pages/post/[slug].vue',
  '../pages/archives.vue',
  '../pages/links.vue',
  './Comment.vue'
]

describe('public loading states', () => {
  it('does not render retry controls for load failures', () => {
    for (const file of files) {
      const source = readFileSync(resolve(currentDir, file), 'utf8')
      expect(source).not.toContain('重试')
      expect(source).not.toContain('Retry')
      expect(source).not.toContain('加载失败')
      expect(source).not.toContain('Unable to load')
    }
  })
})
