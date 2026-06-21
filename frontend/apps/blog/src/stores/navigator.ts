import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNavigatorStore = defineStore('navigatorStore', () => {
  const openMenu = ref(false)
  const isDone = ref(false)
  const progress = ref(0)

  const toggleMobileMenu = () => {
    isDone.value = false
    openMenu.value = !openMenu.value
    setTimeout(() => {
      isDone.value = openMenu.value
    }, 300)
  }

  const updateProgress = (progressValue: number) => {
    progress.value = progressValue
  }

  return {
    openMenu,
    isDone,
    progress,
    toggleMobileMenu,
    updateProgress
  }
})
