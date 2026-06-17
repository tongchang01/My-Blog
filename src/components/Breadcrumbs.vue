<template>
  <ul class="breadcrumbs flex flex-row gap-6 text-white z-50 px-4">
    <li>
      {{ t('menu.home') }}
    </li>
    <li>{{ current }}</li>
  </ul>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps({
  current: String
})

const { t } = useI18n()
</script>

<style lang="scss" scoped>
.breadcrumbs {
  position: relative;
  z-index: 20;
  li {
    position: relative;
    z-index: 20;
    &:after {
      content: '>';
      position: absolute;
      top: 0.05rem;
      right: -0.95rem;
      opacity: 0.65;
    }
    &:last-of-type {
      &:after {
        content: '';
      }
    }
  }
}
</style>
