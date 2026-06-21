import { describe, expect, it } from 'vitest'
import { mapArticleDetail, mapArticlePage } from './mapper'

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

describe('article detail mapper', () => {
  it('renders Markdown while preserving the canonical slug and string ID', () => {
    const mapped = mapArticleDetail(
      {
        id: '9007199254740993',
        title: 'Article',
        summary: 'Summary',
        body: '# Body',
        categoryId: null,
        categoryName: null,
        slug: 'canonical-slug',
        publishAt: '2026-06-15T10:00:00',
        coverUrl: null,
        commentCount: 0,
        tags: [],
        createdAt: '2026-06-15T09:00:00',
        updatedAt: '2026-06-15T11:00:00',
        locked: false
      },
      'en'
    )

    expect(mapped.id).toBe('9007199254740993')
    expect(mapped.slug).toBe('canonical-slug')
    expect(mapped.bodyHtml).toContain('<h1>Body</h1>')
  })
})
