import { defineStore } from 'pinia'
import {
  Archives,
  FeaturePosts,
  Post,
  PostList,
  SpecificPostsList
} from '@/models/Post.class'
import {
  fetchFeature,
  fetchPostsList,
  fetchPostBySlug,
  fetchPostsListByCategory,
  fetchPostsListByTag,
  fetchArchivesList
} from '@/api'
import { ref } from 'vue'

export const usePostStore = defineStore('postStore', () => {
  const featurePosts = ref(new FeaturePosts())
  const posts = ref(new PostList())
  const postTotal = ref(0)
  const cachePost = ref({
    title: '',
    body: '',
    uid: ''
  })

  const fetchFeaturePostsData = async () => {
    const { data } = await fetchFeature()
    return new Promise(resolve =>
      setTimeout(() => {
        featurePosts.value = new FeaturePosts(data)
        resolve(featurePosts.value)
      }, 200)
    )
  }

  const fetchPostsListData = async (page?: number): Promise<PostList> => {
    if (!page) page = 1
    const { data } = await fetchPostsList(page)
    return new Promise(resolve =>
      setTimeout(() => {
        posts.value = new PostList(data)
        postTotal.value = posts.value.total
        resolve(posts.value)
      }, 200)
    )
  }

  const fetchArchives = async (page?: number): Promise<Archives> => {
    if (!page) page = 1
    const { data } = await fetchArchivesList(page)
    return new Promise(resolve =>
      setTimeout(() => {
        resolve(new Archives(data))
      }, 200)
    )
  }

  const fetchPost = async (slug: string): Promise<Post> => {
    const { data } = await fetchPostBySlug(slug)
    return new Promise(resolve =>
      setTimeout(() => {
        resolve(new Post(data))
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
    featurePosts,
    posts,
    postTotal,
    cachePost,
    fetchFeaturePosts: fetchFeaturePostsData,
    fetchPostsList: fetchPostsListData,
    fetchArchives,
    fetchPost,
    fetchPostsByCategory,
    fetchPostsByTag,
    setCache
  }
})
