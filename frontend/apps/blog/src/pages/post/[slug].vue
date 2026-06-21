<template>
  <div class="flex flex-col mt-20">
    <div v-if="articleStore.detailStatus === 'loading'" class="main-grid">
      <div
        class="bg-ob-deep-800 px-14 py-16 rounded-2xl shadow-xl min-h-screen"
      >
        <ob-skeleton tag="h1" height="4rem" width="80%" class="mb-8" />
        <ob-skeleton
          tag="div"
          :count="30"
          height="16px"
          width="100px"
          class="mr-2"
        />
      </div>
    </div>

    <article
      v-else-if="articleStore.detailStatus === 'ready' && article"
      class="main-grid"
    >
      <div>
        <header class="post-header">
          <span class="post-labels">
            <b>{{
              article.category?.name || t('settings.default-category')
            }}</b>
            <ul>
              <li v-for="tag in article.tags" :key="tag.id">
                <em class="opacity-50">#</em> {{ tag.name }}
              </li>
            </ul>
          </span>
          <h1 class="post-title text-white">{{ article.title }}</h1>
          <p class="text-white opacity-80 mt-5">
            {{ article.publishedAt }} · {{ article.commentCount }} comments
          </p>
        </header>

        <div class="post-html" v-html="article.bodyHtml" />
      </div>
    </article>

    <div v-else class="py-32 text-center text-ob-dim">
      <h1 class="text-3xl text-ob-bright">{{ stateTitle }}</h1>
      <p class="mt-4">{{ stateMessage }}</p>
      <button
        v-if="articleStore.detailStatus === 'error'"
        class="mt-6 px-5 py-2 rounded-full text-ob-bright"
        :style="retryStyle"
        @click="retry"
      >
        {{ retryLabel }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useArticleStore } from '@/features/articles/store'
import { isSupportedLocale } from '@/shared/i18n/locale'
import { useAppStore } from '@/stores/app'
import { useCommonStore } from '@/stores/common'
import { useMetaStore } from '@/stores/meta'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const articleStore = useArticleStore()
const commonStore = useCommonStore()
const metaStore = useMetaStore()
const { t } = useI18n()
const invalidRoute = ref(false)

const article = computed(() => articleStore.detail)
const locale = computed(() =>
  isSupportedLocale(route.params.lang) ? route.params.lang : null
)
const state = computed(() =>
  invalidRoute.value ? 'notFound' : articleStore.detailStatus
)
const stateTitle = computed(() => {
  if (state.value === 'locked') return '🔒'
  return state.value === 'notFound' ? '404' : 'Error'
})
const stateMessage = computed(() => {
  const lang = locale.value ?? appStore.locale
  if (state.value === 'locked') {
    return lang === 'zh'
      ? '该文章需要密码，首版暂不支持解锁'
      : lang === 'ja'
        ? 'この記事はパスワードで保護されています'
        : 'This article is password protected'
  }
  if (state.value === 'notFound') {
    return lang === 'zh'
      ? '文章不存在或尚未发布'
      : lang === 'ja'
        ? '記事が見つかりません'
        : 'Article not found'
  }
  return lang === 'zh'
    ? '文章加载失败，请稍后重试'
    : lang === 'ja'
      ? '記事を読み込めませんでした'
      : 'Unable to load the article'
})
const retryLabel = computed(() =>
  (locale.value ?? appStore.locale) === 'zh'
    ? '重试'
    : (locale.value ?? appStore.locale) === 'ja'
      ? '再試行'
      : 'Retry'
)
const retryStyle = computed(() => ({
  background: appStore.themeConfig.theme.header_gradient_css
}))

const replaceCanonicalSlug = async (slug: string | null) => {
  if (!slug || !article.value || route.params.slug === slug) return
  await router.replace({
    name: 'article-detail',
    params: {
      lang: locale.value,
      id: article.value.id,
      slug
    },
    query: route.query,
    hash: route.hash
  })
}

const load = async () => {
  const id = typeof route.params.id === 'string' ? route.params.id : null
  if (!id || !locale.value) {
    invalidRoute.value = true
    return
  }
  invalidRoute.value = false
  const slug = await articleStore.loadDetail(id, locale.value)
  if (article.value) {
    metaStore.setTitle(article.value.title)
    commonStore.setHeaderImage(article.value.coverUrl ?? '')
  }
  await replaceCanonicalSlug(slug)
}

const retry = async () => {
  const slug = await articleStore.retryDetail()
  await replaceCanonicalSlug(slug)
}

onMounted(load)
watch(
  () => [route.params.lang, route.params.id],
  ([nextLang, nextId], [previousLang, previousId]) => {
    if (nextLang !== previousLang || nextId !== previousId) void load()
  }
)
onBeforeUnmount(() => commonStore.resetHeaderImage())
</script>
