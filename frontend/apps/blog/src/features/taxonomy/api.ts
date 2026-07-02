import { ApiError } from '@/shared/http/error'
import { requestApi } from '@/shared/http/client'
import type { SupportedLocale } from '@/shared/i18n/locale'
import type { PublicTaxonomyDto } from './contract'

export const loadPublicCategories = async (
  locale: SupportedLocale,
  signal?: AbortSignal
): Promise<PublicTaxonomyDto[]> => {
  const data = await requestApi<PublicTaxonomyDto[]>({
    method: 'GET',
    url: '/public/categories',
    params: { lang: locale },
    signal
  })
  if (data === null) throw new ApiError('Category response is empty')
  return data
}

export const loadPublicTags = async (
  locale: SupportedLocale,
  signal?: AbortSignal
): Promise<PublicTaxonomyDto[]> => {
  const data = await requestApi<PublicTaxonomyDto[]>({
    method: 'GET',
    url: '/public/tags',
    params: { lang: locale },
    signal
  })
  if (data === null) throw new ApiError('Tag response is empty')
  return data
}
