import type { PageResponse } from '@/shared/http/contract'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { formatJst } from '@/shared/time/jst'
import type { PublicArticleListItemDto } from './contract'
import type { ArticleCardViewModel, ArticlePageViewModel } from './model'

const mapArticle = (
  dto: PublicArticleListItemDto,
  locale: SupportedLocale
): ArticleCardViewModel => ({
  id: dto.id,
  slug: dto.slug,
  title: dto.title,
  summary: dto.summary ?? '',
  coverUrl: dto.coverUrl,
  category:
    dto.categoryId !== null && dto.categoryName !== null
      ? { id: dto.categoryId, name: dto.categoryName }
      : null,
  tags: dto.tags.map(tag => ({ ...tag })),
  publishedAt: formatJst(dto.publishAt, locale),
  locked: dto.locked,
  commentCount: dto.commentCount
})

export const mapArticlePage = (
  dto: PageResponse<PublicArticleListItemDto>,
  locale: SupportedLocale
): ArticlePageViewModel => ({
  records: dto.records.map(article => mapArticle(article, locale)),
  total: dto.total,
  page: dto.page,
  size: dto.size,
  pages: dto.size > 0 ? Math.ceil(dto.total / dto.size) : 0
})
