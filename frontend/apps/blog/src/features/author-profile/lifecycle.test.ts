import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = (path: string) =>
  readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

describe('author profile lifecycle', () => {
  it('loads globally instead of depending on optional profile components', () => {
    expect(source('../../App.vue')).toContain(
      'authorProfileStore.load(appStore.locale)'
    )
    expect(source('../../components/Sidebar/src/Profile.vue')).not.toContain(
      'authorProfileStore.load'
    )
    expect(source('../../components/MobileMenu.vue')).not.toContain(
      'authorProfileStore.load'
    )
  })
})
