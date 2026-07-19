import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'
import { recordPageView } from '@/features/stats/api'
import type { SupportedLocale } from '@/shared/i18n/locale'

const supportedLocales = new Set(['zh', 'ja', 'en'])

export const resolvePageViewPayload = (
  route: Pick<RouteLocationNormalizedLoaded, 'name' | 'params'>
) => {
  if (route.name === 'not-found') return null
  const routeLang = String(route.params.lang ?? '')
  if (!supportedLocales.has(routeLang)) return null

  if (route.name === 'article-detail') {
    const articleId = Number(route.params.id)
    if (!Number.isSafeInteger(articleId) || articleId <= 0) return null
    return { articleId, lang: routeLang as SupportedLocale }
  }

  return { lang: routeLang as SupportedLocale }
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
    const payload = resolvePageViewPayload(to)
    if (!payload) return
    void recordPageView(payload).catch(() => undefined)
  })
}
