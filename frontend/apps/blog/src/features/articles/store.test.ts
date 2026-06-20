import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadPublicArticles } from './api'
import { useArticleStore } from './store'

vi.mock('./api', () => ({ loadPublicArticles: vi.fn() }))
const mockedLoad = vi.mocked(loadPublicArticles)

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
})
