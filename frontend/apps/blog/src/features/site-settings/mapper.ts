import type { PublicSiteConfigDto } from './contract'
import { createDefaultSiteSettings } from './defaults'
import type { SiteSettingsViewModel } from './model'

export const mapSiteSettings = (
  dto: PublicSiteConfigDto
): SiteSettingsViewModel => {
  const settings = createDefaultSiteSettings()
  settings.siteTitle = dto.siteTitle
  settings.siteSubtitle = dto.siteSubtitle
  settings.aboutMd = dto.aboutMd
  settings.logoUrl = dto.logoUrl
  settings.faviconUrl = dto.faviconUrl
  settings.icpNo = dto.icpNo
  settings.spotifyPlaylistId = dto.spotifyPlaylistId

  settings.themeConfig.site.subtitle = dto.siteSubtitle ?? ''
  settings.themeConfig.site.logo = dto.logoUrl ?? ''
  settings.themeConfig.site_meta.favicon = dto.faviconUrl ?? ''
  settings.themeConfig.site.beian.number = dto.icpNo ?? ''
  return settings
}
