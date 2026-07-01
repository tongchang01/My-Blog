import type { PageResponse } from '@/shared/http/contract'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { formatJst } from '@/shared/time/jst'
import { renderMarkdown } from '@/shared/markdown/render'
import type {
  PublicArticleDetailDto,
  PublicArticleHomeDto,
  PublicArticleListItemDto
} from './contract'
import type {
  ArticleCardViewModel,
  ArticleDetailViewModel,
  ArticleHomeViewModel,
  ArticlePageViewModel
} from './model'

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

export const mapArticleHome = (
  dto: PublicArticleHomeDto,
  locale: SupportedLocale
): ArticleHomeViewModel => ({
  pinnedArticle: dto.pinnedArticle
    ? mapArticle(dto.pinnedArticle, locale)
    : null,
  featuredArticles: dto.featuredArticles.map(article =>
    mapArticle(article, locale)
  ),
  articles: dto.articles.map(article => mapArticle(article, locale))
})

export const mapArticleDetail = (
  dto: PublicArticleDetailDto,
  locale: SupportedLocale
): ArticleDetailViewModel => ({
  ...mapArticle(dto, locale),
  bodyHtml: renderMarkdown(dto.body),
  updatedAt: formatJst(dto.updatedAt, locale)
})
