import { describe, expect, it } from 'vitest'
import { isSupportedLocale, resolveInitialLocale } from './locale'

describe('locale selection', () => {
  it('prefers a saved locale', () => {
    expect(resolveInitialLocale('ja', 'zh-CN')).toBe('ja')
  })

  it('migrates the legacy Chinese locale', () => {
    expect(resolveInitialLocale('zh-CN', 'en-US')).toBe('zh')
  })

  it.each([
    ['zh-CN', 'zh'],
    ['zh-TW', 'zh'],
    ['ja-JP', 'ja'],
    ['fr-FR', 'en']
  ])('maps %s to %s', (system, expected) => {
    expect(resolveInitialLocale(null, system)).toBe(expected)
  })

  it('rejects unsupported route params', () => {
    expect(isSupportedLocale('de')).toBe(false)
  })
})
