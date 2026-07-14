<template>
  <div class="block mt-8">
    <Feature
      v-if="showFeature && mainArticle"
      :data="mainArticle"
      :badge="hasPinnedArticle ? 'pinned' : 'featured'"
    >
      <FeatureList
        v-if="featureCards.length > 0"
        :data="featureCards"
        :semantic="hasFeaturedArticles"
      />
    </Feature>
    <HorizontalArticle
      v-else-if="mainArticle"
      class="mb-8"
      :data="mainArticle"
    />

    <div class="main-grid">
      <div id="article-list" class="flex flex-col relative">
        <ul
          v-if="
            articleStore.homeStatus === 'loading' ||
            articleStore.homeStatus === 'error'
          "
          class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6"
        >
          <li v-for="n in 6" :key="n"><ArticleCard :data="null" /></li>
        </ul>

        <div
          v-else-if="articleStore.homeStatus === 'empty'"
          class="py-24 text-center text-ob-dim"
        >
          {{ emptyMessage }}
        </div>

        <ul v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          <li v-for="article in listArticles" :key="article.id">
            <ArticleCard :data="article" />
          </li>
        </ul>
      </div>
      <Sidebar>
        <Profile author="blog-author" />
      </Sidebar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Feature, FeatureList } from '@/components/Feature'
import { ArticleCard, HorizontalArticle } from '@/components/ArticleCard'
import { Profile, Sidebar } from '@/components/Sidebar'
import { useArticleStore } from '@/features/articles/store'
import { isSupportedLocale } from '@/shared/i18n/locale'
import { useAppStore } from '@/stores/app'
import { useMetaStore } from '@/stores/meta'

useMetaStore().setTitle('')
const route = useRoute()
const appStore = useAppStore()
const articleStore = useArticleStore()
const DEFAULT_PAGE_SIZE = 12

const currentLocale = computed(() =>
  isSupportedLocale(route.params.lang) ? route.params.lang : appStore.locale
)
const showFeature = computed(() => appStore.themeConfig.theme.feature)
const hasPinnedArticle = computed(
  () => articleStore.home.pinnedArticle !== null
)
const hasFeaturedArticles = computed(
  () => articleStore.home.featuredArticles.length > 0
)
const mainArticle = computed(
  () => articleStore.home.pinnedArticle ?? articleStore.home.articles[0] ?? null
)
const ordinaryConsumedByMain = computed(() => (hasPinnedArticle.value ? 0 : 1))
const featureCards = computed(() => {
  if (hasFeaturedArticles.value) return articleStore.home.featuredArticles
  if (!showFeature.value) return []
  return articleStore.home.articles.slice(
    ordinaryConsumedByMain.value,
    ordinaryConsumedByMain.value + 2
  )
})
const listArticles = computed(() => {
  const articles = articleStore.home.articles
  const consumed = showFeature.value
    ? ordinaryConsumedByMain.value +
      (hasFeaturedArticles.value ? 0 : featureCards.value.length)
    : ordinaryConsumedByMain.value
  return hasFeaturedArticles.value && !showFeature.value
    ? [...articleStore.home.featuredArticles, ...articles.slice(consumed)]
    : articles.slice(consumed)
})
const emptyMessage = computed(() =>
  currentLocale.value === 'zh'
    ? '暂时没有公开文章'
    : currentLocale.value === 'ja'
      ? '公開記事はまだありません'
      : 'No public articles yet'
)
const loadHome = async () => {
  await articleStore.loadHome({
    size: DEFAULT_PAGE_SIZE,
    lang: currentLocale.value
  })
}

onMounted(async () => {
  await loadHome()
})

watch(
  () => route.params.lang,
  async (next, previous) => {
    if (next !== previous && isSupportedLocale(next)) {
      await loadHome()
    }
  }
)
</script>

<style lang="scss" scoped></style>
