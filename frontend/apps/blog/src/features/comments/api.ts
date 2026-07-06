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
}

export const loadArticleComments = async ({
  articleId,
  page,
  size,
  signal
}: LoadArticleCommentsParams): Promise<PageResponse<PublicCommentDto>> => {
  const data = await requestApi<PageResponse<PublicCommentDto>>({
    method: 'GET',
    url: `/public/articles/${encodeURIComponent(articleId)}/comments`,
    params: { page, size },
    signal
  })
  if (data === null) throw new ApiError('Comment page response is empty')
  return data
}

export const createArticleComment = async (
  articleId: string,
  payload: CreateCommentPayload
): Promise<CreateCommentResultDto> => {
  const data = await requestApi<CreateCommentResultDto>({
    method: 'POST',
    url: `/public/articles/${encodeURIComponent(articleId)}/comments`,
    data: payload
  })
  if (data === null) throw new ApiError('Comment create response is empty')
  return data
}
