import type { PublicArticleUnlockDto } from './contract'

const STORAGE_PREFIX = 'myblog-article-access:'

const storageKey = (articleId: string): string =>
  `${STORAGE_PREFIX}${articleId}`

export const loadArticleAccessToken = (articleId: string): string | null => {
  if (typeof sessionStorage === 'undefined') return null
  const raw = sessionStorage.getItem(storageKey(articleId))
  if (!raw) return null

  try {
    const value = JSON.parse(raw) as Partial<PublicArticleUnlockDto>
    if (
      typeof value.token !== 'string' ||
      typeof value.expiresAt !== 'string' ||
      Number.isNaN(Date.parse(value.expiresAt)) ||
      Date.parse(value.expiresAt) <= Date.now()
    ) {
      sessionStorage.removeItem(storageKey(articleId))
      return null
    }
    return value.token
  } catch {
    sessionStorage.removeItem(storageKey(articleId))
    return null
  }
}

export const saveArticleAccessToken = (
  articleId: string,
  access: PublicArticleUnlockDto
): void => {
  if (typeof sessionStorage === 'undefined') return
  sessionStorage.setItem(storageKey(articleId), JSON.stringify(access))
}

export const clearArticleAccessToken = (articleId: string): void => {
  if (typeof sessionStorage === 'undefined') return
  sessionStorage.removeItem(storageKey(articleId))
}
