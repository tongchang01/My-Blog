import type { Link } from '@/models/Article.class'

export interface PublicFriendLinkDto {
  id: string
  name: string
  url: string
  avatarUrl: string | null
  description: string | null
}

export const mapFriendLink = (link: PublicFriendLinkDto): Link => ({
  nick: link.name,
  avatar: link.avatarUrl ?? '',
  link: link.url,
  description: link.description ?? '',
  label: 'links-badge-personal'
})

export const mapFriendLinks = (links: PublicFriendLinkDto[]): Link[] =>
  links.map(mapFriendLink)
