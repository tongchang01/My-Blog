import { useTaxonomyStore } from '@/features/taxonomy/store'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useTagStore = defineStore('tagStore', () => {
  const taxonomyStore = useTaxonomyStore()
  const isLoaded = ref(false)
  const tags = computed(() => taxonomyStore.tags)

  const fetchAllTagsData = async (locale: SupportedLocale = 'zh') => {
    isLoaded.value = false
    await taxonomyStore.loadTags(locale)
    isLoaded.value = true
    return tags.value
  }

  const fetchTagsByCount = async (
    count: number,
    locale: SupportedLocale = 'zh'
  ) => {
    isLoaded.value = false
    await taxonomyStore.loadTags(locale)
    isLoaded.value = true
    return tags.value.slice(0, count)
  }

  return {
    isLoaded,
    tags,
    fetchAllTags: fetchAllTagsData,
    fetchTagsByCount
  }
})
