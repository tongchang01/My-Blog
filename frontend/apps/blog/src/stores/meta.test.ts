import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useMetaStore } from './meta'

vi.mock('./app', () => ({
  useAppStore: () => ({
    siteTitle: 'TYB',
    siteSubtitle: 'Code and life'
  })
}))

describe('meta store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('uses the site title and subtitle for the home page', () => {
    const store = useMetaStore()
    expect(store.getTitle).toBe('TYB · Code and life')
  })

  it('puts the current page before the site title', () => {
    const store = useMetaStore()
    store.setTitle('A long article title')
    expect(store.getTitle).toBe('A long article title · TYB')
  })
})
