export interface PublicArticleTagDto {
  id: string
  name: string
  slug: string
}

export interface PublicArticleListItemDto {
  id: string
  title: string
  summary: string | null
  categoryId: string | null
  categoryName: string | null
  slug: string
  publishAt: string
  coverUrl: string | null
  commentCount: number
  tags: PublicArticleTagDto[]
  createdAt: string
  locked: boolean
}

export interface PublicArticleDetailDto extends PublicArticleListItemDto {
  body: string
  updatedAt: string
}

export interface PublicArticleHomeDto {
  pinnedArticle: PublicArticleListItemDto | null
  featuredArticles: PublicArticleListItemDto[]
  articles: PublicArticleListItemDto[]
}

export interface PublicArchiveArticleDto {
  id: string
  title: string
  slug: string
  publishedAt: string
  summary: string | null
}

export interface PublicArchiveGroupDto {
  yearMonth: string
  year: number
  month: number
  articles: PublicArchiveArticleDto[]
}
