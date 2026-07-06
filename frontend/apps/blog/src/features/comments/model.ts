export interface CommentViewModel {
  id: string
  parentId: string | null
  replyToCommentId: string | null
  replyToNickname: string | null
  authorNickname: string
  authorSite: string | null
  contentHtml: string
  createdAt: string
  replies: CommentViewModel[]
}

export interface CommentPageViewModel {
  records: CommentViewModel[]
  total: number
  page: number
  size: number
  pages: number
}

export type CommentListStatus =
  | 'idle'
  | 'loading'
  | 'ready'
  | 'empty'
  | 'error'
  | 'submitting'

export interface CommentFormState {
  nickname: string
  email: string
  site: string
  contentMd: string
}
