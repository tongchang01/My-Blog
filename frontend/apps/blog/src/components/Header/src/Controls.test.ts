import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const controlsSource = readFileSync(
  fileURLToPath(new URL('./Controls.vue', import.meta.url)),
  'utf8'
)
const appSource = readFileSync(
  fileURLToPath(new URL('../../../App.vue', import.meta.url)),
  'utf8'
)

describe('language switching', () => {
  it('keeps static pages in place while changing their locale', () => {
    expect(controlsSource).toContain('appStore.changeLocale(name)')
    expect(controlsSource).toContain('await appStore.fetchConfig()')
    expect(controlsSource).not.toContain(
      "await router.push({ name: 'home', params: { lang: name } })"
    )
    expect(appSource).toContain(':key="pageKey"')
  })
})
