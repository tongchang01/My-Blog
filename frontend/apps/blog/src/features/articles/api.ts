import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import type { PageResponse } from '@/shared/http/contract'
import type { SupportedLocale } from '@/shared/i18n/locale'
import type {
  PublicArchiveGroupDto,
  PublicArticleDetailDto,
  PublicArticleHomeDto,
  PublicArticleListItemDto
} from './contract'

export interface LoadPublicArticlesParams {
  page: number
  size: number
  lang: SupportedLocale
  categorySlug?: string
  tagSlug?: string
  signal?: AbortSignal
}

export interface LoadPublicHomeArticlesParams {
  size: number
  lang: SupportedLocale
  signal?: AbortSignal
}

export interface LoadPublicArchivesParams {
  page: number
  size: number
  lang: SupportedLocale
  signal?: AbortSignal
}

export const loadPublicArticles = async ({
  page,
  size,
  lang,
  categorySlug,
  tagSlug,
  signal
}: LoadPublicArticlesParams): Promise<
  PageResponse<PublicArticleListItemDto>
> => {
  const data = await requestApi<PageResponse<PublicArticleListItemDto>>({
    method: 'GET',
    url: '/public/articles',
    params: { page, size, lang, categorySlug, tagSlug },
    signal
  })
  if (data === null) throw new ApiError('Article page response is empty')
  return data
}

export const loadPublicHomeArticles = async ({
  size,
  lang,
  signal
}: LoadPublicHomeArticlesParams): Promise<PublicArticleHomeDto> => {
  const data = await requestApi<PublicArticleHomeDto>({
    method: 'GET',
    url: '/public/articles/home',
    params: { size, lang },
    signal
  })
  if (data === null) throw new ApiError('Article home response is empty')
  return data
}

export const loadPublicArchives = async ({
  page,
  size,
  lang,
  signal
}: LoadPublicArchivesParams): Promise<
  PageResponse<PublicArchiveGroupDto>
> => {
  const data = await requestApi<PageResponse<PublicArchiveGroupDto>>({
    method: 'GET',
    url: '/public/archives',
    params: { page, size, lang },
    signal
  })
  if (data === null) throw new ApiError('Archive page response is empty')
  return data
}

export const loadPublicArticle = async (
  id: string,
  lang: SupportedLocale,
  signal?: AbortSignal
): Promise<PublicArticleDetailDto> => {
  const data = await requestApi<PublicArticleDetailDto>({
    method: 'GET',
    url: `/public/articles/${encodeURIComponent(id)}`,
    params: { lang },
    signal
  })
  if (data === null) throw new ApiError('Article detail response is empty')
  return data
}
