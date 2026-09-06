// @vitest-environment happy-dom
// @vitest-environment-options {"settings":{"disableIframePageLoading":true}}
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import appSource from '@/App.vue?raw'
import { createApp, h, nextTick, type App } from 'vue'
import { createPinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import zh from '@/locales/languages/zh.json'
import en from '@/locales/languages/en.json'
import ja from '@/locales/languages/ja.json'
import SpotifyPlayer from './SpotifyPlayer.vue'

let app: App | undefined
let container: HTMLDivElement
let consoleError: ReturnType<typeof vi.spyOn>

beforeEach(() => {
  // happy-dom 会报告主动禁用的远端 iframe；其他错误仍让测试失败。
  consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
})

afterEach(() => {
  app?.unmount()
  app = undefined
  container?.remove()
  const errors = consoleError.mock.calls
  vi.restoreAllMocks()
  for (const [error] of errors) {
    expect(String(error)).toMatch(
      /^NotSupportedError: Failed to load iframe page "https:\/\/open\.spotify\.com\/embed\/playlist\/.*"\. Iframe page loading is disabled\.$/
    )
  }
})

const mountPlayer = async (id: string | null = 'testPlaylist') => {
  const pinia = createPinia()
  const settings = useSiteSettingsStore(pinia)
  settings.settings.spotifyPlaylistId = id
  settings.status = 'ready'
  const i18n = createI18n({
    legacy: false,
    locale: 'zh',
    messages: { zh, en, ja }
  })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { render: () => h('main', 'Home') } },
      { path: '/ja/about', component: { render: () => h('main', 'About') } }
    ]
  })
  await router.push('/')
  container = document.createElement('div')
  document.body.append(container)
  app = createApp({ render: () => h('div', [h(RouterView), h(SpotifyPlayer)]) })
  app.use(pinia).use(i18n).use(router).mount(container)
  await nextTick()
  return { settings, i18n, router }
}

const togglePanel = async (open: boolean) => {
  const details = container.querySelector('details')!
  details.open = open
  details.dispatchEvent(new Event('toggle'))
  await nextTick()
  return details
}

const clickButton = async (label: string) => {
  const button = [...container.querySelectorAll('button')].find(
    item => item.textContent === label
  )!
  expect(button).toBeDefined()
  button.click()
  await nextTick()
}

describe('Spotify official player lifecycle', () => {
  it('mounts once outside the route view in the real application', () => {
    const template = appSource.split('</template>')[0]
    expect(template.indexOf('<SpotifyPlayer />')).toBeGreaterThan(
      template.indexOf('</router-view>')
    )
    expect(template.match(/<SpotifyPlayer \/>/g)).toHaveLength(1)
  })
  it('does not load Spotify until the reader opens the panel', async () => {
    await mountPlayer()
    expect(container.querySelector('iframe')).toBeNull()
    await togglePanel(true)
    const frame = container.querySelector('iframe')!
    expect(frame.src).toBe(
      'https://open.spotify.com/embed/playlist/testPlaylist?utm_source=generator'
    )
    expect(frame.title).toBe(zh.music['player-title'])
    expect(
      frame
        .getAttribute('allow')
        ?.split(';')
        .map(value => value.trim())
        .filter(Boolean)
    ).toEqual([
      'autoplay',
      'clipboard-write',
      'encrypted-media',
      'fullscreen',
      'picture-in-picture'
    ])
    expect(frame.hasAttribute('allowfullscreen')).toBe(true)
    expect(frame.getAttribute('height')).toBe('352')
    expect(frame.src).not.toContain('autoplay=')
    expect(container.querySelector('a')?.rel).toContain('noopener')
  })

  it('keeps the same iframe when collapsing, reopening and navigating', async () => {
    const { router, settings, i18n } = await mountPlayer()
    await togglePanel(true)
    const frame = container.querySelector('iframe')
    await togglePanel(false)
    expect(container.querySelector('iframe')).toBe(frame)
    await togglePanel(true)
    await router.push('/ja/about')
    i18n.global.locale.value = 'ja'
    settings.status = 'loading'
    await nextTick()
    settings.status = 'ready'
    await nextTick()
    expect(container.querySelector('main')?.textContent).toBe('About')
    expect(container.querySelector('iframe')).toBe(frame)
    expect(frame?.title).toBe(ja.music['player-title'])
    expect(container.querySelector('details')?.open).toBe(true)
  })

  it('does not destroy active playback when a locale configuration request fails', async () => {
    const { settings } = await mountPlayer()
    await togglePanel(true)
    const frame = container.querySelector('iframe')
    settings.settings.spotifyPlaylistId = null
    settings.status = 'degraded'
    await nextTick()
    expect(container.querySelector('iframe')).toBe(frame)
    settings.status = 'ready'
    await nextTick()
    expect(container.querySelector('details')).toBeNull()
    expect(container.querySelector('iframe')).toBeNull()
  })

  it('destroys playback only when the reader explicitly stops it', async () => {
    await mountPlayer()
    await togglePanel(true)
    const frame = container.querySelector('iframe')
    await clickButton(zh.music.stop)
    expect(container.querySelector('details')?.open).toBe(false)
    expect(container.querySelector('iframe')).toBeNull()
    await togglePanel(true)
    expect(container.querySelector('iframe')).not.toBe(frame)
  })

  it('allows an explicit reload without pretending iframe load means successful playback', async () => {
    await mountPlayer()
    await togglePanel(true)
    const frame = container.querySelector('iframe')!
    frame.dispatchEvent(new Event('load'))
    expect(container.textContent).toContain(zh.music['unavailable-help'])
    await clickButton(zh.music.reload)
    expect(container.querySelector('iframe')).not.toBe(frame)
    expect(container.querySelector('iframe')?.src).toBe(frame.src)
  })

  it('hides an unconfigured entry and requires a new user action after changing playlists', async () => {
    const { settings } = await mountPlayer(null)
    expect(container.querySelector('details')).toBeNull()
    settings.settings.spotifyPlaylistId = 'first'
    await nextTick()
    await togglePanel(true)
    settings.settings.spotifyPlaylistId = 'second'
    await nextTick()
    expect(container.querySelector('details')?.open).toBe(false)
    expect(container.querySelector('iframe')).toBeNull()
    await togglePanel(true)
    expect(container.querySelector('iframe')?.src).toContain(
      '/playlist/second?'
    )
  })

  it('never treats configuration as a URL or HTML fragment', async () => {
    await mountPlayer('https://other.example/?x=<script>')
    await togglePanel(true)
    const frame = container.querySelector('iframe')!
    expect(new URL(frame.src).origin).toBe('https://open.spotify.com')
    expect(new URL(frame.src).search).toBe('?utm_source=generator')
    expect(container.querySelector('script')).toBeNull()
  })

  it('provides the same complete translation keys in all three languages', () => {
    expect(Object.keys(en.music)).toEqual(Object.keys(zh.music))
    expect(Object.keys(ja.music)).toEqual(Object.keys(zh.music))
    for (const messages of [zh.music, en.music, ja.music]) {
      expect(Object.values(messages).every(value => value.length > 0)).toBe(
        true
      )
    }
  })
})
