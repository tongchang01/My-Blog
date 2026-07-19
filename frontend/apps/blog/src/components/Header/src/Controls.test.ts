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
  it('changes every page through its localized route', () => {
    expect(controlsSource).toContain('params: { ...route.params, lang: name }')
    expect(controlsSource).not.toContain('appStore.changeLocale(name)')
    expect(controlsSource).not.toContain('await appStore.fetchConfig()')
    expect(controlsSource).not.toContain(
      "await router.push({ name: 'home', params: { lang: name } })"
    )
    expect(appSource).toContain(':key="pageKey"')
  })
})
