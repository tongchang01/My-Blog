import { beforeEach, describe, expect, it, vi } from 'vitest'
import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import { loadPublicArchives, loadPublicArticles } from './api'

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

  it('loads public articles with keyword', async () => {
    mockedRequestApi.mockResolvedValueOnce({
      records: [],
      total: 0,
      page: 1,
      size: 8
    })

    await loadPublicArticles({
      page: 1,
      size: 8,
      lang: 'zh',
      keyword: 'Spring'
    })

    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'GET',
      url: '/public/articles',
      params: {
        page: 1,
        size: 8,
        lang: 'zh',
        categorySlug: undefined,
        tagSlug: undefined,
        keyword: 'Spring'
      },
      signal: undefined
    })
  })
})
