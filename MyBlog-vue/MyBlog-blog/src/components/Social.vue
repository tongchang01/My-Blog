<template>
  <ul class="flex flex-row justify-evenly flex-wrap w-full py-4 px-2 text-center items-center">
    <li v-if="websiteConfig.github" class="diamond-clip-path diamond-icon">
      <a :href="websiteConfig.github" target="_blank" ref="github" class="social-link">
        <svg-icon icon-class="github" class="fill-current" />
      </a>
    </li>
    <li v-if="websiteConfig.twitter" class="diamond-clip-path diamond-icon">
      <a :href="websiteConfig.twitter" target="_blank" ref="twitter" class="social-link">
        <svg-icon icon-class="twitter" class="fill-current" />
      </a>
    </li>
    <li v-if="websiteConfig.weChat" class="diamond-clip-path diamond-icon">
      <a :href="websiteConfig.weChat" target="_blank" ref="wechat" class="social-link">
        <svg-icon icon-class="wechat" class="fill-current" />
      </a>
    </li>
    <li v-if="websiteConfig.qq" class="diamond-clip-path diamond-icon">
      <a :href="websiteConfig.qq" target="_blank" ref="qq" class="social-link">
        <svg-icon icon-class="qq" class="fill-current" />
      </a>
    </li>
    <li v-if="websiteConfig.email" class="social-item">
      <span class="diamond-clip-path diamond-icon social-icon-shell">
        <button type="button" class="social-link social-button" @click="copyEmail">
          <svg-icon icon-class="email" class="fill-current" />
        </button>
      </span>

      <span class="social-tooltip">{{ websiteConfig.email }}</span>
    </li>
  </ul>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance } from 'vue'
import { useAppStore } from '@/stores/app'
import { useI18n } from 'vue-i18n'

export default defineComponent({
  name: 'Social',
  setup() {
    const appStore = useAppStore()
    const proxy: any = getCurrentInstance()?.appContext.config.globalProperties
    const { t } = useI18n()

    const copyByExecCommand = (text: string) => {
      const input = document.createElement('input')
      input.value = text
      document.body.appendChild(input)
      input.select()
      const copied = document.execCommand('copy')
      document.body.removeChild(input)
      return copied
    }

    const canUseClipboardApi = async () => {
      if (!navigator?.clipboard?.writeText) return false
      if (!window.isSecureContext) return false
      if (!navigator.permissions?.query) return true
      try {
        const result = await navigator.permissions.query({
          name: 'clipboard-write' as PermissionName
        })
        return result.state === 'granted' || result.state === 'prompt'
      } catch (error) {
        return true
      }
    }

    const copyEmail = async () => {
      const email = appStore.websiteConfig?.email
      if (!email) return
      let copied = false
      try {
        if (await canUseClipboardApi()) {
          await navigator.clipboard.writeText(email)
          copied = true
        }
      } catch (error) {
        copied = false
      }
      if (!copied) {
        copied = copyByExecCommand(email)
      }
      if (copied) {
        proxy?.$notify?.({
          title: t('notify.success'),
          message: t('notify.copy_email_success'),
          type: 'success'
        })
        return
      }
      proxy?.$notify?.({
        title: t('notify.error'),
        message: `${t('notify.copy_email_failed')}: ${email}`,
        type: 'error'
      })
    }

    return {
      websiteConfig: computed(() => appStore.websiteConfig),
      copyEmail
    }
  }
})
</script>

<style scoped>
.social-item {
  position: relative;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
}

.social-link {
  width: 100%;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.social-button {
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.social-button:hover,
.social-button:focus-visible {
  opacity: 0.5;
}

.social-tooltip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 10px);
  transform: translateX(-50%) translateY(4px);
  opacity: 0;
  pointer-events: none;
  white-space: nowrap;
  padding: 0.35rem 0.6rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.92);
  color: #fff;
  font-size: 0.75rem;
  line-height: 1;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.22);
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.social-tooltip::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 100%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: rgba(15, 23, 42, 0.92);
}

.social-item:hover .social-tooltip,
.social-item:focus-within .social-tooltip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.custom-social-svg-icon {
  width: 1em;
  height: 1em;
  font-size: 1em;
  vertical-align: -0.15em;
  fill: var(--text-bright);
  stroke: var(--background-primary);
  overflow: hidden;
}
</style>
