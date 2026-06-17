<template>
  <transition name="dropdown-content">
    <div
      v-if="!expand && active"
      class="origin-top-right absolute right-0 mt-2 w-48 bg-ob-deep-900 rounded-lg py-2 shadow-md"
    >
      <slot />
    </div>
    <div
      v-else-if="expand && active"
      class="flex flex-col justify-center items-center mt-2 w-48 bg-ob-deep-900 rounded-lg py-2"
    >
      <slot />
    </div>
  </transition>
</template>

<script setup lang="ts">
import { useDropdownStore } from '@/stores/dropdown'
import { computed, inject, watch } from 'vue'

defineProps({
  expand: {
    type: Boolean,
    default: false
  }
})

const dropdownStore = useDropdownStore()
const sharedState = inject('sharedState', { active: false })
const active = computed(() => sharedState.active)

watch(
  () => dropdownStore.commandName,
  (newValue, oldValue) => {
    if (oldValue !== newValue) {
      sharedState.active = false
    }
  }
)
</script>

<style lang="scss" scoped>
.dropdown-content-enter-active,
.dropdown-content-leave-active {
  transition: all 0.2s;
}
.dropdown-content-enter,
.dropdown-content-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
