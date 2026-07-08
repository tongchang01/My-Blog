import { describe, expect, it } from 'vitest'
import { mapFriendLinks } from './model'

describe('friend link model', () => {
  it('maps public friend links to existing link cards', () => {
    expect(
      mapFriendLinks([
        {
          id: '1',
          name: 'Example',
          url: 'https://example.com',
          avatarUrl: null,
          description: null
        }
      ])
    ).toEqual([
      {
        nick: 'Example',
        avatar: '',
        link: 'https://example.com',
        description: '',
        label: 'links-badge-personal'
      }
    ])
  })
})
