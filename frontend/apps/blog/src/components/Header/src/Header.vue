<template>
  <Sticky
    :stickyTop="0"
    :z-index="999"
    @activeChange="handleActiveState"
    :delay="650"
  >
    <div :class="containerClasses">
      <header class="site-header lg:max-w-screen-2xl px-3 lg:px-8">
        <Logo />
        <Navigation />
        <Controls :scroll-progress="progress" />
        <Notification />
      </header>
    </div>
  </Sticky>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Logo, Navigation, Controls, Notification } from '../index'
import Sticky from '@/components/Sticky.vue'
import { useNavigatorStore } from '@/stores/navigator'

defineProps({
  msg: String
})

const navigatorStore = useNavigatorStore()
const active = ref<boolean>(false)

const handleActiveState = (value: boolean) => {
  active.value = value
}

const containerClasses = computed(() => ({
  'header-container': true,
  'header-active': active.value
}))
const progress = computed(() => navigatorStore.progress)
</script>

<style lang="scss">
.header-container {
  position: relative;

  &.header-active {
    @apply bg-ob-backdrop shadow-xl text-ob-bright;
    // 模糊只作用于背景层，避免为内部 fixed 播放器建立定位包含块。
    &::before {
      content: '';
      position: absolute;
      inset: 0;
      pointer-events: none;
      @apply backdrop-blur;
    }
    .site-header {
      @apply py-2;
    }
  }
  transition: 0.3s background ease;
  .site-header {
    @apply relative flex z-50 py-4;
    transition: 0.3s padding ease-in-out;
    margin: 0 auto;
  }
}

.header-active {
  .text-invert {
    @apply text-ob-bright hover:opacity-60;
  }
}
</style>
