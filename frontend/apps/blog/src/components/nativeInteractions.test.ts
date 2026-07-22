import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const readSource = (path: string): string =>
  readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const navigationSource = readSource('./Header/src/Navigation.vue')
const logoSource = readSource('./Header/src/Logo.vue')
const controlsSource = readSource('./Header/src/Controls.vue')
const dropdownItemSource = readSource('./Dropdown/src/DropdownItem.vue')
const toggleSource = readSource('./ToggleSwitch/Toggle.vue')
const mobileMenuSource = readSource('./MobileMenu.vue')
const searchSource = readSource('./SearchModal.vue')
const articleCardSources = [
  readSource('./ArticleCard/src/ArticleCard.vue'),
  readSource('./ArticleCard/src/HorizontalArticle.vue')
]
const notFoundSource = readSource('../pages/[...all].vue')
const appSource = readSource('../App.vue')

describe('native public interactions', () => {
  it('uses real links for site navigation and the logo', () => {
    expect(navigationSource).toContain('<RouterLink')
    expect(navigationSource).toContain('<a')
    expect(navigationSource).not.toContain('@click="pushPage')
    expect(logoSource).toContain('<RouterLink')
    expect(logoSource).not.toContain('@click="handleLogoClick"')
    expect(mobileMenuSource).toContain('<RouterLink')
  })

  it('uses buttons for header, dropdown, and theme actions', () => {
    expect(controlsSource).toMatch(
      /<button[\s\S]*data-dia="language"[\s\S]*data-dia="search"[\s\S]*data-dia="menu"/
    )
    expect(dropdownItemSource).toContain('<button v-else type="button"')
    expect(toggleSource).toContain('<button')
    expect(toggleSource).toContain(':aria-pressed="toggleStatus"')
  })

  it('uses real article links in search results', () => {
    expect(searchSource).toContain('<RouterLink')
    expect(searchSource).not.toContain('javascript:void(0)')
  })

  it('uses router links for article cards and 404 recovery', () => {
    for (const source of articleCardSources) {
      expect(source).toContain(':is="article ? RouterLink : \'div\'"')
      expect(source).not.toContain('@click="navigateToArticle"')
    }
    expect(notFoundSource).toContain('<RouterLink')
    expect(notFoundSource).toContain("t('settings.tips-back-to-home')")
  })

  it('keeps a visible keyboard focus indicator', () => {
    expect(appSource).toContain('*:focus-visible')
    expect(appSource).toContain('#App-Container:focus-visible')
    expect(appSource).not.toMatch(/\*:focus\s*{\s*outline:\s*none/)
  })
})
