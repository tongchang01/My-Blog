import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, RouteRecordRedirectOption } from 'vue-router'
import Cookies from 'js-cookie'
import { resolveInitialLocale } from '@/shared/i18n/locale'
import { installPageViewTracking } from './pageViewTracking'

const initialLocale = () =>
  resolveInitialLocale(
    Cookies.get('locale') ?? localStorage.getItem('locale'),
    navigator.language
  )

const localizedRedirect =
  (name: string): RouteRecordRedirectOption =>
  to => ({
    name,
    params: { lang: initialLocale() },
    query: to.query,
    hash: to.hash
  })

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'root',
    redirect: localizedRedirect('home')
  },
  { path: '/about', redirect: localizedRedirect('about') },
  { path: '/archives', redirect: localizedRedirect('archives') },
  { path: '/tags', redirect: localizedRedirect('tags') },
  { path: '/categories', redirect: localizedRedirect('categories') },
  { path: '/links', redirect: localizedRedirect('links') },
  {
    path: '/message-board',
    redirect: localizedRedirect('message-board')
  },
  {
    path: '/:lang(zh|ja|en)',
    name: 'home',
    component: () => import('@/pages/index.vue')
  },
  {
    path: '/:lang(zh|ja|en)/about',
    name: 'about',
    component: () => import('@/pages/about.vue')
  },
  {
    path: '/:lang(zh|ja|en)/archives',
    name: 'archives',
    component: () => import('@/pages/archives.vue')
  },
  {
    path: '/:lang(zh|ja|en)/tags',
    name: 'tags',
    component: () => import('@/pages/tags.vue')
  },
  {
    path: '/:lang(zh|ja|en)/categories',
    name: 'categories',
    component: () => import('@/pages/categories.vue')
  },
  {
    path: '/:lang(zh|ja|en)/links',
    name: 'links',
    component: () => import('@/pages/links.vue')
  },
  {
    path: '/:lang(zh|ja|en)/message-board',
    name: 'message-board',
    component: () => import('@/pages/message-board.vue')
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
  },
  {
    path: '/:lang(zh|ja|en)/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/pages/[...all].vue')
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: to => ({
      name: 'not-found',
      params: {
        lang: initialLocale(),
        pathMatch: to.params.pathMatch
      },
      query: to.query,
      hash: to.hash
    })
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
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
