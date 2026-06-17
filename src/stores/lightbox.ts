import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLightBoxStore = defineStore('lightBoxStore', () => {
  const images = ref<string[]>([])
  const index = ref(0)
  const visible = ref(false)

  const addImage = (image: string) => {
    images.value.push(image)
  }

  const setImages = (newImages: string[]): void => {
    images.value = newImages
  }

  const openImage = (imageEle: HTMLImageElement) => {
    index.value = images.value.indexOf(imageEle.src)
    visible.value = true
  }

  const hideLightBox = () => {
    visible.value = false
  }

  return {
    images,
    index,
    visible,
    addImage,
    setImages,
    openImage,
    hideLightBox
  }
})
