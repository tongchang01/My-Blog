import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loadPublicArticles } from '@/features/articles/api'
import { loadPublicCategories, loadPublicTags } from '@/features/taxonomy/api'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import type { SupportedLocale } from '@/shared/i18n/locale'
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

export const useAuthorProfileStore = defineStore('author-profile', () => {
  const profile = ref<AuthorProfileViewModel>(fromSiteSettings())
  const status = ref<AuthorProfileStatus>('idle')

  const load = async (locale: SupportedLocale): Promise<void> => {
    status.value = 'loading'
    const next = fromSiteSettings()
    const [articles, categories, tags] = await Promise.allSettled([
      loadPublicArticles({ page: 1, size: 1, lang: locale }),
      loadPublicCategories(locale),
      loadPublicTags(locale)
    ])

    if (articles.status === 'fulfilled') next.articleCount = articles.value.total
    if (categories.status === 'fulfilled') {
      next.categoryCount = categories.value.length
    }
    if (tags.status === 'fulfilled') next.tagCount = tags.value.length

    profile.value = next
    status.value =
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
