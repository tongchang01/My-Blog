import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('../../styles/theme-variables.scss', import.meta.url)),
  'utf8'
)
const messages = (language: string) =>
  JSON.parse(
    readFileSync(
      fileURLToPath(
        new URL(`../../locales/languages/${language}.json`, import.meta.url)
      ),
      'utf8'
    )
  )

describe('footer avatar', () => {
  it('renders without a translucent overlay', () => {
    const footerAvatarStyle = source.match(/\.footer-avatar\s*\{[^}]*\}/s)?.[0]
    expect(footerAvatarStyle).toBeDefined()
    expect(footerAvatarStyle).not.toContain('opacity-40')
  })

  it('labels the daily visitor metric accurately in every locale', () => {
    expect(messages('zh').settings['page-views-value']).toBe('累计浏览：')
    expect(messages('ja').settings['page-views-value']).toBe('累計閲覧数：')
    expect(messages('en').settings['page-views-value']).toBe('Total Page Views: ')
    expect(messages('zh').settings['unique_visitor-value']).toBe('今日访客：')
    expect(messages('ja').settings['unique_visitor-value']).toBe('本日の訪問者：')
    expect(messages('en').settings['unique_visitor-value']).toBe(
      "Today's Visitors: "
    )
  })
})
