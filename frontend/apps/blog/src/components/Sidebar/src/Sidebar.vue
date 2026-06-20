<template>
  <div v-if="!isMobile">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { useCommonStore } from '@/stores/common'
import { computed } from 'vue'

const commonStore = useCommonStore()
const isMobile = computed(() => commonStore.isMobile)
</script>

<style lang="scss" scoped></style>
