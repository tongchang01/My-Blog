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

  it('renders the public author location when available', () => {
    expect(source).toContain('authorData.location')
    expect(source).toContain('icon-class="location"')
    expect(source).toContain('w-full')
    expect(source).toContain('flex-1')
    expect(source).toContain('text-black')
    expect(source).toContain('text-base')
    expect(source).not.toContain('text-base font-bold text-black')
  })
})
