import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Toc.vue'), 'utf8')

describe('table of contents navigation', () => {
  it('scrolls within the current page without changing the route hash', () => {
    expect(source).toContain('@click="jumpToTocTarget"')
    expect(source).toContain('event.preventDefault()')
    expect(source).toContain('window.scrollTo({')
    expect(source).toContain("behavior: 'smooth'")
    expect(source).not.toContain('v-scroll-spy-link')
  })
})
