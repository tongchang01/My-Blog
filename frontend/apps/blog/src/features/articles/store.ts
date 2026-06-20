import { defineStore } from 'pinia'
import { ref } from 'vue'
import { normalizeApiError } from '@/shared/http/client'
import type { ApiError } from '@/shared/http/error'
import type { ArticleListStatus, ArticlePageViewModel } from './model'
import { loadPublicArticles, type LoadPublicArticlesParams } from './api'
import { mapArticlePage } from './mapper'

const emptyPage = (): ArticlePageViewModel => ({
  records: [],
  total: 0,
  page: 1,
  size: 12,
  pages: 0
})

export const useArticleStore = defineStore('public-articles', () => {
  const page = ref<ArticlePageViewModel>(emptyPage())
  const status = ref<ArticleListStatus>('idle')
  const error = ref<ApiError | null>(null)
  let activeRequest: AbortController | null = null
  let lastQuery: Omit<LoadPublicArticlesParams, 'signal'> | null = null

  const load = async (
    query: Omit<LoadPublicArticlesParams, 'signal'>
  ): Promise<void> => {
    activeRequest?.abort()
    const request = new AbortController()
    activeRequest = request
    lastQuery = query
    status.value = 'loading'
    error.value = null

    try {
      page.value = mapArticlePage(
        await loadPublicArticles({ ...query, signal: request.signal }),
        query.lang
      )
      status.value = page.value.records.length === 0 ? 'empty' : 'ready'
    } catch (cause) {
      if (request.signal.aborted) return
      error.value = normalizeApiError(cause)
      status.value = 'error'
    } finally {
      if (activeRequest === request) activeRequest = null
    }
  }

  const retry = async (): Promise<void> => {
    if (lastQuery) await load(lastQuery)
  }

  return { page, status, error, load, retry }
})
