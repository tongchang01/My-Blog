<template>
  <div id="feature">
    <horizontal-article :data="featurePost" />
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
  }
})

const featurePost = toRefs(props).data
</script>

<style lang="scss" scoped></style>
