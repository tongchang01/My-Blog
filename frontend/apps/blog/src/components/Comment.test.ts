import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Comment.vue'), 'utf8')
const localeDir = resolve(currentDir, '../locales/languages')
const locales = ['zh', 'en', 'ja'].map(locale =>
  JSON.parse(readFileSync(resolve(localeDir, `${locale}.json`), 'utf8'))
)

const commentKeys = [
  'disabled',
  'replying-to',
  'cancel',
  'content-placeholder',
  'nickname',
  'email',
  'website',
  'submitting',
  'submit',
  'error',
  'empty',
  'reply-to',
  'reply',
  'previous',
  'next',
  'published',
  'pending'
]

describe('Comment.vue', () => {
  it('uses the V2 comment store', () => {
    expect(source).toContain('useCommentStore')
    expect(source).toContain('articleId')
    expect(source).toContain('commentStore.notice')
  })

  it('uses complete reactive translations for every public locale', () => {
    expect(source).toContain('const { t } = useI18n()')
    expect(source).toContain('appStore.locale')
    expect(source).not.toMatch(/[\u4e00-\u9fff]/)

    for (const messages of locales) {
      expect(Object.keys(messages.comments)).toEqual(commentKeys)
    }
  })
})
