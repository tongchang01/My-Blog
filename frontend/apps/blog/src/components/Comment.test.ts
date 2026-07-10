import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Comment.vue'), 'utf8')

describe('Comment.vue', () => {
  it('uses the V2 comment store', () => {
    expect(source).toContain('useCommentStore')
    expect(source).toContain('articleId')
    expect(source).toContain('commentStore.notice')
  })
})
