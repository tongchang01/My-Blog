import { describe, expect, it } from 'vitest'
import { mapArticlePage } from './mapper'

describe('article mapper', () => {
  it('preserves string IDs and computes pagination', () => {
    const mapped = mapArticlePage(
      {
        records: [
          {
            id: '9007199254740993',
            title: 'Article',
            summary: null,
            categoryId: '9007199254740994',
            categoryName: 'Java',
            slug: 'article',
            publishAt: '2026-06-15T10:00:00',
            coverUrl: null,
            commentCount: 2,
            tags: [{ id: '9007199254740995', name: 'Spring', slug: 'spring' }],
            createdAt: '2026-06-15T09:00:00',
            locked: false
          }
        ],
        total: 13,
        page: 1,
        size: 12
      },
      'en'
    )

    expect(mapped.records[0].id).toBe('9007199254740993')
    expect(mapped.records[0].category?.id).toBe('9007199254740994')
    expect(mapped.records[0].tags[0].id).toBe('9007199254740995')
    expect(mapped.records[0].summary).toBe('')
    expect(mapped.records[0].coverUrl).toBeNull()
    expect(mapped.records[0].publishedAt).toContain('10:00')
    expect(mapped.pages).toBe(2)
  })
})
