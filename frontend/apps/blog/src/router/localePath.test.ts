import { describe, expect, it } from 'vitest'
import { localizedPath } from './localePath'

describe('localized menu paths', () => {
  it.each([
    ['/', 'zh', '/zh'],
    ['/archives', 'ja', '/ja/archives'],
    ['/message-board', 'en', '/en/message-board']
  ] as const)('maps %s with %s to %s', (path, locale, expected) => {
    expect(localizedPath(path, locale)).toBe(expected)
  })
})
