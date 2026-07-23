import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { loadPublicCategories, loadPublicTags } from './api'
import { mapCategories, mapTags } from './mapper'
import type { TaxonomyItemViewModel, TaxonomyStatus } from './model'

export const useTaxonomyStore = defineStore('taxonomy', () => {
  const categories = ref<TaxonomyItemViewModel[]>([])
  const tags = ref<TaxonomyItemViewModel[]>([])
  const categoryStatus = ref<TaxonomyStatus>('idle')
  const tagStatus = ref<TaxonomyStatus>('idle')
  let activeCategoryRequest: AbortController | null = null
  let activeTagRequest: AbortController | null = null

  const loadCategories = async (locale: SupportedLocale): Promise<void> => {
    activeCategoryRequest?.abort()
    const request = new AbortController()
    activeCategoryRequest = request
    categoryStatus.value = 'loading'

    try {
      categories.value = mapCategories(
        await loadPublicCategories(locale, request.signal)
      )
      categoryStatus.value = categories.value.length > 0 ? 'ready' : 'empty'
    } catch {
      if (request.signal.aborted) return
      categories.value = []
      categoryStatus.value = 'error'
    } finally {
      if (activeCategoryRequest === request) activeCategoryRequest = null
    }
  }

  const loadTags = async (locale: SupportedLocale): Promise<void> => {
    activeTagRequest?.abort()
    const request = new AbortController()
    activeTagRequest = request
    tagStatus.value = 'loading'

    try {
      tags.value = mapTags(await loadPublicTags(locale, request.signal))
      tagStatus.value = tags.value.length > 0 ? 'ready' : 'empty'
    } catch {
      if (request.signal.aborted) return
      tags.value = []
      tagStatus.value = 'error'
    } finally {
      if (activeTagRequest === request) activeTagRequest = null
    }
  }

  return {
    categories,
    tags,
    categoryStatus,
    tagStatus,
    loadCategories,
    loadTags
  }
})
