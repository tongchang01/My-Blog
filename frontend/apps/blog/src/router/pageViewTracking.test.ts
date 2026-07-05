import { beforeEach, describe, expect, it, vi } from 'vitest'
import { recordPageView } from '@/features/stats/api'
import {
  installPageViewTracking,
  resolvePageViewPayload
} from './pageViewTracking'

vi.mock('@/features/stats/api', () => ({
  recordPageView: vi.fn()
}))

const mockedRecordPageView = vi.mocked(recordPageView)

describe('page view tracking', () => {
  beforeEach(() => {
    mockedRecordPageView.mockReset()
  })

  it('resolves article page payloads', () => {
    expect(
      resolvePageViewPayload({
        name: 'article-detail',
        params: { lang: 'en', id: '42' }
      } as never)
    ).toEqual({ articleId: 42, lang: 'en' })
  })

  it('resolves non-article pages to articleId 0', () => {
    expect(
      resolvePageViewPayload({
        name: 'home',
        params: { lang: 'zh' }
      } as never)
    ).toEqual({ articleId: 0, lang: 'zh' })
  })

  it('ignores routes without supported languages', () => {
    expect(
      resolvePageViewPayload({
        name: 'legacy',
        params: {}
      } as never)
    ).toBeNull()
  })

  it('records after successful navigations and swallows failures', async () => {
    mockedRecordPageView.mockRejectedValueOnce(new Error('offline'))
    let hook: ((to: unknown) => void) | undefined
    installPageViewTracking({
      afterEach: vi.fn(callback => {
        hook = callback as (to: unknown) => void
      })
    } as never)

    hook?.({ name: 'home', params: { lang: 'ja' } })
    await Promise.resolve()

    expect(mockedRecordPageView).toHaveBeenCalledWith({
      articleId: 0,
      lang: 'ja'
    })
  })
})
