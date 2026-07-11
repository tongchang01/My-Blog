import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('../../styles/theme-variables.scss', import.meta.url)),
  'utf8'
)

describe('footer avatar', () => {
  it('renders without a translucent overlay', () => {
    const footerAvatarStyle = source.match(/\.footer-avatar\s*\{[^}]*\}/s)?.[0]
    expect(footerAvatarStyle).toBeDefined()
    expect(footerAvatarStyle).not.toContain('opacity-40')
  })
})
