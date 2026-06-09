import { defineStore } from 'pinia'

export const useNavigatorStore = defineStore({
  id: 'navigatorStore',
  state: () => ({
    openMenu: false,
    isDone: false,
    progress: 0
  }),
  getters: {},
  actions: {
    toggleMobileMenu() {
      this.isDone = false
      this.openMenu = !this.openMenu
      setTimeout(() => {
        this.isDone = this.openMenu
      }, 300)
    },
    updateProgress(progress: number) {
      this.progress = progress
    }
  }
})
