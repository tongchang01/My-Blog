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

export interface ArticleHomeViewModel {
  pinnedArticle: ArticleCardViewModel | null
  featuredArticles: ArticleCardViewModel[]
  articles: ArticleCardViewModel[]
}

export interface ArticleDetailViewModel extends ArticleCardViewModel {
  bodyHtml: string
  updatedAt: string
}

export type ArticleListStatus = 'idle' | 'loading' | 'ready' | 'empty' | 'error'

export type ArticleDetailStatus =
  | 'idle'
  | 'loading'
  | 'ready'
  | 'locked'
  | 'notFound'
  | 'error'
