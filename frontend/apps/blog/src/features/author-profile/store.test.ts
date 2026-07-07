import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadPublicArticles } from '@/features/articles/api'
import { loadPublicCategories, loadPublicTags } from '@/features/taxonomy/api'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import { useAuthorProfileStore } from './store'

vi.mock('@/features/articles/api', () => ({ loadPublicArticles: vi.fn() }))
vi.mock('@/features/taxonomy/api', () => ({
  loadPublicCategories: vi.fn(),
  loadPublicTags: vi.fn()
}))

const mockedArticles = vi.mocked(loadPublicArticles)
const mockedCategories = vi.mocked(loadPublicCategories)
const mockedTags = vi.mocked(loadPublicTags)

describe('author profile store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedArticles.mockReset()
    mockedCategories.mockReset()
    mockedTags.mockReset()
  })

  it('builds the profile from V2 site settings and public counters', async () => {
    mockedArticles.mockResolvedValue({
      records: [],
      total: 8,
      page: 1,
      size: 1
    })
    mockedCategories.mockResolvedValue([
      { id: '1', name: 'Backend', slug: 'backend', articleCount: 2 }
    ])
    mockedTags.mockResolvedValue([
      { id: '2', name: 'Java', slug: 'java', articleCount: 3 },
      { id: '3', name: 'Vue', slug: 'vue', articleCount: 1 }
    ])
    const settings = useSiteSettingsStore()
    settings.settings.themeConfig.site.author = 'TYB'
    settings.settings.themeConfig.site.avatar = 'https://example.com/me.png'
    settings.settings.themeConfig.site.description = 'Code and life'
    const store = useAuthorProfileStore()

    await store.load('zh')

    expect(store.status).toBe('ready')
    expect(store.profile.name).toBe('TYB')
    expect(store.profile.avatar).toBe('https://example.com/me.png')
    expect(store.profile.description).toBe('Code and life')
    expect(store.profile.articleCount).toBe(8)
    expect(store.profile.categoryCount).toBe(1)
    expect(store.profile.tagCount).toBe(2)
  })

  it('keeps site settings when counters fail', async () => {
    mockedArticles.mockRejectedValue(new Error('offline'))
    mockedCategories.mockResolvedValue([])
    mockedTags.mockRejectedValue(new Error('offline'))
    const settings = useSiteSettingsStore()
    settings.settings.themeConfig.site.author = 'Fallback'
    const store = useAuthorProfileStore()

    await store.load('en')

    expect(store.status).toBe('degraded')
    expect(store.profile.name).toBe('Fallback')
    expect(store.profile.articleCount).toBe(0)
    expect(store.profile.categoryCount).toBe(0)
    expect(store.profile.tagCount).toBe(0)
  })
})
