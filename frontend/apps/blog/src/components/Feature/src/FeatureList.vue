<template>
  <div
    :class="[
      semantic ? 'inverted-main-grid' : 'feature-card-grid',
      'py-7 gap-7 box-border'
    ]"
  >
    <div
      v-if="semantic"
      class="relative overflow-hidden h-56 lg:h-auto rounded-2xl bg-ob-deep-800 shadow-lg"
    >
      <div
        class="ob-gradient-plate opacity-90 relative z-10 bg-ob-deep-900 rounded-2xl flex justify-start items-end px-6 pb-10 shadow-md"
      >
        <h2 class="text-3xl pb-8 lg:pb-14">
          <p :style="gradientText">EDITOR'S SELECTION</p>
          <span class="relative text-lg text-ob-bright font-semibold">
            <SvgIcon class="inline-block" icon-class="hot" stroke="white" />
            {{ t('home.recommended') }}
          </span>
        </h2>
      </div>
      <span
        class="absolute top-0 w-full h-full z-0"
        :style="gradientBackground"
      />
    </div>

    <ul class="grid lg:grid-cols-2 gap-7">
      <template v-if="featurePosts.length > 0">
        <li v-for="post in featurePosts" :key="post.slug">
          <ArticleCard :data="post" badge="featured" />
        </li>
      </template>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { useI18n } from 'vue-i18n'
import { PropType, StyleValue, computed, toRefs } from 'vue'
import { ArticleCard } from '@/components/ArticleCard'
import SvgIcon from '@/components/SvgIcon/index.vue'
import type { ArticleCardViewModel } from '@/features/articles/model'

const props = defineProps({
  data: {
    type: Array as PropType<ArticleCardViewModel[]>,
    required: true
  },
  semantic: {
    type: Boolean,
    default: true
  }
})

const appStore = useAppStore()
const { data: featurePosts, semantic } = toRefs(props)
const { t } = useI18n()

const gradientBackground = computed(() => ({
  background: appStore.themeConfig.theme.header_gradient_css
}))
const gradientText = computed(
  () => appStore.themeConfig.theme.background_gradient_style as StyleValue
)
</script>

<style lang="scss" scoped>
.feature-card-grid {
  display: grid;
}
</style>
