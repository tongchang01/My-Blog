export type CommentAuditStatus = 'PASS' | 'PENDING' | 'HIDDEN'

export interface PublicCommentDto {
  id: string
  parentId: string | null
  replyToCommentId: string | null
  replyToNickname: string | null
  authorNickname: string
  authorSite: string | null
  contentHtml: string
  createdAt: string
  replies: PublicCommentDto[]
}

export interface CreateCommentPayload {
  nickname: string
  email: string
  site: string | null
  contentMd: string
  replyToCommentId: string | null
}

export interface CreateCommentResultDto {
  id: string
  auditStatus: CommentAuditStatus
}
