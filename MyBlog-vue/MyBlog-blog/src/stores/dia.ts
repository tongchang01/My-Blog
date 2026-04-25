import { defineStore } from 'pinia'
import { AuroraDia, DiaConfig } from '@/utils/aurora-dia'
import cookies from 'js-cookie'

export const useDiaStore = defineStore('diaStore', {
  state: () => {
    return {
      dia: new AuroraDia(),
      aurora_bot: {
        enable: true,
        locale: cookies.get('locale') || 'cn',
        bot_type: 'dia'
      }
    }
  },
  actions: {
    initializeBot(configs: DiaConfig): void {
      this.dia.installSoftware(configs)
      this.dia.on()
    },
    updateBotLocale(locale: string): void {
      this.aurora_bot.locale = locale
      this.dia.configs.locale = locale

      const software = this.dia.software

      software.config.locale = locale
      software.loadLocaleMessages()
      software.injectBotScripts()
      software.messages = software.botTips.messages || []

      sessionStorage.removeItem(software.messageCacheKey)
      sessionStorage.removeItem(software.mouseoverEventCacheKey)

      software.showWelcomeMessage()
    }
  }
})
