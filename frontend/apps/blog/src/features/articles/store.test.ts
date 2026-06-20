import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadPublicArticle, loadPublicArticles } from './api'
import { useArticleStore } from './store'
import { ApiError } from '@/shared/http/error'

vi.mock('./api', () => ({
  loadPublicArticles: vi.fn(),
  loadPublicArticle: vi.fn()
}))
const mockedLoad = vi.mocked(loadPublicArticles)
const mockedDetail = vi.mocked(loadPublicArticle)

const page = (
  records: Awaited<ReturnType<typeof loadPublicArticles>>['records']
) => ({
  records,
  total: records.length,
  page: 1,
  size: 12
})

describe('article store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedLoad.mockReset()
    mockedDetail.mockReset()
  })

  it('distinguishes ready and empty pages', async () => {
    mockedLoad
      .mockResolvedValueOnce(
        page([
          {
            id: '1',
            title: 'A',
            summary: null,
            categoryId: null,
            categoryName: null,
            slug: 'a',
            publishAt: '2026-06-15T10:00:00',
            coverUrl: null,
            commentCount: 0,
            tags: [],
            createdAt: '2026-06-15T10:00:00',
            locked: false
          }
        ])
      )
      .mockResolvedValueOnce(page([]))
    const store = useArticleStore()

    await store.load({ page: 1, size: 12, lang: 'en' })
    expect(store.status).toBe('ready')
    await store.load({ page: 2, size: 12, lang: 'en' })
    expect(store.status).toBe('empty')
  })

  it('enters error state and retries the last query', async () => {
    mockedLoad
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(page([]))
    const store = useArticleStore()

    await store.load({ page: 1, size: 12, lang: 'zh' })
    expect(store.status).toBe('error')
    await store.retry()
    expect(store.status).toBe('empty')
    expect(mockedLoad).toHaveBeenCalledTimes(2)
  })

  it('aborts the previous page request', async () => {
    let firstSignal: AbortSignal | undefined
    mockedLoad
      .mockImplementationOnce(params => {
        firstSignal = params.signal
        return new Promise(() => undefined)
      })
      .mockResolvedValueOnce(page([]))
    const store = useArticleStore()

    void store.load({ page: 1, size: 12, lang: 'zh' })
    await store.load({ page: 1, size: 12, lang: 'en' })

    expect(firstSignal?.aborted).toBe(true)
    expect(store.status).toBe('empty')
  })

  it.each([
    [new ApiError('locked', 403, '10003'), 'locked'],
    [new ApiError('missing', 404), 'notFound'],
    [new ApiError('server', 500), 'error']
  ] as const)('maps detail errors to %s', async (failure, expected) => {
    mockedDetail.mockRejectedValueOnce(failure)
    const store = useArticleStore()

    await store.loadDetail('1', 'en')

    expect(store.detailStatus).toBe(expected)
  })

  it('returns the canonical slug after loading detail', async () => {
    mockedDetail.mockResolvedValueOnce({
      id: '9007199254740993',
      title: 'Article',
      summary: null,
      body: '# Body',
      categoryId: null,
      categoryName: null,
      slug: 'canonical-slug',
      publishAt: '2026-06-15T10:00:00',
      coverUrl: null,
      commentCount: 0,
      tags: [],
      createdAt: '2026-06-15T09:00:00',
      updatedAt: '2026-06-15T11:00:00',
      locked: false
    })
    const store = useArticleStore()

    const slug = await store.loadDetail('9007199254740993', 'en')

    expect(slug).toBe('canonical-slug')
    expect(store.detailStatus).toBe('ready')
    expect(store.detail?.bodyHtml).toContain('<h1>Body</h1>')
  })
})
