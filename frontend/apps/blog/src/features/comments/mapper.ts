import type { PageResponse } from '@/shared/http/contract'
import type { PublicCommentDto } from './contract'
import type { CommentPageViewModel, CommentViewModel } from './model'

const mapComment = (dto: PublicCommentDto): CommentViewModel => ({
  id: dto.id,
  parentId: dto.parentId,
  replyToCommentId: dto.replyToCommentId,
  replyToNickname: dto.replyToNickname,
  authorNickname: dto.authorNickname,
  authorSite: dto.authorSite,
  contentHtml: dto.contentHtml,
  createdAt: dto.createdAt,
  replies: dto.replies.map(mapComment)
})

export const mapCommentPage = (
  dto: PageResponse<PublicCommentDto>
): CommentPageViewModel => ({
  records: dto.records.map(mapComment),
  total: dto.total,
  page: dto.page,
  size: dto.size,
  pages: dto.size > 0 ? Math.ceil(dto.total / dto.size) : 0
})
