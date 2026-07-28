import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = dirname(fileURLToPath(import.meta.url))
const indexSource = readFileSync(resolve(currentDir, 'index.vue'), 'utf8')
const detailSource = readFileSync(resolve(currentDir, 'post/[slug].vue'), 'utf8')
const messages = ['zh', 'ja', 'en'].map(locale =>
  JSON.parse(
    readFileSync(
      resolve(currentDir, `../locales/languages/${locale}.json`),
      'utf8'
    )
  )
)

describe('user-visible page translations', () => {
  it('uses i18n keys instead of language-specific text branches', () => {
    expect(indexSource).toContain("t('home.empty-public-articles')")
    expect(detailSource).not.toMatch(/lang === ['\"](?:zh|ja|en)['\"]/)

    for (const key of [
      'password-protected',
      'not-found',
      'password-placeholder',
      'unlock',
      'unlocking',
      'unlock-error'
    ]) {
      expect(detailSource).toContain(`t('article.${key}')`)
    }
  })

  it('defines every page and copy-attribution label in all public locales', () => {
    for (const locale of messages) {
      expect(locale.home['empty-public-articles']).toBeTruthy()
      expect(Object.values(locale.article)).toHaveLength(6)
      expect(locale['copy-protection'].author).toBeTruthy()
      expect(locale['copy-protection'].link).toBeTruthy()
      expect(locale.markdown['reading-time']).toContain('{minutes}')
      expect(locale.markdown['copy-code']).toBeTruthy()
      expect(Object.values(locale.markdown.mermaid)).toHaveLength(10)
    }
  })
})
