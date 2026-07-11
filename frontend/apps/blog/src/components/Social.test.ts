import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Social.vue'), 'utf8')
const customSocialsStart = source.indexOf('<template v-if="customSocials')
const emailLinkStart = source.indexOf(':href="`mailto:${socials.email}`"')

describe('Social.vue', () => {
  it('exposes the public email with the shared email icon', () => {
    expect(existsSync(resolve(currentDir, '../icons/email.svg'))).toBe(true)
    expect(source).toContain('icon-class="email"')
    expect(emailLinkStart).toBeGreaterThan(-1)
    expect(source.slice(customSocialsStart, emailLinkStart)).toContain(
      '</template>'
    )
  })
})
