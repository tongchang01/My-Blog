import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { createDefaultSiteSettings } from './defaults'
import { mapSiteSettings } from './mapper'
import type { SiteSettingsStatus, SiteSettingsViewModel } from './model'
import { loadPublicSiteConfig } from './api'

export const useSiteSettingsStore = defineStore('site-settings', () => {
  const settings = ref<SiteSettingsViewModel>(createDefaultSiteSettings())
  const status = ref<SiteSettingsStatus>('idle')
  let activeRequest: AbortController | null = null

  const load = async (locale: SupportedLocale): Promise<void> => {
    activeRequest?.abort()
    const request = new AbortController()
    activeRequest = request
    status.value = 'loading'

    try {
      settings.value = mapSiteSettings(
        await loadPublicSiteConfig(locale, request.signal)
      )
      status.value = 'ready'
    } catch {
      if (request.signal.aborted) return
      settings.value = createDefaultSiteSettings()
      status.value = 'degraded'
    } finally {
      if (activeRequest === request) activeRequest = null
    }
  }

  return { settings, status, load }
})
