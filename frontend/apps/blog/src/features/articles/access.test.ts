// @vitest-environment happy-dom
import { afterEach, describe, expect, it } from 'vitest'
import {
  clearArticleAccessToken,
  loadArticleAccessToken,
  saveArticleAccessToken
} from './access'

describe('article access session storage', () => {
  afterEach(() => sessionStorage.clear())

  it('keeps a valid token only for the current browser session', () => {
    saveArticleAccessToken('1', {
      token: 'article-token',
      expiresAt: new Date(Date.now() + 60_000).toISOString()
    })

    expect(loadArticleAccessToken('1')).toBe('article-token')
    clearArticleAccessToken('1')
    expect(loadArticleAccessToken('1')).toBeNull()
  })

  it('removes an expired token', () => {
    saveArticleAccessToken('1', {
      token: 'expired-token',
      expiresAt: '2020-01-01T00:00:00Z'
    })

    expect(loadArticleAccessToken('1')).toBeNull()
  })
})
