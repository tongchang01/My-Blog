<template>
  <div class="article-container" @click="navigateToArticle">
    <div class="feature-article">
      <div class="feature-thumbnail">
        <img
          v-if="article?.coverUrl"
          class="ob-hz-thumbnail"
          v-lazy="article.coverUrl"
          alt=""
        />
        <img
          v-else
          class="ob-hz-thumbnail"
          src="@/assets/default-cover.jpg"
          alt=""
        />
        <span class="thumbnail-screen" :style="bannerHoverGradient" />
      </div>
      <div class="feature-content">
        <span v-if="article">
          <b>{{ article.category?.name || t('settings.default-category') }}</b>
          <b v-if="article.locked" class="article-tag ml-2">🔒</b>
        </span>
        <ob-skeleton v-else tag="b" height="20px" width="60px" />

        <ul v-if="article" class="flex flex-wrap">
          <li v-for="tag in visibleTags" :key="tag.id">
            <em># </em><span>{{ tag.name }}</span>
          </li>
        </ul>

        <h1 v-if="article" data-dia="article-link">{{ article.title }}</h1>
        <ob-skeleton v-else tag="h1" height="3rem" />

        <p v-if="article">{{ article.summary }}</p>
        <ob-skeleton v-else tag="p" :count="3" height="20px" />

        <div v-if="article" class="article-footer">
          <span class="text-ob-dim">
            {{ article.publishedAt }} · {{ article.commentCount }} comments
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useCommonStore } from '@/stores/common'
import type { ArticleCardViewModel } from '@/features/articles/model'

const props = defineProps({
  data: {
    type: Object as PropType<ArticleCardViewModel | null>,
    default: null
  }
})

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const commonStore = useCommonStore()
const { t } = useI18n()
const article = computed(() => props.data)
const visibleTags = computed(
  () => article.value?.tags.slice(0, commonStore.isMobile ? 2 : 5) ?? []
)

const navigateToArticle = () => {
  if (!article.value) return
  router.push({
    name: 'article-detail',
    params: {
      lang: route.params.lang ?? appStore.locale,
      id: article.value.id,
      slug: article.value.slug
    }
  })
}

const bannerHoverGradient = computed(() => ({
  background: appStore.themeConfig.theme.header_gradient_css
}))
</script>
