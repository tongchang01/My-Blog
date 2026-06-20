import type { SupportedLocale } from '@/shared/i18n/locale'
import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import type { PublicSiteConfigDto } from './contract'

export const loadPublicSiteConfig = async (
  locale: SupportedLocale,
  signal?: AbortSignal
): Promise<PublicSiteConfigDto> => {
  const data = await requestApi<PublicSiteConfigDto>({
    method: 'GET',
    url: '/public/site-config',
    params: { lang: locale },
    signal
  })
  if (data === null) throw new ApiError('Site config response is empty')
  return data
}
