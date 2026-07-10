import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'

export interface PublicAuthorProfileDto {
  nickname: string
  avatarUrl: string | null
  bioZh: string | null
  bioJa: string | null
  bioEn: string | null
  location: string | null
  website: string | null
  emailPublic: string | null
  githubUrl: string | null
  twitterUrl: string | null
  linkedinUrl: string | null
  zhihuUrl: string | null
  qiitaUrl: string | null
  juejinUrl: string | null
}

export const loadPublicAuthorProfile =
  async (): Promise<PublicAuthorProfileDto> => {
    const data = await requestApi<PublicAuthorProfileDto>({
      method: 'GET',
      url: '/public/author-profile'
    })
    if (data === null) throw new ApiError('Author profile response is empty')
    return data
  }
