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
