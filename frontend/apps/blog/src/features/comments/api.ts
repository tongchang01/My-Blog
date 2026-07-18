import { requestApi } from '@/shared/http/client'
import type { PageResponse } from '@/shared/http/contract'
import { ApiError } from '@/shared/http/error'
import type {
  CreateCommentPayload,
  CreateCommentResultDto,
  PublicCommentDto
} from './contract'

export interface LoadArticleCommentsParams {
  articleId: string
  page: number
  size: number
  signal?: AbortSignal
  articleAccessToken?: string | null
}

export const loadArticleComments = async ({
  articleId,
  page,
  size,
  signal,
  articleAccessToken
}: LoadArticleCommentsParams): Promise<PageResponse<PublicCommentDto>> => {
  const data = await requestApi<PageResponse<PublicCommentDto>>({
    method: 'GET',
    url: `/public/articles/${encodeURIComponent(articleId)}/comments`,
    params: { page, size },
    signal,
    headers: articleAccessToken
      ? { 'X-Article-Access-Token': articleAccessToken }
      : undefined
  })
  if (data === null) throw new ApiError('Comment page response is empty')
  return data
}

export const createArticleComment = async (
  articleId: string,
  payload: CreateCommentPayload,
  articleAccessToken?: string | null
): Promise<CreateCommentResultDto> => {
  const data = await requestApi<CreateCommentResultDto>({
    method: 'POST',
    url: `/public/articles/${encodeURIComponent(articleId)}/comments`,
    data: payload,
    headers: articleAccessToken
      ? { 'X-Article-Access-Token': articleAccessToken }
      : undefined
  })
  if (data === null) throw new ApiError('Comment create response is empty')
  return data
}

export const loadGuestbookComments = async (
  page: number,
  size: number
): Promise<PageResponse<PublicCommentDto>> => {
  const data = await requestApi<PageResponse<PublicCommentDto>>({
    method: 'GET',
    url: '/public/guestbook/comments',
    params: { page, size }
  })
  if (data === null) throw new ApiError('Guestbook response is empty')
  return data
}

export const createGuestbookComment = async (
  payload: CreateCommentPayload
): Promise<CreateCommentResultDto> => {
  const data = await requestApi<CreateCommentResultDto>({
    method: 'POST',
    url: '/public/guestbook/comments',
    data: payload
  })
  if (data === null) throw new ApiError('Guestbook create response is empty')
  return data
}
