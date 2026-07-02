import { useTaxonomyStore } from '@/features/taxonomy/store'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useCategoryStore = defineStore('categoryStore', () => {
  const taxonomyStore = useTaxonomyStore()
  const isLoaded = ref(false)
  const categories = computed(() => taxonomyStore.categories)

  const fetchCategories = async (locale: SupportedLocale = 'zh') => {
    isLoaded.value = false
    await taxonomyStore.loadCategories(locale)
    isLoaded.value = true
    return categories.value
  }

  return {
    isLoaded,
    categories,
    fetchCategories
  }
})
