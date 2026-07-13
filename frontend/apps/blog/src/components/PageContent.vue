<template>
  <div class="flex flex-col">
    <div class="post-header">
      <h1 v-if="post.title" class="post-title text-white uppercase">
        {{ pageTitle }}
      </h1>
      <ob-skeleton
        v-else
        class="post-title text-white uppercase"
        width="100%"
        height="clamp(1.2rem, calc(1rem + 3.5vw), 4rem)"
      />

      <div class="flex flex-row items-center justify-start mt-8">
        <PostStats
          :post-word-count="post.count_time.symbolsCount"
          :post-time-count="post.count_time.symbolsTime"
        />
      </div>
    </div>
    <div class="main-grid">
      <div class="relative">
        <div
          v-if="post.content"
          ref="postHtml"
          class="post-html"
          v-html="post.content"
          v-scroll-spy="{ sectionSelector: 'h1, h2, h3, h4, h5, h6' }"
        />
        <div
          v-else
          class="bg-ob-deep-800 px-14 py-16 rounded-2xl shadow-xl block min-h-screen"
        >
          <ob-skeleton
            tag="div"
            :count="1"
            height="36px"
            width="150px"
            class="mb-6"
          />
          <br />
          <ob-skeleton
            tag="div"
            :count="35"
            height="16px"
            width="100px"
            class="mr-2"
          />
          <br />
          <br />
          <ob-skeleton
            tag="div"
            :count="25"
            height="16px"
            width="100px"
            class="mr-2"
          />
        </div>
        <slot />
      </div>
      <div class="col-span-1">
        <Sidebar>
          <Profile author="blog-author" />
          <Toc :toc="post.toc" :comments="false" />
        </Sidebar>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
  toRefs,
  watch
} from 'vue'
import { Profile, Sidebar, Toc } from '@/components/Sidebar'
import { useCommonStore } from '@/stores/common'
import PostStats from './Post/PostStats.vue'
import useLightBox from '@/hooks/useLightBox'
import ObSkeleton from '@/components/LoadingSkeleton/Skeleton.vue'
import { useAppStore } from '@/stores/app'
import { enhanceMarkdown } from '@/shared/markdown/enhance'

const props = defineProps({
  post: {
    type: Object,
    default: () => {
      return {}
    }
  },
  title: {
    type: String,
    default: ''
  }
})

const commonStore = useCommonStore()
const appStore = useAppStore()
const post = toRefs(props).post
const title = toRefs(props).title
const postHtml = ref<HTMLElement | null>(null)
const { initializeLightBox } = useLightBox()

const initializeContent = async () => {
  await nextTick()
  if (!postHtml.value) return
  initializeLightBox()
  await enhanceMarkdown(
    postHtml.value,
    appStore.theme === 'theme-dark',
    appStore.locale
  )
}

watch(
  () => post.value.covers,
  value => {
    if (value) commonStore.setHeaderImage(value)
  }
)

watch(
  () => post.value.content,
  async value => {
    if (value) await initializeContent()
  }
)

watch(
  () => appStore.theme,
  () => {
    if (postHtml.value)
      void enhanceMarkdown(
        postHtml.value,
        appStore.theme === 'theme-dark',
        appStore.locale
      )
  }
)

onMounted(() => {
  commonStore.setHeaderImage(post.value.covers)
  if (post.value.content) void initializeContent()
})

onUnmounted(() => {
  commonStore.resetHeaderImage()
})

const pageTitle = computed(() => {
  if (title.value !== '') return title.value
  return post.value.title
})
</script>

<style lang="scss" scoped>
.post-title {
  @apply mt-6 mb-4;
  font-size: clamp(1.2rem, calc(1rem + 3.5vw), 3rem);
  text-shadow: 0 2px 2px rgba(0, 0, 0, 0.5);
  line-height: 1.1;
}
.post-stats {
  @apply w-full flex flex-row text-xs lg:text-sm mb-6;
  span {
    @apply text-white stroke-current flex flex-row items-center pr-4;
  }
}
</style>
