import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(
  resolve(currentDir, '../pages/post/[slug].vue'),
  'utf8'
)

describe('post detail page comments', () => {
  it('mounts the V2 comment component after article content', () => {
    expect(source).toContain("import Comment from '@/components/Comment.vue'")
    expect(source).toContain('id="comments"')
    expect(source).toContain(':article-id="article.id"')
    expect(source).toContain(':enabled="!article.locked"')
  })
})
