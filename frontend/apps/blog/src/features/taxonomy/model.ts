export type TaxonomyStatus = 'idle' | 'loading' | 'ready' | 'empty' | 'error'

export interface TaxonomyItemViewModel {
  id: string
  name: string
  slug: string
  path: string
  count: number
}
