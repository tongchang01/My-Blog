import { describe, expect, it } from 'vitest'
import { mapCommentPage } from './mapper'

describe('comments mapper', () => {
  it('preserves string IDs and maps nested replies', () => {
    const mapped = mapCommentPage({
      records: [
        {
          id: '9007199254740993',
          parentId: null,
          replyToCommentId: null,
          replyToNickname: null,
          authorNickname: 'TYB',
          authorSite: null,
          contentHtml: '<p>Hello</p>',
          createdAt: '2026-07-06T10:00:00',
          replies: [
            {
              id: '9007199254740994',
              parentId: '9007199254740993',
              replyToCommentId: '9007199254740993',
              replyToNickname: 'TYB',
              authorNickname: 'Reader',
              authorSite: 'https://example.com',
              contentHtml: '<p>Reply</p>',
              createdAt: '2026-07-06T10:05:00',
              replies: []
            }
          ]
        }
      ],
      total: 1,
      page: 1,
      size: 20
    })

    expect(mapped.records[0].id).toBe('9007199254740993')
    expect(mapped.records[0].authorSite).toBeNull()
    expect(mapped.records[0].contentHtml).toContain('Hello')
    expect(mapped.records[0].replies[0].id).toBe('9007199254740994')
    expect(mapped.records[0].replies[0].replyToNickname).toBe('TYB')
    expect(mapped.pages).toBe(1)
  })
})
