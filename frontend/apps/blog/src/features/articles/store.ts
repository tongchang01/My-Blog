import { defineStore } from 'pinia'
import { ref } from 'vue'
import { normalizeApiError } from '@/shared/http/client'
import type { ApiError } from '@/shared/http/error'
import type { SupportedLocale } from '@/shared/i18n/locale'
import type {
  ArticleDetailStatus,
  ArticleDetailViewModel,
  ArticleListStatus,
  ArticlePageViewModel
} from './model'
import {
  loadPublicArticle,
  loadPublicArticles,
  type LoadPublicArticlesParams
} from './api'
import { mapArticleDetail, mapArticlePage } from './mapper'

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
  const detail = ref<ArticleDetailViewModel | null>(null)
  const detailStatus = ref<ArticleDetailStatus>('idle')
  const detailError = ref<ApiError | null>(null)
  let activeRequest: AbortController | null = null
  let activeDetailRequest: AbortController | null = null
  let lastQuery: Omit<LoadPublicArticlesParams, 'signal'> | null = null
  let lastDetailQuery: { id: string; lang: SupportedLocale } | null = null

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

  const loadDetail = async (
    id: string,
    lang: SupportedLocale
  ): Promise<string | null> => {
    activeDetailRequest?.abort()
    const request = new AbortController()
    activeDetailRequest = request
    lastDetailQuery = { id, lang }
    detail.value = null
    detailStatus.value = 'loading'
    detailError.value = null

    try {
      detail.value = mapArticleDetail(
        await loadPublicArticle(id, lang, request.signal),
        lang
      )
      detailStatus.value = 'ready'
      return detail.value.slug
    } catch (cause) {
      if (request.signal.aborted) return null
      const normalized = normalizeApiError(cause)
      detailError.value = normalized
      if (normalized.status === 403 && normalized.code === '10003') {
        detailStatus.value = 'locked'
      } else if (normalized.status === 404) {
        detailStatus.value = 'notFound'
      } else {
        detailStatus.value = 'error'
      }
      return null
    } finally {
      if (activeDetailRequest === request) activeDetailRequest = null
    }
  }

  const retryDetail = async (): Promise<string | null> =>
    lastDetailQuery
      ? loadDetail(lastDetailQuery.id, lastDetailQuery.lang)
      : null

  return {
    page,
    status,
    error,
    detail,
    detailStatus,
    detailError,
    load,
    retry,
    loadDetail,
    retryDetail
  }
})
