import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loadPublicCategories, loadPublicTags } from './api'
import { useTaxonomyStore } from './store'

vi.mock('./api', () => ({
  loadPublicCategories: vi.fn(),
  loadPublicTags: vi.fn()
}))

const mockedCategories = vi.mocked(loadPublicCategories)
const mockedTags = vi.mocked(loadPublicTags)

describe('taxonomy store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedCategories.mockReset()
    mockedTags.mockReset()
  })

  it('loads public categories and tags', async () => {
    mockedCategories.mockResolvedValue([
      { id: '1', name: 'Backend', slug: 'backend', articleCount: 2 }
    ])
    mockedTags.mockResolvedValue([
      { id: '2', name: 'Java', slug: 'java', articleCount: 3 }
    ])
    const store = useTaxonomyStore()

    await store.loadCategories('en')
    await store.loadTags('en')

    expect(store.categoryStatus).toBe('ready')
    expect(store.categories[0].count).toBe(2)
    expect(store.tagStatus).toBe('ready')
    expect(store.tags[0].count).toBe(3)
  })

  it('marks empty and error states', async () => {
    mockedCategories.mockResolvedValue([])
    mockedTags.mockRejectedValue(new Error('offline'))
    const store = useTaxonomyStore()

    await store.loadCategories('zh')
    await store.loadTags('zh')

    expect(store.categoryStatus).toBe('empty')
    expect(store.tagStatus).toBe('error')
  })
})
