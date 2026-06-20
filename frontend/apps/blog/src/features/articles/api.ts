import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import type { PageResponse } from '@/shared/http/contract'
import type { SupportedLocale } from '@/shared/i18n/locale'
import type { PublicArticleListItemDto } from './contract'

export interface LoadPublicArticlesParams {
  page: number
  size: number
  lang: SupportedLocale
  signal?: AbortSignal
}

export const loadPublicArticles = async ({
  page,
  size,
  lang,
  signal
}: LoadPublicArticlesParams): Promise<
  PageResponse<PublicArticleListItemDto>
> => {
  const data = await requestApi<PageResponse<PublicArticleListItemDto>>({
    method: 'GET',
    url: '/public/articles',
    params: { page, size, lang },
    signal
  })
  if (data === null) throw new ApiError('Article page response is empty')
  return data
}
