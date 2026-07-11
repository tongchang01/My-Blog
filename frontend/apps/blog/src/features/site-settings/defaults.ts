import { Theme, ThemeConfig, ThemeMenu } from '@/models/ThemeConfig.class'
import type { SiteSettingsViewModel } from './model'

export const createDefaultSiteSettings = (): SiteSettingsViewModel => {
  const themeConfig = new ThemeConfig()
  themeConfig.version = '2.5.3'
  themeConfig.menu = new ThemeMenu({
    Home: true,
    About: true,
    Archives: true,
    Tags: true,
    Categories: true,
    Links: true,
    MessageBoard: true
  })
  themeConfig.theme = new Theme({
    profile_shape: 'circle',
    feature: true,
    dark_mode: 'auto',
    gradient: {
      color_1: '#06b6d4',
      color_2: '#6366f1',
      color_3: '#8b5cf6'
    }
  })
  themeConfig.site.subtitle = '代码与生活'
  themeConfig.site.author = 'TYB'
  themeConfig.site.nick = 'BLOG'
  themeConfig.site.description = '一个写代码的人，偶尔记下一些想说的话。'
  // Author avatar is supplied by /api/public/author-profile.
  themeConfig.site.avatar = ''
  themeConfig.site.language = 'zh'
  themeConfig.site.multi_language = true
  themeConfig.site_meta.favicon = '/favicon.ico'

  return {
    siteTitle: 'MyBlog',
    siteSubtitle: themeConfig.site.subtitle,
    aboutMd: null,
    logoUrl: null,
    faviconUrl: themeConfig.site_meta.favicon,
    icpNo: null,
    spotifyPlaylistId: null,
    startedDate: null,
    themeConfig
  }
}

export const DEFAULT_SITE_SETTINGS = createDefaultSiteSettings()
