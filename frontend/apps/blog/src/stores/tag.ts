import { fetchAllTags } from '@/api'
import { Tags } from '@/models/Post.class'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTagStore = defineStore('tagStore', () => {
  const isLoaded = ref(false)
  const tags = ref(new Tags().data)

  const fetchAllTagsData = async () => {
    const { data } = await fetchAllTags()
    return new Promise(resolve => {
      tags.value = new Tags(data).data
      resolve(tags.value)
    })
  }

  const fetchTagsByCount = async (count: number) => {
    isLoaded.value = false
    const { data } = await fetchAllTags()
    return new Promise(resolve => {
      isLoaded.value = true
      const maxLength = data.length > count ? count : data.length
      tags.value = new Tags(data.splice(0, maxLength)).data
      resolve(tags.value)
    })
  }

  return {
    isLoaded,
    tags,
    fetchAllTags: fetchAllTagsData,
    fetchTagsByCount
  }
})
