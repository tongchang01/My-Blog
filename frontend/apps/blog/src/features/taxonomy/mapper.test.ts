import { describe, expect, it } from 'vitest'
import { mapCategories, mapTags } from './mapper'

describe('taxonomy mapper', () => {
  it('maps backend articleCount to category count', () => {
    const mapped = mapCategories([
      {
        id: '9007199254740993',
        name: 'Backend',
        slug: 'backend',
        articleCount: 8
      }
    ])

    expect(mapped[0]).toEqual({
      id: '9007199254740993',
      name: 'Backend',
      slug: 'backend',
      path: 'categories/backend/',
      count: 8
    })
  })

  it('maps backend articleCount to tag count', () => {
    const mapped = mapTags([
      {
        id: '9007199254740994',
        name: 'Java',
        slug: 'java',
        articleCount: 5
      }
    ])

    expect(mapped[0]).toEqual({
      id: '9007199254740994',
      name: 'Java',
      slug: 'java',
      path: 'tags/java/',
      count: 5
    })
  })
})
