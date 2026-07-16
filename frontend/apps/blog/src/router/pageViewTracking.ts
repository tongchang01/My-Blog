import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'
import { recordPageView } from '@/features/stats/api'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { useAppStore } from '@/stores/app'

const supportedLocales = new Set(['zh', 'ja', 'en'])
const publicStaticPages = new Set([
  'about',
  'archives',
  'categories',
  'links',
  'message-board',
  'tags'
])

export const resolvePageViewPayload = (
  route: Pick<RouteLocationNormalizedLoaded, 'name' | 'params'>,
  fallbackLang?: unknown
) => {
  const routeLang = String(route.params.lang ?? '')
  const lang = supportedLocales.has(routeLang)
    ? routeLang
    : publicStaticPages.has(String(route.name))
      ? String(fallbackLang ?? '')
      : ''
  if (!supportedLocales.has(lang)) return null

  if (route.name === 'article-detail') {
    const articleId = Number(route.params.id)
    if (!Number.isSafeInteger(articleId) || articleId <= 0) return null
    return { articleId, lang: lang as SupportedLocale }
  }

  return { lang: lang as SupportedLocale }
}

export const installPageViewTracking = (router: Router): void => {
  router.afterEach((to, from) => {
    if (
      to.name === 'article-detail' &&
      from.name === 'article-detail' &&
      to.params.id === from.params.id &&
      to.params.lang === from.params.lang
    ) {
      return
    }
    const payload = resolvePageViewPayload(to, useAppStore().locale)
    if (!payload) return
    void recordPageView(payload).catch(() => undefined)
  })
}
