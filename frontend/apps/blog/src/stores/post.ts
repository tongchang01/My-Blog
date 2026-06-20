import { defineStore } from 'pinia'
import { Archives, SpecificPostsList } from '@/models/Post.class'
import {
  fetchPostsListByCategory,
  fetchPostsListByTag,
  fetchArchivesList
} from '@/api'
import { ref } from 'vue'

export const usePostStore = defineStore('postStore', () => {
  const cachePost = ref({
    title: '',
    body: '',
    uid: ''
  })

  const fetchArchives = async (page?: number): Promise<Archives> => {
    if (!page) page = 1
    const { data } = await fetchArchivesList(page)
    return new Promise(resolve =>
      setTimeout(() => {
        resolve(new Archives(data))
      }, 200)
    )
  }

  const fetchPostsByCategory = async (
    category: string,
    page: number = 1,
    pageSize: number = 12
  ): Promise<SpecificPostsList> => {
    const { data } = await fetchPostsListByCategory(category, page, pageSize)
    return new Promise(resolve =>
      setTimeout(() => {
        resolve(new SpecificPostsList(data))
      }, 200)
    )
  }

  const fetchPostsByTag = async (
    slug: string,
    page: number = 1,
    pageSize: number = 12
  ): Promise<SpecificPostsList> => {
    const { data } = await fetchPostsListByTag(slug, page, pageSize)
    return new Promise(resolve => {
      setTimeout(() => {
        resolve(new SpecificPostsList(data))
      }, 200)
    })
  }

  const setCache = (data: { title: string; body: string; uid: string }) => {
    cachePost.value = data
  }

  return {
    cachePost,
    fetchArchives,
    fetchPostsByCategory,
    fetchPostsByTag,
    setCache
  }
})
