import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Profile.vue'), 'utf8')
const locationBlock =
  source.match(/<p\s+v-if="authorData\.location"[\s\S]*?<\/p>/)?.[0] ?? ''

describe('Profile.vue', () => {
  it('does not render the removed author word-count statistic', () => {
    expect(source).not.toContain('word_count')
    expect(source).not.toContain("t('settings.words')")
    expect(source).not.toContain('profile.wordCount')
  })

  it('renders the public author location when available', () => {
    expect(locationBlock).toContain('authorData.location')
    expect(locationBlock).toContain('icon-class="location"')
    expect(locationBlock).toContain('w-full')
    expect(locationBlock).toContain('flex-1')
    expect(locationBlock).toContain('gap-1')
    expect(locationBlock).toContain('text-base')
    expect(locationBlock).not.toMatch(/text-(?:black|ob-bright)/)
  })
})
