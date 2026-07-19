import { describe, expect, it } from 'vitest'
import { mapSiteSettings } from './mapper'
import { createDefaultSiteSettings } from './defaults'

describe('site settings mapper', () => {
  it('leaves author fields to the author profile store', () => {
    const site = createDefaultSiteSettings().themeConfig.site
    expect(site.author).toBe('')
    expect(site.description).toBe('')
    expect(site.avatar).toBe('')
  })

  it('overlays backend fields without replacing frontend-owned settings', () => {
    const defaults = createDefaultSiteSettings()
    const mapped = mapSiteSettings({
      siteTitle: 'Backend title',
      siteSubtitle: null,
      aboutMd: '# About',
      logoUrl: null,
      faviconUrl: '/api/favicon.png',
      icpNo: null,
      spotifyPlaylistId: null,
      startedDate: '2024-01-02'
    })

    expect(mapped.siteTitle).toBe('Backend title')
    expect(mapped.siteSubtitle).toBeNull()
    expect(mapped.themeConfig.site.subtitle).toBe('')
    expect(mapped.themeConfig.theme.gradient).toEqual(
      defaults.themeConfig.theme.gradient
    )
    expect(mapped.themeConfig.menu.menus).toEqual(
      defaults.themeConfig.menu.menus
    )
    expect(mapped.themeConfig.site_meta.favicon).toBe('/api/favicon.png')
    expect(mapped.themeConfig.site.started_date).toBe('2024-01-02')
  })
})
