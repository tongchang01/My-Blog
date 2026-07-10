import type { PageResponse } from '@/shared/http/contract'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { formatJst } from '@/shared/time/jst'
import type { PublicCommentDto } from './contract'
import type { CommentPageViewModel, CommentViewModel } from './model'

const mapComment = (
  dto: PublicCommentDto,
  locale: SupportedLocale
): CommentViewModel => ({
  id: dto.id,
  parentId: dto.parentId,
  replyToCommentId: dto.replyToCommentId,
  replyToNickname: dto.replyToNickname,
  authorNickname: dto.authorNickname,
  authorSite: dto.authorSite,
  contentHtml: dto.contentHtml,
  createdAt: formatJst(dto.createdAt, locale),
  replies: dto.replies.map(reply => mapComment(reply, locale))
})

export const mapCommentPage = (
  dto: PageResponse<PublicCommentDto>,
  locale: SupportedLocale
): CommentPageViewModel => ({
  records: dto.records.map(comment => mapComment(comment, locale)),
  total: dto.total,
  page: dto.page,
  size: dto.size,
  pages: dto.size > 0 ? Math.ceil(dto.total / dto.size) : 0
})
