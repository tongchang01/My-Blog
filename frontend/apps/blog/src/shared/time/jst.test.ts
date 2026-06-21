import { describe, expect, it } from 'vitest'
import { formatJst, parseJst } from './jst'

describe('JST date handling', () => {
  it('parses backend local date-times as Asia/Tokyo', () => {
    expect(parseJst('2026-06-15T10:00:00').toISOString()).toBe(
      '2026-06-15T01:00:00.000Z'
    )
  })

  it('formats in JST independently of the machine time zone', () => {
    const formatted = formatJst('2026-06-15T10:00:00', 'en')
    expect(formatted).toContain('2026')
    expect(formatted).toContain('10:00')
  })

  it('rejects invalid values', () => {
    expect(() => parseJst('not-a-date')).toThrowError('Invalid JST date')
  })
})
