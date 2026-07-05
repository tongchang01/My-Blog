import { beforeEach, describe, expect, it, vi } from 'vitest'
import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import { loadSiteStatsSummary, recordPageView } from './api'

vi.mock('@/shared/http/client', () => ({
  requestApi: vi.fn()
}))

const mockedRequestApi = vi.mocked(requestApi)

describe('stats api', () => {
  beforeEach(() => {
    mockedRequestApi.mockReset()
  })

  it('loads public site stats summary', async () => {
    mockedRequestApi.mockResolvedValueOnce({ todayUv: 3, totalPv: 20 })

    await expect(loadSiteStatsSummary()).resolves.toEqual({
      todayUv: 3,
      totalPv: 20
    })
    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'GET',
      url: '/public/stats/site-summary'
    })
  })

  it('rejects empty stats summary responses', async () => {
    mockedRequestApi.mockResolvedValueOnce(null)

    await expect(loadSiteStatsSummary()).rejects.toBeInstanceOf(ApiError)
  })

  it('records page views', async () => {
    mockedRequestApi.mockResolvedValueOnce(null)

    await recordPageView({ articleId: 0, lang: 'zh' })

    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'POST',
      url: '/public/stats/page-views',
      data: { articleId: 0, lang: 'zh' }
    })
  })
})
