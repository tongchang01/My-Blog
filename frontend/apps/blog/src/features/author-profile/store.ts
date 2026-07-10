import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loadPublicArticles } from '@/features/articles/api'
import { loadPublicCategories, loadPublicTags } from '@/features/taxonomy/api'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import { Social } from '@/models/ThemeConfig.class'
import type { SupportedLocale } from '@/shared/i18n/locale'
import {
  loadPublicAuthorProfile,
  type PublicAuthorProfileDto
} from './api'
import type { AuthorProfileStatus, AuthorProfileViewModel } from './model'

const fromSiteSettings = (): AuthorProfileViewModel => {
  const settings = useSiteSettingsStore().settings
  return {
    name: settings.themeConfig.site.author,
    avatar: settings.themeConfig.site.avatar,
    description: settings.themeConfig.site.description,
    socials: settings.themeConfig.socials,
    wordCount: 0,
    articleCount: 0,
    categoryCount: 0,
    tagCount: 0
  }
}

const localizedBio = (
  dto: PublicAuthorProfileDto,
  locale: SupportedLocale
): string | null => {
  if (locale === 'ja') return dto.bioJa || dto.bioZh || dto.bioEn
  if (locale === 'en') return dto.bioEn || dto.bioZh || dto.bioJa
  return dto.bioZh || dto.bioEn || dto.bioJa
}

const applyAuthorProfile = (
  next: AuthorProfileViewModel,
  dto: PublicAuthorProfileDto,
  locale: SupportedLocale
): void => {
  next.name = dto.nickname || next.name
  next.avatar = dto.avatarUrl || next.avatar
  next.description = localizedBio(dto, locale) || next.description
  next.socials = new Social({
    github: dto.githubUrl || '',
    twitter: dto.twitterUrl || '',
    zhihu: dto.zhihuUrl || '',
    juejin: dto.juejinUrl || ''
  })
}

export const useAuthorProfileStore = defineStore('author-profile', () => {
  const profile = ref<AuthorProfileViewModel>(fromSiteSettings())
  const status = ref<AuthorProfileStatus>('idle')

  const load = async (locale: SupportedLocale): Promise<void> => {
    status.value = 'loading'
    const next = fromSiteSettings()
    const [author, articles, categories, tags] = await Promise.allSettled([
      loadPublicAuthorProfile(),
      loadPublicArticles({ page: 1, size: 1, lang: locale }),
      loadPublicCategories(locale),
      loadPublicTags(locale)
    ])

    if (author.status === 'fulfilled') {
      applyAuthorProfile(next, author.value, locale)
    }
    if (articles.status === 'fulfilled') next.articleCount = articles.value.total
    if (categories.status === 'fulfilled') {
      next.categoryCount = categories.value.length
    }
    if (tags.status === 'fulfilled') next.tagCount = tags.value.length

    profile.value = next
    status.value =
      author.status === 'fulfilled' &&
      articles.status === 'fulfilled' &&
      categories.status === 'fulfilled' &&
      tags.status === 'fulfilled'
        ? 'ready'
        : 'degraded'
  }

  return {
    profile,
    status,
    load
  }
})
