import type { Social } from '@/models/ThemeConfig.class'

export type AuthorProfileStatus = 'idle' | 'loading' | 'ready' | 'degraded'

export interface AuthorProfileViewModel {
  name: string
  avatar: string
  description: string
  socials: Social
  articleCount: number
  categoryCount: number
  tagCount: number
}
