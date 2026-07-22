<template>
  <RouterLink v-if="to" :to="to" :class="itemClasses" @click.stop="select">
    <slot />
  </RouterLink>
  <a v-else-if="href" :href="href" :class="itemClasses" @click.stop="select">
    <slot />
  </a>
  <button v-else type="button" :class="itemClasses" @click.stop="handleClick">
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useDropdownStore } from '@/stores/dropdown'

const props = defineProps<{
  name?: string
  active?: boolean
  to?: RouteLocationRaw
  href?: string
}>()

const emit = defineEmits<{
  (e: 'select'): void
}>()

const dropdownStore = useDropdownStore()

const handleClick = () => {
  dropdownStore.setCommand(String(props.name))
}

const select = () => emit('select')

const itemClasses = computed(() => {
  return {
    'text-ob-bright block w-full border-0 bg-transparent cursor-pointer hover:bg-ob-trans hover:opacity-100 my-1 px-4 py-1 font-medium text-left text-invert hover:text-ob-bright': true,
    active: !!props.active
  }
})
</script>

<style lang="scss" scoped>
.active {
  @apply bg-ob-trans;
}
</style>
