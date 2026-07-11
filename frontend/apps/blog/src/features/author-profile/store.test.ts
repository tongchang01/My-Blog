import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadPublicArticles } from '@/features/articles/api'
import { loadPublicCategories, loadPublicTags } from '@/features/taxonomy/api'
import { loadPublicAuthorProfile } from './api'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import { useAuthorProfileStore } from './store'

vi.mock('@/features/articles/api', () => ({ loadPublicArticles: vi.fn() }))
vi.mock('./api', () => ({ loadPublicAuthorProfile: vi.fn() }))
vi.mock('@/features/taxonomy/api', () => ({
  loadPublicCategories: vi.fn(),
  loadPublicTags: vi.fn()
}))

const mockedAuthor = vi.mocked(loadPublicAuthorProfile)
const mockedArticles = vi.mocked(loadPublicArticles)
const mockedCategories = vi.mocked(loadPublicCategories)
const mockedTags = vi.mocked(loadPublicTags)

describe('author profile store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedAuthor.mockReset()
    mockedArticles.mockReset()
    mockedCategories.mockReset()
    mockedTags.mockReset()
  })

  it('builds the profile from public author profile and counters', async () => {
    mockedAuthor.mockResolvedValue({
      nickname: '三钻',
      avatarUrl: 'https://example.com/author.png',
      bioZh: '中文作者简介',
      bioJa: '日本語プロフィール',
      bioEn: 'English author bio',
      location: 'Tokyo',
      website: 'https://example.com',
      emailPublic: 'public@example.com',
      githubUrl: 'https://github.com/tyb',
      twitterUrl: 'https://x.com/tyb',
      linkedinUrl: 'https://linkedin.com/in/tyb',
      zhihuUrl: 'https://zhihu.com/people/tyb',
      qiitaUrl: 'https://qiita.com/tyb',
      juejinUrl: 'https://juejin.cn/user/tyb'
    })
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
    expect(store.profile.name).toBe('三钻')
    expect(store.profile.avatar).toBe('https://example.com/author.png')
    expect(store.profile.description).toBe('中文作者简介')
    expect(store.profile.socials.github).toBe('https://github.com/tyb')
    expect(store.profile.socials.twitter).toBe('https://x.com/tyb')
    expect(store.profile.socials.linkedin).toBe('https://linkedin.com/in/tyb')
    expect(store.profile.socials.qiita).toBe('https://qiita.com/tyb')
    expect(store.profile.socials.website).toBe('https://example.com')
    expect(store.profile.socials.email).toBe('public@example.com')
    expect(store.profile.socials.zhihu).toBe('https://zhihu.com/people/tyb')
    expect(store.profile.socials.juejin).toBe('https://juejin.cn/user/tyb')
    expect(settings.settings.themeConfig.site.author).toBe('三钻')
    expect(settings.settings.themeConfig.site.avatar).toBe(
      'https://example.com/author.png'
    )
    expect(settings.settings.themeConfig.site.description).toBe('中文作者简介')
    expect(store.profile.articleCount).toBe(8)
    expect(store.profile.categoryCount).toBe(1)
    expect(store.profile.tagCount).toBe(2)
  })

  it('deduplicates concurrent loads for the same locale', async () => {
    mockedAuthor.mockResolvedValue({
      nickname: 'Author',
      avatarUrl: null,
      bioZh: 'Bio',
      bioJa: null,
      bioEn: null,
      location: null,
      website: null,
      emailPublic: null,
      githubUrl: null,
      twitterUrl: null,
      linkedinUrl: null,
      zhihuUrl: null,
      qiitaUrl: null,
      juejinUrl: null
    })
    mockedArticles.mockResolvedValue({
      records: [],
      total: 1,
      page: 1,
      size: 1
    })
    mockedCategories.mockResolvedValue([])
    mockedTags.mockResolvedValue([])
    const store = useAuthorProfileStore()

    await Promise.all([store.load('zh'), store.load('zh')])

    expect(mockedAuthor).toHaveBeenCalledTimes(1)
    expect(mockedArticles).toHaveBeenCalledTimes(1)
    expect(mockedCategories).toHaveBeenCalledTimes(1)
    expect(mockedTags).toHaveBeenCalledTimes(1)
  })

  it('keeps site settings when author profile and counters fail', async () => {
    mockedAuthor.mockRejectedValue(new Error('offline'))
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
