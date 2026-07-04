import { beforeEach, describe, expect, it, vi } from 'vitest'
import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import { loadPublicArchives } from './api'

vi.mock('@/shared/http/client', () => ({
  requestApi: vi.fn()
}))

const mockedRequestApi = vi.mocked(requestApi)

describe('article api', () => {
  beforeEach(() => {
    mockedRequestApi.mockReset()
  })

  it('loads public archives with pagination and language parameters', async () => {
    mockedRequestApi.mockResolvedValueOnce({
      records: [],
      total: 0,
      page: 1,
      size: 12
    })

    await expect(
      loadPublicArchives({ page: 1, size: 12, lang: 'en' })
    ).resolves.toEqual({
      records: [],
      total: 0,
      page: 1,
      size: 12
    })
    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'GET',
      url: '/public/archives',
      params: { page: 1, size: 12, lang: 'en' },
      signal: undefined
    })
  })

  it('rejects empty archive responses', async () => {
    mockedRequestApi.mockResolvedValueOnce(null)

    await expect(
      loadPublicArchives({ page: 1, size: 12, lang: 'zh' })
    ).rejects.toBeInstanceOf(ApiError)
  })
})
