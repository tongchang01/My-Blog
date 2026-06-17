import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDropdownStore = defineStore('dropdown', () => {
  const commandName = ref('')
  const uid = ref(0)

  const setCommand = (name: string): void => {
    commandName.value = name
  }

  const setUid = (): number => {
    uid.value = Date.now()
    return uid.value
  }

  return {
    commandName,
    uid,
    setCommand,
    setUid
  }
})
