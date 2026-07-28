import { describe, expect, it } from 'vitest'
import { isPublicHttpUrl } from './publicUrl'

describe('isPublicHttpUrl', () => {
  it('accepts public HTTP URLs without credentials', () => {
    expect(
      isPublicHttpUrl('https://example.com/path?next=user@example.com')
    ).toBe(true)
    expect(isPublicHttpUrl('http://localhost:8080/profile')).toBe(true)
  })

  it('rejects credentials and non-HTTP URLs', () => {
    expect(isPublicHttpUrl('https://user:password@example.com')).toBe(false)
    expect(isPublicHttpUrl('ftp://example.com/file')).toBe(false)
    expect(isPublicHttpUrl('/relative')).toBe(false)
  })
})
