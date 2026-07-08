import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import type { PublicFriendLinkDto } from './model'

export const loadPublicFriendLinks = async (
  signal?: AbortSignal
): Promise<PublicFriendLinkDto[]> => {
  const data = await requestApi<PublicFriendLinkDto[]>({
    method: 'GET',
    url: '/public/friend-links',
    signal
  })
  if (data === null) throw new ApiError('Friend link response is empty')
  return data
}
