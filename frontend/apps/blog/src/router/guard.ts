import router from '@/router'
import { useAppStore } from '@/stores/app'
import i18n from '@/locales/index'
import { useMetaStore } from '@/stores/meta'
import { isSupportedLocale } from '@/shared/i18n/locale'

router.beforeEach(async (to, from, next) => {
  const appStore = useAppStore()
  const metaStore = useMetaStore()

  const routeLocale = to.params.lang
  if (routeLocale !== undefined && !isSupportedLocale(routeLocale)) {
    next({ name: 'root' })
    return
  }
  if (isSupportedLocale(routeLocale)) {
    appStore.changeLocale(routeLocale)
  }

  appStore.startLoading()

  const title = i18n.global.te(`menu.${String(to.name)}`)
    ? i18n.global.t(`menu.${String(to.name)}`)
    : to.name
  metaStore.setTitle(String(title))

  i18n.global.locale.value = appStore.locale

  next()
})

router.afterEach(() => {
  const appStore = useAppStore()
  appStore.endLoading()
  document.getElementById('App-Container')?.focus()
})
