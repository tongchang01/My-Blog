import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = dirname(fileURLToPath(import.meta.url))

describe('categories page', () => {
  it('provides a generated /categories page backed by the taxonomy store', () => {
    const page = resolve(currentDir, '../pages/categories.vue')

    expect(existsSync(page)).toBe(true)

    const source = readFileSync(page, 'utf8')
    expect(source).toContain('loadCategories')
    expect(source).toContain('v-for="category in categories"')
    expect(source).toContain("name: 'category-articles'")
  })
})
