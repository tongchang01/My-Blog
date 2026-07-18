import { defineStore } from 'pinia'
import { ref } from 'vue'
import { normalizeApiError } from '@/shared/http/client'
import type { ApiError } from '@/shared/http/error'
import type { SupportedLocale } from '@/shared/i18n/locale'
import type {
  ArchivePageViewModel,
  ArticleDetailStatus,
  ArticleDetailViewModel,
  ArticleHomeViewModel,
  ArticleListStatus,
  ArticlePageViewModel
} from './model'
import {
  loadPublicArticle,
  loadPublicArchives,
  loadPublicHomeArticles,
  loadPublicArticles,
  unlockPublicArticle,
  type LoadPublicArchivesParams,
  type LoadPublicHomeArticlesParams,
  type LoadPublicArticlesParams
} from './api'
import {
  clearArticleAccessToken,
  loadArticleAccessToken,
  saveArticleAccessToken
} from './access'
import {
  mapArchivePage,
  mapArticleDetail,
  mapArticleHome,
  mapArticlePage
} from './mapper'

const emptyPage = (): ArticlePageViewModel => ({
  records: [],
  total: 0,
  page: 1,
  size: 12,
  pages: 0
})

const emptyHome = (): ArticleHomeViewModel => ({
  pinnedArticle: null,
  featuredArticles: [],
  articles: []
})

const emptyArchive = (): ArchivePageViewModel => ({
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
  const home = ref<ArticleHomeViewModel>(emptyHome())
  const homeStatus = ref<ArticleListStatus>('idle')
  const homeError = ref<ApiError | null>(null)
  const archive = ref<ArchivePageViewModel>(emptyArchive())
  const archiveStatus = ref<ArticleListStatus>('idle')
  const archiveError = ref<ApiError | null>(null)
  const detail = ref<ArticleDetailViewModel | null>(null)
  const detailStatus = ref<ArticleDetailStatus>('idle')
  const detailError = ref<ApiError | null>(null)
  let activeRequest: AbortController | null = null
  let activeHomeRequest: AbortController | null = null
  let activeArchiveRequest: AbortController | null = null
  let activeDetailRequest: AbortController | null = null
  let lastQuery: Omit<LoadPublicArticlesParams, 'signal'> | null = null
  let lastHomeQuery: Omit<LoadPublicHomeArticlesParams, 'signal'> | null = null
  let lastArchiveQuery: Omit<LoadPublicArchivesParams, 'signal'> | null = null
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
      const response = await loadPublicArticles({
        ...query,
        signal: request.signal
      })
      if (request.signal.aborted) return
      page.value = mapArticlePage(response, query.lang)
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

  const loadHome = async (
    query: Omit<LoadPublicHomeArticlesParams, 'signal'>
  ): Promise<void> => {
    activeHomeRequest?.abort()
    const request = new AbortController()
    activeHomeRequest = request
    lastHomeQuery = query
    homeStatus.value = 'loading'
    homeError.value = null

    try {
      home.value = mapArticleHome(
        await loadPublicHomeArticles({ ...query, signal: request.signal }),
        query.lang
      )
      homeStatus.value =
        home.value.pinnedArticle ||
        home.value.featuredArticles.length > 0 ||
        home.value.articles.length > 0
          ? 'ready'
          : 'empty'
    } catch (cause) {
      if (request.signal.aborted) return
      homeError.value = normalizeApiError(cause)
      homeStatus.value = 'error'
    } finally {
      if (activeHomeRequest === request) activeHomeRequest = null
    }
  }

  const retryHome = async (): Promise<void> => {
    if (lastHomeQuery) await loadHome(lastHomeQuery)
  }

  const loadArchives = async (
    query: Omit<LoadPublicArchivesParams, 'signal'>
  ): Promise<void> => {
    activeArchiveRequest?.abort()
    const request = new AbortController()
    activeArchiveRequest = request
    lastArchiveQuery = query
    archiveStatus.value = 'loading'
    archiveError.value = null

    try {
      archive.value = mapArchivePage(
        await loadPublicArchives({ ...query, signal: request.signal }),
        query.lang
      )
      archiveStatus.value =
        archive.value.records.length === 0 ? 'empty' : 'ready'
    } catch (cause) {
      if (request.signal.aborted) return
      archiveError.value = normalizeApiError(cause)
      archiveStatus.value = 'error'
    } finally {
      if (activeArchiveRequest === request) activeArchiveRequest = null
    }
  }

  const retryArchives = async (): Promise<void> => {
    if (lastArchiveQuery) await loadArchives(lastArchiveQuery)
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
    let accessToken: string | null = null

    try {
      accessToken = loadArticleAccessToken(id)
      detail.value = mapArticleDetail(
        await loadPublicArticle(id, lang, request.signal, accessToken),
        lang
      )
      detailStatus.value = 'ready'
      return detail.value.slug
    } catch (cause) {
      if (request.signal.aborted) return null
      const normalized = normalizeApiError(cause)
      if (accessToken && normalized.status === 403) clearArticleAccessToken(id)
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

  const unlockDetail = async (
    id: string,
    lang: SupportedLocale,
    password: string
  ): Promise<string | null> => {
    saveArticleAccessToken(id, await unlockPublicArticle(id, password))
    return loadDetail(id, lang)
  }

  const articleAccessToken = (id: string): string | null =>
    loadArticleAccessToken(id)

  return {
    page,
    status,
    error,
    home,
    homeStatus,
    homeError,
    archive,
    archiveStatus,
    archiveError,
    detail,
    detailStatus,
    detailError,
    load,
    retry,
    loadHome,
    retryHome,
    loadArchives,
    retryArchives,
    loadDetail,
    retryDetail,
    unlockDetail,
    articleAccessToken
  }
})
