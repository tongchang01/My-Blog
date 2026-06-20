<template>
  <div id="feature">
    <horizontal-article :data="featurePost" />
    <!-- Feature sub-component -->
    <slot />
  </div>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import HorizontalArticle from '@/components/ArticleCard/src/HorizontalArticle.vue'

const props = defineProps({
  data: Object
})

const featurePost = toRefs(props).data
</script>

<style lang="scss" scoped></style>
