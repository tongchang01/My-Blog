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
          <b v-if="badge" class="article-tag">
            <span class="inline-flex items-center">
              <SvgIcon
                icon-class="hot"
                width="1rem"
                height="1rem"
                stroke="currentColor"
              />
              <span v-if="badge === 'pinned'">{{ t('settings.pinned') }}</span>
              <span v-else>{{ t('settings.featured') }}</span>
            </span>
          </b>
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
            {{ article.publishedAt }} · {{ article.commentCount }}
            {{ t('settings.comments') }}
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
import SvgIcon from '@/components/SvgIcon/index.vue'
import type { ArticleCardViewModel } from '@/features/articles/model'

type HomepageBadge = 'pinned' | 'featured'

const props = defineProps({
  data: {
    type: Object as PropType<ArticleCardViewModel | null>,
    default: null
  },
  badge: {
    type: String as PropType<HomepageBadge>,
    default: null
  }
})

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const { t } = useI18n()
const article = computed(() => props.data)

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

const gradientBackground = computed(() => ({
  background: appStore.themeConfig.theme.header_gradient_css
}))
</script>

<style lang="scss" scoped></style>
