import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useMetaStore } from './meta'

vi.mock('./app', () => ({
  useAppStore: () => ({
    themeConfig: { site: { subtitle: 'Blog', slogan: '' } }
  })
}))

describe('meta store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('computes a title without component injection context', () => {
    const store = useMetaStore()
    expect(store.getTitle).toBe('Blog')
  })
})
