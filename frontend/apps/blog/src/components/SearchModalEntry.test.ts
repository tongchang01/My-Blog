import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const appSource = readFileSync(
  fileURLToPath(new URL('../App.vue', import.meta.url)),
  'utf8'
)
const controlsSource = readFileSync(
  fileURLToPath(new URL('./Header/src/Controls.vue', import.meta.url)),
  'utf8'
)
const modalSource = readFileSync(
  fileURLToPath(new URL('./SearchModal.vue', import.meta.url)),
  'utf8'
)

describe('search modal entry', () => {
  it('mounts the modal and exposes a header control', () => {
    expect(appSource).toContain('<SearchModal />')
    expect(controlsSource).toContain('data-dia="search"')
    expect(controlsSource).toContain('handleSearchOpen')
    expect(controlsSource).toContain('appStore.handleSearchOpen()')
    expect(modalSource).toContain('appStore.openSearchModal')
    expect(modalSource).toContain('appStore.changeOpenModal(status)')
  })
})
