// @vitest-environment happy-dom
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'
import router from './index'

vi.mock('@/features/stats/api', () => ({
  recordPageView: vi.fn(() => Promise.resolve())
}))

const localizedPages = [
  ['/zh', 'home'],
  ['/zh/about', 'about'],
  ['/ja/archives', 'archives'],
  ['/en/tags', 'tags'],
  ['/zh/categories', 'categories'],
  ['/ja/links', 'links'],
  ['/en/message-board', 'message-board'],
  ['/zh/posts/42/example', 'article-detail'],
  ['/ja/categories/backend', 'category-articles'],
  ['/en/tags/java', 'tag-articles']
] as const

const redirectTarget = (
  path: string,
  to: Partial<RouteLocationNormalized>
): RouteLocationRaw => {
  const redirect = router
    .getRoutes()
    .find(route => route.path === path)?.redirect
  if (typeof redirect !== 'function')
    throw new Error(`Missing redirect: ${path}`)
  return redirect(to as RouteLocationNormalized)
}

describe('localized public routes', () => {
  afterEach(() => {
    document.cookie = 'locale=; Max-Age=0; path=/'
    localStorage.clear()
  })

  it.each(localizedPages)('resolves %s as %s', (path, name) => {
    expect(router.resolve(path).name).toBe(name)
  })

  it('redirects old static links with saved-language priority', () => {
    document.cookie = 'locale=ja; path=/'

    const target = redirectTarget('/archives', {
      params: {},
      query: { year: '2026' },
      hash: '#timeline'
    })

    expect(router.resolve(target).fullPath).toBe(
      '/ja/archives?year=2026#timeline'
    )
  })

  it('keeps unknown links inside the selected locale', () => {
    localStorage.setItem('locale', 'en')

    const target = redirectTarget('/:pathMatch(.*)*', {
      params: { pathMatch: ['missing', 'page'] },
      query: {},
      hash: ''
    })
    const resolved = router.resolve(target)

    expect(resolved.name).toBe('not-found')
    expect(resolved.fullPath).toBe('/en/missing/page')
  })
})
