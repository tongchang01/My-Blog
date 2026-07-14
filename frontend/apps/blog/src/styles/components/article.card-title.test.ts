import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const articleStyles = readFileSync(new URL('./article.scss', import.meta.url), 'utf8')

describe('feature article title layout', () => {
  it('uses a font-metric-safe title line-height before applying the two-line clamp', () => {
    expect(articleStyles).toMatch(
      /\.feature-content[\s\S]*?h1\s*\{[\s\S]*?leading-normal[\s\S]*?lg:leading-normal[\s\S]*?line-clamp-2/
    )
  })
})
