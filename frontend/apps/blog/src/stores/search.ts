import { loadPublicArticles } from '@/features/articles/api'
import { mapArticlePage } from '@/features/articles/mapper'
import type { ArticleCardViewModel } from '@/features/articles/model'
import {
  RecentSearchResults,
  type SearchResultType
} from '@/models/Search.class'
import { normalizeApiError } from '@/shared/http/client'
import type { ApiError } from '@/shared/http/error'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useSearchStore = defineStore('searchStore', () => {
  const recentResults = ref(new RecentSearchResults())
  const openModal = ref(false)
  const searchResults = ref<ArticleCardViewModel[]>([])
  const searchStatus = ref<'idle' | 'loading' | 'ready' | 'empty' | 'error'>(
    'idle'
  )
  const searchError = ref<ApiError | null>(null)
  let activeSearchRequest: AbortController | null = null

  const results = computed(() => {
    return recentResults.value.getData()
  })

  const searchArticles = async (
    keyword: string,
    locale: SupportedLocale
  ): Promise<void> => {
    const normalizedKeyword = keyword.trim()
    activeSearchRequest?.abort()
    if (normalizedKeyword === '') {
      searchResults.value = []
      searchStatus.value = 'idle'
      searchError.value = null
      return
    }

    const request = new AbortController()
    activeSearchRequest = request
    searchStatus.value = 'loading'
    searchError.value = null

    try {
      const page = mapArticlePage(
        await loadPublicArticles({
          page: 1,
          size: 8,
          lang: locale,
          keyword: normalizedKeyword,
          signal: request.signal
        }),
        locale
      )
      searchResults.value = page.records
      searchStatus.value = page.records.length > 0 ? 'ready' : 'empty'
    } catch (cause) {
      if (request.signal.aborted) return
      searchResults.value = []
      searchError.value = normalizeApiError(cause)
      searchStatus.value = 'error'
    } finally {
      if (activeSearchRequest === request) activeSearchRequest = null
    }
  }

  const setOpenModal = (status: boolean) => {
    openModal.value = status
    let searchContainer: HTMLElement | null
    if (status === true) {
      document.body.classList.add('modal--active')
    } else {
      searchContainer = document.getElementById('search-modal')
      if (searchContainer) {
        searchContainer.style.animation =
          '0.85s ease 0s 1 normal none running opacity_hide'
      }
      document.body.classList.remove('modal--active')
    }
  }

  const addRecentSearch = (result: SearchResultType) => {
    recentResults.value.add(result)
  }

  return {
    recentResults,
    openModal,
    searchResults,
    searchStatus,
    searchError,
    results,
    searchArticles,
    setOpenModal,
    addRecentSearch
  }
})
