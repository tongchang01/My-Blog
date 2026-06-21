import { fetchAllCategories } from '@/api'
import { Categories } from '@/models/Post.class'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCategoryStore = defineStore('categoryStore', () => {
  const isLoaded = ref(false)
  const categories = ref(new Categories().data)

  const fetchCategories = async () => {
    isLoaded.value = false
    const { data } = await fetchAllCategories()
    return new Promise(resolve => {
      isLoaded.value = true
      categories.value = new Categories(data).data
      resolve(categories.value)
    })
  }

  return {
    isLoaded,
    categories,
    fetchCategories
  }
})
