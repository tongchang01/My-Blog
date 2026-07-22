<template>
  <button
    type="button"
    class="toggler"
    :aria-label="label"
    :aria-pressed="toggleStatus"
    @click="changeStatus"
  >
    <div class="toggle-track"></div>
    <div
      class="slider"
      :style="{
        transform: toggleStyle.transform,
        backgroundColor: toggleStyle.background
      }"
    >
      <slot />
    </div>
  </button>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, toRefs } from 'vue'

const props = defineProps<{
  status?: boolean
  label: string
}>()

const emit = defineEmits<{
  (e: 'changeStatus', value: boolean): void
}>()

const { status } = toRefs(props)

onMounted(() => {
  changeTransform()
})

const toggleStyle = reactive({
  transform: '',
  background: '#6e40c9'
})
const toggleStatus = ref(Boolean(status.value))

const changeStatus = () => {
  toggleStatus.value = !toggleStatus.value
  changeTransform()
  emit('changeStatus', toggleStatus.value)
}

const changeTransform = () => {
  const transform = toggleStatus.value ? '18px' : '0'
  toggleStyle.transform = `translateX(${transform})`
  const backgroundColor = toggleStatus.value ? '#6e40c9' : '#100E16'
  toggleStyle.background = backgroundColor
}
</script>

<style lang="scss" scoped>
.toggler {
  @apply appearance-none relative p-0 cursor-pointer;
  width: 40px;
  height: 22px;
  background-color: var(--background-primary);
  border-radius: 24px;
  border: 3px solid rgba(110, 64, 201, 0.35);
  box-sizing: border-box;
  transition: background-color 250ms ease;
}
.slider {
  top: -6px;
  left: -6px;
  width: 28px;
  height: 28px;
  background-color: #6e40c9;
  border-radius: 50%;
  transition: all 250ms cubic-bezier(0.4, 0.03, 0, 1) 0s;
  @apply absolute shadow-lg;
}
</style>
