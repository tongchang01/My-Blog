export interface ArticleTagViewModel {
  id: string
  name: string
  slug: string
}

export interface ArticleCardViewModel {
  id: string
  slug: string
  title: string
  summary: string
  coverUrl: string | null
  category: { id: string; name: string } | null
  tags: ArticleTagViewModel[]
  publishedAt: string
  locked: boolean
  commentCount: number
}

export interface ArticlePageViewModel {
  records: ArticleCardViewModel[]
  total: number
  page: number
  size: number
  pages: number
}

export type ArticleListStatus = 'idle' | 'loading' | 'ready' | 'empty' | 'error'
