import type { ThemeConfig } from '@/models/ThemeConfig.class'

export type SiteSettingsStatus = 'idle' | 'loading' | 'ready' | 'degraded'

export interface SiteSettingsViewModel {
  siteTitle: string
  siteSubtitle: string | null
  aboutMd: string | null
  logoUrl: string | null
  faviconUrl: string | null
  icpNo: string | null
  spotifyPlaylistId: string | null
  themeConfig: ThemeConfig
}
