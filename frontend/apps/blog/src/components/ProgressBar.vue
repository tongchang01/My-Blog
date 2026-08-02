<template>
  <div
    id="progress-bar"
    class="rounded-full shadow-2xl"
    :style="progressBarStyle"
  ></div>
</template>

<script setup lang="ts">
import { throttle } from 'lodash-es'
import { computed, onMounted, onUnmounted, ref } from 'vue'

const progress = ref(0)

const scrollHandler = throttle(
  () => {
    progress.value =
      (window.scrollY /
        (document.documentElement.scrollHeight - window.innerHeight)) *
      100
  },
  100,
  { trailing: true, leading: true }
)

onMounted(() => {
  scrollHandler()
  document.addEventListener('scroll', scrollHandler)
})

onUnmounted(() => {
  document.removeEventListener('scroll', scrollHandler)
})

const progressBarStyle = computed(() => {
  return { width: `${progress.value}%` }
})
</script>

<style lang="scss" scoped>
#progress-bar {
  content: '';
  position: fixed;
  z-index: 9999;
  top: 0;
  height: 3px;
  background: var(--main-gradient);
}
</style>
