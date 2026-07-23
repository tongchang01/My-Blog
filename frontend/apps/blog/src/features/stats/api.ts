import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import type { SupportedLocale } from '@/shared/i18n/locale'

export interface SiteStatsSummaryDto {
  todayUv: number
  totalPv: number
}

export interface RecordPageViewPayload {
  articleId?: number
  lang: SupportedLocale
}

export const loadSiteStatsSummary = async (): Promise<SiteStatsSummaryDto> => {
  const data = await requestApi<SiteStatsSummaryDto>({
    method: 'GET',
    url: '/public/stats/site-summary'
  })
  if (data === null) throw new ApiError('Site stats summary is empty')
  return data
}

export const recordPageView = async (
  payload: RecordPageViewPayload
): Promise<void> => {
  await requestApi<null>({
    method: 'POST',
    url: '/public/stats/page-views',
    data: payload
  })
}
