<template>
  <div ref="rootEl" id="sticky" :style="{ height: height + 'px', zIndex: zIndex }">
    <div :class="className" :style="styles">
      <slot>
        <div>sticky</div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Lodash package is imported through CDN.
 *
 * For version 4.17.21
 */
declare const _: any

import { useNavigatorStore } from '@/stores/navigator'
import {
  StyleValue,
  computed,
  onActivated,
  onMounted,
  onUnmounted,
  ref
} from 'vue'

const props = defineProps({
  stickyTop: {
    type: Number,
    default: 0
  },
  zIndex: {
    type: Number,
    default: 1
  },
  className: {
    type: String,
    default: ''
  },
  stickyBottom: {
    type: Number,
    default: 0
  },
  endingElId: {
    type: String,
    default: ''
  },
  dynamicElClass: {
    type: String,
    default: ''
  },
  delay: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits<{
  (e: 'activeChange', value: boolean): void
}>()

const rootEl = ref<HTMLElement>()
const active = ref(false)
const position = ref('')
const width = ref<any>()
const height = ref<any>()
const isSticky = ref(false)
const newTop = ref(0)
const top = ref(0)
const isBottom = ref(false)
const navigatorStore = useNavigatorStore()

const styles = computed(
  () =>
    ({
      top: isSticky.value
        ? top.value === -1
          ? 'initial'
          : top.value + 'px'
        : '',
      bottom: isBottom.value ? 0 : 'initial',
      zIndex: props.zIndex,
      position: position.value,
      width: width.value,
      height: height.value + 'px'
    }) as StyleValue
)

const sticky = (topValue: number, positionValue: any) => {
  if (active.value) {
    return
  }
  top.value = topValue
  position.value = positionValue
  active.value = true
  width.value = width.value + 'px'
  isSticky.value = true
  emit('activeChange', true)
}

const reset = () => {
  emit('activeChange', false)
  setTimeout(() => {
    position.value = ''
    width.value = 'auto'
    active.value = false
    isSticky.value = false
  }, props.delay)
}

const handleReset = () => {
  if (!active.value) {
    return
  }
  reset()
}

const updateProgress = () => {
  const progress = Number(
    (
      (window.scrollY /
        (document.documentElement.scrollHeight - window.innerHeight)) *
      100
    ).toFixed(0)
  )

  navigatorStore.updateProgress(progress)
}

const updateScroll = () => {
  updateProgress()

  const documentHeight = document.documentElement.scrollHeight
  const widthVal = rootEl.value!.getBoundingClientRect().width
  const heightVal = rootEl.value!.getBoundingClientRect().height

  // Use `Dynamic Element Class` when your content will change
  // which will affect the height of your fixed container
  // this will update the height of your fixed container
  if (props.dynamicElClass !== '') {
    const dynamicEl = rootEl.value!.querySelector(props.dynamicElClass)
    height.value = (dynamicEl as HTMLElement)?.getBoundingClientRect().height || heightVal
  }

  const scrollTop = window.scrollY
  width.value = widthVal || 'auto'
  const offsetTop = rootEl.value!.getBoundingClientRect().top

  // When the fixed container reaches the ending element container
  // Fix position property will be turned off, and the fixed container
  // will stop right before the ending element.
  const endingEl =
    props.endingElId !== '' ? document.getElementById(props.endingElId) : null

  const endingElSpacing = documentHeight - (endingEl?.offsetTop ?? 0)

  const wrapperEl = document.getElementById('App-Wrapper')

  const containerBottomSpacing = parseInt(
    window.getComputedStyle(wrapperEl || document.documentElement)
      .paddingBottom,
    10
  )

  const endingPos =
    endingEl && endingEl instanceof HTMLElement
      ? documentHeight -
        scrollTop -
        heightVal -
        props.stickyTop -
        props.stickyBottom -
        endingElSpacing -
        containerBottomSpacing
      : documentHeight

  if (offsetTop < props.stickyTop) {
    active.value = false
    if (endingPos <= 0) {
      isBottom.value = true
      sticky(-1, 'absolute')
    } else {
      isBottom.value = false
      sticky(props.stickyTop, 'fixed')
    }
    return
  }

  handleReset()
}

const handleScroll = () => {
  /**
   * throttle is added due to the warning of
   * "This site appears to use a scroll-linked
   * positioning effect. This may not work well
   * with asynchronous panning; " (On FireFox)
   */
  _.throttle(updateScroll, 100, { trailing: true, leading: true })()
}

const handleResize = () => {
  if (isSticky.value) {
    width.value = rootEl.value!.getBoundingClientRect().width + 'px'
  }
  updateScroll()
}

onMounted(() => {
  height.value = rootEl.value!.getBoundingClientRect().height
  updateScroll()
  document.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
})

onActivated(() => {
  updateScroll()
})

onUnmounted(() => {
  document.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
})
</script>
