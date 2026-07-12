import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, '../pages/about.vue'), 'utf8')

describe('about page', () => {
  it('derives reading statistics from the about markdown', () => {
    expect(source).toContain('renderArticleMarkdown')
    expect(source).toContain('page.count_time')
  })
})
