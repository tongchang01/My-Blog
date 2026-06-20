import { defineStore } from 'pinia'
import i18n from '@/locales/index'
import { useAppStore } from './app'
import { useRoute } from 'vue-router'
import { computed, ref } from 'vue'

export const useMetaStore = defineStore('metaStore', () => {
  const title = ref('')
  const description = ref('')
  const links = ref<string[]>([])
  const scripts = ref<string[]>([])
  const meta = ref<string[]>([])

  const getTitle = computed(() => {
    const appStore = useAppStore()
    const route = useRoute()
    let subtitle = appStore.themeConfig.site.subtitle || 'Blog'
    if (route.name && route.name === 'home') {
      subtitle = appStore.themeConfig.site.slogan
    }
    if (title.value === '') return subtitle
    return `${title.value} | ${subtitle}`
  })

  const setTitle = (newTitle: string): void => {
    title.value = i18n.global.te(`menu.${newTitle}`)
      ? i18n.global.t(`menu.${newTitle}`)
      : newTitle
  }

  const addScripts = (...scriptArgs: string[]) => {
    const scriptsFlat = scriptArgs.flat(1)
    for (const script of scriptsFlat) {
      scripts.value.push(script)
    }
  }

  return {
    title,
    description,
    links,
    scripts,
    meta,
    getTitle,
    setTitle,
    addScripts
  }
})
