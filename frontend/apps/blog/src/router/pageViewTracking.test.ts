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

  it('resolves non-article pages without an article id', () => {
    expect(
      resolvePageViewPayload({
        name: 'home',
        params: { lang: 'zh' }
      } as never)
    ).toEqual({ lang: 'zh' })
  })

  it('requires public static pages to carry their locale', () => {
    expect(
      resolvePageViewPayload({ name: 'about', params: { lang: 'ja' } } as never)
    ).toEqual({ lang: 'ja' })
    expect(
      resolvePageViewPayload({ name: 'about', params: {} } as never)
    ).toBeNull()
  })

  it('ignores routes without supported languages', () => {
    expect(
      resolvePageViewPayload({
        name: 'legacy',
        params: {}
      } as never)
    ).toBeNull()
    expect(
      resolvePageViewPayload({
        name: 'not-found',
        params: { lang: 'zh', pathMatch: ['missing'] }
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

    expect(mockedRecordPageView).toHaveBeenCalledWith({ lang: 'ja' })
  })

  it('does not record canonical slug replacement twice', () => {
    mockedRecordPageView.mockResolvedValue(undefined as never)
    let hook: ((to: unknown, from: unknown) => void) | undefined
    installPageViewTracking({
      afterEach: vi.fn(callback => {
        hook = callback as (to: unknown, from: unknown) => void
      })
    } as never)
    const initial = {
      name: 'article-detail',
      params: { lang: 'zh', id: '42' }
    }
    const canonical = {
      name: 'article-detail',
      params: { lang: 'zh', id: '42', slug: 'canonical' }
    }

    hook?.(initial, { name: 'home', params: { lang: 'zh' } })
    hook?.(canonical, initial)

    expect(mockedRecordPageView).toHaveBeenCalledTimes(1)
  })
})
