<template>
  <ul class="flex flex-row justify-evenly flex-wrap w-full py-4 px-2 text-center items-center">
    <a :href="websiteConfig.github" target="_blank" ref="github" v-if="websiteConfig.github">
      <li class="diamond-clip-path diamond-icon">
        <svg-icon icon-class="github" class="fill-current" />
      </li>
    </a>
    <a :href="websiteConfig.twitter" target="_blank" ref="twitter" v-if="websiteConfig.twitter">
      <li class="diamond-clip-path diamond-icon">
        <svg-icon icon-class="twitter" class="fill-current" />
      </li>
    </a>
    <a :href="websiteConfig.wechat" target="_blank" ref="wechat" v-if="websiteConfig.wechat">
      <li class="diamond-clip-path diamond-icon">
        <svg-icon icon-class="wechat" class="fill-current" />
      </li>
    </a>
    <a :href="websiteConfig.qq" target="_blank" ref="qq" v-if="websiteConfig.qq">
      <li class="diamond-clip-path diamond-icon">
        <svg-icon icon-class="qq" class="fill-current" />
      </li>
    </a>
  </ul>
</template>

<script lang="ts">
import { computed, defineComponent, toRefs, toRef } from 'vue'
import { useAppStore } from '@/stores/app'

export default defineComponent({
  name: 'Social',
  setup() {
    let appStore = useAppStore()
    return {
      websiteConfig: computed(() => {
        return appStore.websiteConfig
      })
    }
  }
})
</script>

<style scoped>
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


