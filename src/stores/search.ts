import { fetchSearchIndexes } from '@/api'
import { RecentSearchResults, SearchIndexes } from '@/models/Search.class'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useSearchStore = defineStore('searchStore', () => {
  const searchIndexes = ref(new SearchIndexes())
  const recentResults = ref(new RecentSearchResults())
  const openModal = ref(false)

  const results = computed(() => {
    return recentResults.value.getData()
  })

  const fetchSearchIndex = async (): Promise<SearchIndexes> => {
    const { data } = await fetchSearchIndexes()
    searchIndexes.value = new SearchIndexes(data)
    return new Promise(resolve => {
      resolve(searchIndexes.value)
    })
  }

  const setOpenModal = (status: boolean) => {
    openModal.value = status
    let searchContainer: HTMLElement | null
    if (status === true) {
      document.body.classList.add('modal--active')
    } else {
      searchContainer = document.getElementById('search-modal')
      if (searchContainer) {
        searchContainer.style.animation =
          '0.85s ease 0s 1 normal none running opacity_hide'
      }
      document.body.classList.remove('modal--active')
    }
  }

  const addRecentSearch = (result: { [key: string]: string }) => {
    recentResults.value.add(result)
  }

  return {
    searchIndexes,
    recentResults,
    openModal,
    results,
    fetchSearchIndex,
    setOpenModal,
    addRecentSearch
  }
})
