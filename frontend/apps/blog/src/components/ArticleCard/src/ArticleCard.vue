<template>
  <div class="article-container" @click="navigateToArticle">
    <div class="article">
      <div class="article-thumbnail">
        <img v-if="article?.coverUrl" v-lazy="article.coverUrl" alt="" />
        <img v-else src="@/assets/default-cover.jpg" alt="" />
        <span class="thumbnail-screen" :style="gradientBackground" />
      </div>
      <div class="article-content">
        <span v-if="article">
          <b>{{ article.category?.name || t('settings.default-category') }}</b>
          <b v-if="article.locked" class="article-tag ml-2">🔒</b>
        </span>
        <ob-skeleton v-else tag="b" height="20px" width="60px" />

        <span class="flex flex-wrap">
          <ul v-if="article">
            <li v-for="tag in article.tags.slice(0, 2)" :key="tag.id">
              <em># </em><span>{{ tag.name }}</span>
            </li>
          </ul>
          <ob-skeleton
            v-else
            :count="2"
            tag="span"
            height="16px"
            width="35px"
          />
        </span>

        <h1 v-if="article" data-dia="article-link">{{ article.title }}</h1>
        <ob-skeleton v-else tag="h1" height="3rem" />

        <p v-if="article">{{ article.summary }}</p>
        <ob-skeleton v-else tag="p" :count="4" height="16px" />

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
import type { ArticleCardViewModel } from '@/features/articles/model'
import type { Post } from '@/models/Post.class'

type LegacyPost = Partial<Post>
type CompatibleArticle = ArticleCardViewModel & { legacy: boolean }

const props = defineProps({
  data: {
    type: Object as PropType<ArticleCardViewModel | LegacyPost | null>,
    default: null
  }
})

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const { t } = useI18n()
const article = computed<CompatibleArticle | null>(() => {
  if (!props.data) return null
  if ('id' in props.data) return { ...props.data, legacy: false }

  const category = props.data.categories?.[0]
  return {
    id: props.data.uid ?? props.data.slug ?? '',
    slug: props.data.slug ?? '',
    title: props.data.title ?? '',
    summary: props.data.text ?? '',
    coverUrl: props.data.cover || null,
    category: category ? { id: category.slug, name: category.name } : null,
    tags: (props.data.tags ?? []).map(tag => ({
      id: tag.slug,
      name: tag.name,
      slug: tag.slug
    })),
    publishedAt: props.data.date
      ? `${props.data.date.year}-${props.data.date.day}`
      : '',
    locked: false,
    commentCount: 0,
    legacy: true
  }
})

const navigateToArticle = () => {
  if (!article.value) return
  if (article.value.legacy) {
    if (article.value.slug) {
      router.push({ name: 'post-slug', params: { slug: article.value.slug } })
    }
    return
  }
  router.push({
    name: 'article-detail',
    params: {
      lang: route.params.lang ?? appStore.locale,
      id: article.value.id,
      slug: article.value.slug
    }
  })
}

const gradientBackground = computed(() => ({
  background: appStore.themeConfig.theme.header_gradient_css
}))
</script>

<style lang="scss" scoped></style>
