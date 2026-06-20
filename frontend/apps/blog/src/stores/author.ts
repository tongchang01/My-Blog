import { fetchAuthorPost } from '@/api'
import { AuthorPosts } from '@/models/Post.class'
import { defineStore } from 'pinia'

export const useAuthorStore = defineStore('authorStore', () => {
  const fetchAuthorData = async (slug: string): Promise<AuthorPosts> => {
    const { data } = await fetchAuthorPost(slug)
    return new Promise(resolve => {
      resolve(new AuthorPosts(data))
    })
  }

  return {
    fetchAuthorData
  }
})
