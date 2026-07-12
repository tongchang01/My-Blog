import type { Router, RouteLocationNormalizedLoaded } from 'vue-router'
import { recordPageView } from '@/features/stats/api'
import type { SupportedLocale } from '@/shared/i18n/locale'

const supportedLocales = new Set(['zh', 'ja', 'en'])

export const resolvePageViewPayload = (
  route: Pick<RouteLocationNormalizedLoaded, 'name' | 'params'>
) => {
  const lang = String(route.params.lang ?? '')
  if (!supportedLocales.has(lang)) return null

  if (route.name === 'article-detail') {
    const articleId = Number(route.params.id)
    if (!Number.isSafeInteger(articleId) || articleId <= 0) return null
    return { articleId, lang: lang as SupportedLocale }
  }

  return { lang: lang as SupportedLocale }
}

export const installPageViewTracking = (router: Router): void => {
  router.afterEach(to => {
    const payload = resolvePageViewPayload(to)
    if (!payload) return
    void recordPageView(payload).catch(() => undefined)
  })
}
