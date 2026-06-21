<template>
  <div class="block mt-8">
    <Feature v-if="showFeature && featuredArticle" :data="featuredArticle">
      <FeatureList :data="secondaryFeatures" />
    </Feature>
    <HorizontalArticle
      v-else-if="!showFeature && featuredArticle"
      class="mb-8"
      :data="featuredArticle"
    />

    <div id="article-list" class="flex flex-col relative">
      <ul
        v-if="articleStore.status === 'loading'"
        class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6"
      >
        <li v-for="n in 6" :key="n"><ArticleCard :data="null" /></li>
      </ul>

      <div
        v-else-if="articleStore.status === 'empty'"
        class="py-24 text-center text-ob-dim"
      >
        {{ emptyMessage }}
      </div>

      <div
        v-else-if="articleStore.status === 'error'"
        class="py-24 text-center text-ob-dim"
      >
        <p>{{ errorMessage }}</p>
        <button
          class="mt-4 px-5 py-2 rounded-full text-ob-bright"
          :style="retryStyle"
          @click="retry"
        >
          {{ retryLabel }}
        </button>
      </div>

      <ul v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        <li v-for="article in listArticles" :key="article.id">
          <ArticleCard :data="article" />
        </li>
      </ul>

      <Paginator
        v-if="articleStore.page.total > articleStore.page.size"
        :pageSize="articleStore.page.size"
        :pageTotal="articleStore.page.total"
        :page="articleStore.page.page"
        @pageChange="pageChangeHandler"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Feature, FeatureList } from '@/components/Feature'
import { ArticleCard, HorizontalArticle } from '@/components/ArticleCard'
import Paginator from '@/components/Paginator.vue'
import { useArticleStore } from '@/features/articles/store'
import { isSupportedLocale } from '@/shared/i18n/locale'
import { useAppStore } from '@/stores/app'
import { useMetaStore } from '@/stores/meta'
import usePageTitle from '@/hooks/usePageTitle'

useMetaStore().setTitle('home')
const route = useRoute()
const appStore = useAppStore()
const articleStore = useArticleStore()
const { updateTitleByText } = usePageTitle()
const articleOffset = ref(0)
const DEFAULT_PAGE_SIZE = 12

const currentLocale = computed(() =>
  isSupportedLocale(route.params.lang) ? route.params.lang : appStore.locale
)
const showFeature = computed(() => appStore.themeConfig.theme.feature)
const featuredArticle = computed(() => articleStore.page.records[0] ?? null)
const secondaryFeatures = computed(() => articleStore.page.records.slice(1, 4))
const listArticles = computed(() =>
  articleStore.page.records.slice(showFeature.value ? 4 : 1)
)
const emptyMessage = computed(() =>
  currentLocale.value === 'zh'
    ? '暂时没有公开文章'
    : currentLocale.value === 'ja'
      ? '公開記事はまだありません'
      : 'No public articles yet'
)
const errorMessage = computed(() =>
  currentLocale.value === 'zh'
    ? '文章加载失败，请稍后重试'
    : currentLocale.value === 'ja'
      ? '記事を読み込めませんでした'
      : 'Unable to load articles'
)
const retryLabel = computed(() =>
  currentLocale.value === 'zh'
    ? '重试'
    : currentLocale.value === 'ja'
      ? '再試行'
      : 'Retry'
)
const retryStyle = computed(() => ({
  background: appStore.themeConfig.theme.header_gradient_css
}))

const updateArticleOffset = async () => {
  await nextTick()
  articleOffset.value = document.getElementById('article-list')?.offsetTop ?? 0
}

const loadPage = async (page: number) => {
  await articleStore.load({
    page,
    size: DEFAULT_PAGE_SIZE,
    lang: currentLocale.value
  })
  await updateArticleOffset()
}

const backToArticleTop = () => {
  window.scrollTo({ top: articleOffset.value, behavior: 'smooth' })
}

const pageChangeHandler = async (page: number) => {
  backToArticleTop()
  await loadPage(page)
}

const retry = async () => articleStore.retry()

onMounted(async () => {
  await loadPage(1)
  updateTitleByText(appStore.siteSubtitle ?? '')
})

watch(
  () => route.params.lang,
  async (next, previous) => {
    if (next !== previous && isSupportedLocale(next)) await loadPage(1)
  }
)
</script>

<style lang="scss" scoped></style>
