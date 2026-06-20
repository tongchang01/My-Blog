import { describe, expect, it } from 'vitest'
import { mapSiteSettings } from './mapper'
import { createDefaultSiteSettings } from './defaults'

describe('site settings mapper', () => {
  it('overlays backend fields without replacing frontend-owned settings', () => {
    const defaults = createDefaultSiteSettings()
    const mapped = mapSiteSettings({
      siteTitle: 'Backend title',
      siteSubtitle: null,
      aboutMd: '# About',
      logoUrl: null,
      faviconUrl: '/api/favicon.png',
      icpNo: null,
      spotifyPlaylistId: null
    })

    expect(mapped.siteTitle).toBe('Backend title')
    expect(mapped.siteSubtitle).toBeNull()
    expect(mapped.themeConfig.site.subtitle).toBe('')
    expect(mapped.themeConfig.site.author).toBe(
      defaults.themeConfig.site.author
    )
    expect(mapped.themeConfig.theme.gradient).toEqual(
      defaults.themeConfig.theme.gradient
    )
    expect(mapped.themeConfig.menu.menus).toEqual(
      defaults.themeConfig.menu.menus
    )
    expect(mapped.themeConfig.site_meta.favicon).toBe('/api/favicon.png')
  })
})
