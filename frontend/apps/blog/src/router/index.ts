import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import routes from '~pages'
import Cookies from 'js-cookie'
import { resolveInitialLocale } from '@/shared/i18n/locale'
import { installPageViewTracking } from './pageViewTracking'

const initialLocale = () =>
  resolveInitialLocale(
    Cookies.get('locale') ?? localStorage.getItem('locale'),
    navigator.language
  )

const localizedRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'root',
    redirect: () => ({ name: 'home', params: { lang: initialLocale() } })
  },
  {
    path: '/:lang(zh|ja|en)',
    name: 'home',
    component: () => import('@/pages/index.vue')
  },
  {
    path: '/:lang(zh|ja|en)/posts/:id(\\d+)/:slug?',
    name: 'article-detail',
    component: () => import('@/pages/post/[slug].vue')
  },
  {
    path: '/:lang(zh|ja|en)/categories/:slug',
    name: 'category-articles',
    component: () => import('@/pages/post/search/index.vue')
  },
  {
    path: '/:lang(zh|ja|en)/tags/:slug',
    name: 'tag-articles',
    component: () => import('@/pages/post/search/index.vue')
  }
]

const remainingGeneratedRoutes = routes.filter(route => route.name !== 'index')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...localizedRoutes, ...remainingGeneratedRoutes],
  scrollBehavior(to, from, savedPosition) {
    if (to.name === from.name && to.params.lang !== from.params.lang)
      return false

    return new Promise(resolve => {
      if (to.hash) {
        setTimeout(() => {
          resolve({ el: to.hash, behavior: 'smooth', top: 81 })
        }, 1500)
      } else if (savedPosition) {
        resolve(savedPosition)
      } else {
        resolve({ top: 0 })
      }
    })
  }
})

installPageViewTracking(router)

export default router
