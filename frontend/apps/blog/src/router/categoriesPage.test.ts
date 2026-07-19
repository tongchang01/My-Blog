import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = dirname(fileURLToPath(import.meta.url))

describe('categories page', () => {
  it('provides a localized categories page backed by the taxonomy store', () => {
    const page = resolve(currentDir, '../pages/categories.vue')

    expect(existsSync(page)).toBe(true)

    const source = readFileSync(page, 'utf8')
    expect(source).toContain('loadCategories')
    expect(source).toContain(
      "import { TagList, TagItem } from '@/components/Tag'"
    )
    expect(source).toContain('<TagList>')
    expect(source).toContain('route-name="category-articles"')
    expect(source).not.toContain('justify-center gap-3')

    const tagItem = readFileSync(
      resolve(currentDir, '../components/Tag/TagItem.vue'),
      'utf8'
    )
    expect(tagItem).toContain('routeName')
  })
})
