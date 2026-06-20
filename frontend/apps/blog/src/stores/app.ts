import { defineStore } from 'pinia'
import Cookies from 'js-cookie'
import i18n from '@/locales/index'
import { Locales, ThemeConfig } from '@/models/ThemeConfig.class'
import { HexoConfig } from '@/models/HexoConfig.class'
import { fetchHexoConfig, fetchStatistic } from '@/api'
import { Statistic } from '@/models/Statistic.class'
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import { computed, ref } from 'vue'
import { resolveInitialLocale } from '@/shared/i18n/locale'

NProgress.configure({
  showSpinner: false,
  trickleSpeed: 100,
  parent: '#loading-bar-wrapper'
}) // NProgress Configuration

/** Fetching the default color-scheme from the OPSystem */
const getSystemMode = (): string => {
  // dark-mode media query matched or not
  const matched = window.matchMedia('(prefers-color-scheme: dark)').matches

  if (matched) return 'theme-dark'
  else return 'theme-light'
}

const setTheme = (theme: string) => {
  if (theme === 'theme-dark') {
    document.body.classList.remove('theme-light')
    document.body.classList.add('theme-dark')
  } else {
    document.body.classList.remove('theme-dark')
    document.body.classList.add('theme-light')
  }
}

/**
 * Storing the core data of the application
 */
export const useAppStore = defineStore('app', () => {
  const theme = ref(
    Cookies.get('theme') ? String(Cookies.get('theme')) : getSystemMode()
  )
  const locale = ref<Locales>(
    resolveInitialLocale(
      Cookies.get('locale') ?? localStorage.getItem('locale'),
      navigator.language
    )
  )
  const themeConfig = ref(new ThemeConfig())
  const hexoConfig = ref(new HexoConfig())
  const headerGradient = ref('')
  const statistic = ref(new Statistic())
  const appLoading = ref(false)
  const NPTimeout = ref(-1)
  const loadingTimeout = ref(-1)
  const configReady = ref(false)
  const openSearchModal = ref(false)

  const getTheme = computed(() => theme.value)
  const getAppLoading = computed(() => appLoading.value)

  const fetchConfig = async () => {
    configReady.value = false
    const { data } = await fetchHexoConfig()
    themeConfig.value = new ThemeConfig(data)
    hexoConfig.value = new HexoConfig(data)
    initializeTheme(themeConfig.value.theme.dark_mode)
    configReady.value = true
  }

  const fetchStat = async () => {
    const { data } = await fetchStatistic()
    return new Promise(resolve => {
      statistic.value = new Statistic(data)
      resolve(statistic.value)
    })
  }

  const initializeTheme = (isDarkMode?: boolean | string) => {
    if (!Cookies.get('theme') && isDarkMode !== 'auto') {
      theme.value = isDarkMode ? 'theme-dark' : 'theme-light'
      Cookies.set('theme', theme.value)
      setTheme(theme.value)
    }
    setTheme(theme.value)
  }

  const toggleTheme = (isDark?: boolean) => {
    theme.value =
      isDark === true || theme.value === 'theme-light'
        ? 'theme-dark'
        : 'theme-light'
    Cookies.set('theme', theme.value)
    setTheme(theme.value)
  }

  const changeLocale = (newLocale: Locales) => {
    Cookies.set('locale', newLocale)
    localStorage.setItem('locale', newLocale)
    locale.value = newLocale
    i18n.global.locale.value = newLocale
  }

  const startLoading = () => {
    if (appLoading.value === true) return
    if (NPTimeout.value !== -1) clearTimeout(NPTimeout.value)
    if (loadingTimeout.value !== -1) clearTimeout(loadingTimeout.value)

    NProgress.start()
    appLoading.value = true
  }

  const endLoading = () => {
    NPTimeout.value = window.setTimeout(() => {
      NProgress.done()
    }, 100)

    loadingTimeout.value = window.setTimeout(() => {
      appLoading.value = false
    }, 300)
  }

  const changeOpenModal = (status: boolean) => {
    openSearchModal.value = status
  }

  const handleEscKey = () => {
    if (openSearchModal.value) openSearchModal.value = false
  }

  const handleSearchOpen = () => {
    if (!openSearchModal.value) openSearchModal.value = true
  }

  return {
    theme,
    locale,
    themeConfig,
    hexoConfig,
    headerGradient,
    statistic,
    appLoading,
    NPTimeout,
    loadingTimeout,
    configReady,
    openSearchModal,
    getTheme,
    getAppLoading,
    fetchConfig,
    fetchStat,
    initializeTheme,
    toggleTheme,
    changeLocale,
    startLoading,
    endLoading,
    changeOpenModal,
    handleEscKey,
    handleSearchOpen
  }
})
