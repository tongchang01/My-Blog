import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Profile.vue'), 'utf8')

describe('Profile.vue', () => {
  it('does not render the removed author word-count statistic', () => {
    expect(source).not.toContain('word_count')
    expect(source).not.toContain("t('settings.words')")
    expect(source).not.toContain('profile.wordCount')
  })
})
