<template>
  <div id="feature">
    <horizontal-article :data="featurePost" :badge="badge" />
    <!-- Feature sub-component -->
    <slot />
  </div>
</template>

<script setup lang="ts">
import { toRefs, type PropType } from 'vue'
import HorizontalArticle from '@/components/ArticleCard/src/HorizontalArticle.vue'
import type { ArticleCardViewModel } from '@/features/articles/model'

const props = defineProps({
  data: {
    type: Object as PropType<ArticleCardViewModel>,
    required: true
  },
  badge: {
    type: String as PropType<'pinned' | 'featured'>,
    default: 'featured'
  }
})

const { data: featurePost, badge } = toRefs(props)
</script>

<style lang="scss" scoped></style>
