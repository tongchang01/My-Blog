import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadPublicSiteConfig } from './api'
import { useSiteSettingsStore } from './store'

vi.mock('./api', () => ({ loadPublicSiteConfig: vi.fn() }))

const mockedLoad = vi.mocked(loadPublicSiteConfig)

describe('site settings store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedLoad.mockReset()
  })

  it('loads backend settings', async () => {
    mockedLoad.mockResolvedValue({
      siteTitle: 'MyBlog',
      siteSubtitle: 'Code and life',
      aboutMd: null,
      logoUrl: null,
      faviconUrl: null,
      icpNo: null,
      spotifyPlaylistId: null,
      startedDate: null
    })
    const store = useSiteSettingsStore()

    await store.load('en')

    expect(store.status).toBe('ready')
    expect(store.settings.siteTitle).toBe('MyBlog')
  })

  it('keeps renderable defaults when the API fails', async () => {
    mockedLoad.mockRejectedValue(new Error('offline'))
    const store = useSiteSettingsStore()

    await store.load('zh')

    expect(store.status).toBe('degraded')
    expect(store.settings.siteTitle).not.toBe('')
    expect(store.settings.themeConfig.site.author).toBe('')
    expect(store.settings.themeConfig.site.avatar).toBe('')
  })

  it('aborts the previous language request', async () => {
    let firstSignal: AbortSignal | undefined
    mockedLoad
      .mockImplementationOnce((_locale, signal) => {
        firstSignal = signal
        return new Promise(() => undefined)
      })
      .mockResolvedValueOnce({
        siteTitle: 'English title',
        siteSubtitle: null,
        aboutMd: null,
        logoUrl: null,
        faviconUrl: null,
        icpNo: null,
        spotifyPlaylistId: null,
        startedDate: null
      })
    const store = useSiteSettingsStore()

    void store.load('zh')
    await store.load('en')

    expect(firstSignal?.aborted).toBe(true)
    expect(store.settings.siteTitle).toBe('English title')
  })
})
