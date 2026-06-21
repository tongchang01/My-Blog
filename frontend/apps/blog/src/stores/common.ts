import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCommonStore = defineStore('commonStore', () => {
  const isMobile = ref(false)
  const headerImage = ref('')
  const notificationState = ref(false)
  const notificationMessage = ref('')

  const setHeaderImage = (imageUrl: string) => {
    headerImage.value = imageUrl
  }

  const resetHeaderImage = () => {
    headerImage.value = ''
  }

  const changeMobileState = (mobile: boolean) => {
    isMobile.value = mobile
  }

  const sendNotification = (message: string) => {
    notificationState.value = true
    notificationMessage.value = message
  }

  const closeNotification = () => {
    notificationState.value = false
  }

  return {
    isMobile,
    headerImage,
    notificationState,
    notificationMessage,
    setHeaderImage,
    resetHeaderImage,
    changeMobileState,
    sendNotification,
    closeNotification
  }
})
