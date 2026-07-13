import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const currentDir = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(currentDir, 'Social.vue'), 'utf8')
const emailIcon = readFileSync(
  resolve(currentDir, '../icons/email.svg'),
  'utf8'
)
const twitterIcon = readFileSync(
  resolve(currentDir, '../icons/twitter.svg'),
  'utf8'
)
const csdnIcon = readFileSync(resolve(currentDir, '../icons/csdn.svg'), 'utf8')
const customSocialsStart = source.indexOf('<template v-if="customSocials')
const emailLinkStart = source.indexOf(':href="`mailto:${socials.email}`"')
const emailBlock = source.slice(emailLinkStart)

describe('Social.vue', () => {
  it('exposes the public email with the supplied solid envelope icon', () => {
    expect(existsSync(resolve(currentDir, '../icons/email.svg'))).toBe(true)
    expect(emailIcon).toContain('viewBox="0 0 512 512"')
    expect(emailIcon).not.toContain('#000000')
    expect(emailBlock).toContain('icon-class="email"')
    expect(emailLinkStart).toBeGreaterThan(-1)
    expect(source.slice(customSocialsStart, emailLinkStart)).toContain(
      '</template>'
    )
    expect(emailBlock).toContain('fill="currentColor"')
    expect(emailBlock).toContain('stroke="none"')
  })

  it('keeps social SVGs free of invalid empty paint attributes', () => {
    expect(twitterIcon).not.toContain('undefined=')
    expect(twitterIcon).not.toContain('fill=""')
    expect(csdnIcon).not.toContain('fill=""')
  })
})
