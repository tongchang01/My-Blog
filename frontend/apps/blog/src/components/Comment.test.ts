import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Comment.vue'), 'utf8')

describe('Comment.vue', () => {
  it('uses the V2 comment store instead of third-party plugin containers', () => {
    expect(source).toContain('useCommentStore')
    expect(source).toContain('articleId')
    expect(source).toContain('commentStore.notice')
    expect(source).not.toContain('useCommentPlugin')
    expect(source).not.toContain('usePostStore')
    expect(source).not.toContain('gitalk-container')
    expect(source).not.toContain('vcomments')
    expect(source).not.toContain('tcomment')
    expect(source).not.toContain('walineInit')
    expect(source).not.toContain('githubInit')
    expect(source).not.toContain('valineInit')
    expect(source).not.toContain('twikooInit')
  })
})
