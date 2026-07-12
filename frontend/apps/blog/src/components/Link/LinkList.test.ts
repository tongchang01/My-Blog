import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('./LinkList.vue', import.meta.url)),
  'utf8'
)

describe('LinkList', () => {
  it('imports the card component it renders', () => {
    expect(source).toContain("import LinkCard from './LinkCard.vue'")
  })
})
