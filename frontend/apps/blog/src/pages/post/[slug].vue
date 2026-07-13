<template>
  <div class="flex flex-col mt-20">
    <div
      v-if="
        articleStore.detailStatus === 'loading' ||
        articleStore.detailStatus === 'error'
      "
      class="main-grid"
    >
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
      class="flex flex-col"
    >
      <header class="post-header">
        <span class="post-labels">
          <b>{{ article.category?.name || t('settings.default-category') }}</b>
          <ul>
            <li v-for="tag in article.tags" :key="tag.id">
              <em class="opacity-50">#</em> {{ tag.name }}
            </li>
          </ul>
        </span>
        <h1 class="post-title text-white">{{ article.title }}</h1>
        <div class="flex flex-row items-center justify-start mt-8">
          <span class="text-white opacity-80 mr-4">
            {{ t('settings.published-at') }} {{ article.publishedAt }}
            <template v-if="article.updatedAt !== article.publishedAt">
              · {{ t('settings.updated-at') }} {{ article.updatedAt }}
            </template>
            · {{ article.commentCount }} {{ t('settings.comments') }}
          </span>
          <PostStats
            :post-word-count="article.wordCount"
            :post-time-count="article.readingTime"
          />
        </div>
      </header>

      <div class="main-grid">
        <div>
          <div
            ref="postHtml"
            class="post-html"
            v-html="article.bodyHtml"
            v-scroll-spy="{ sectionSelector: 'h1, h2, h3, h4, h5, h6' }"
          />
          <div id="comments">
            <Comment :article-id="article.id" :enabled="!article.locked" />
          </div>
        </div>
        <div class="col-span-1">
          <Sidebar>
            <Profile author="blog-author" />
            <Toc :toc="article.toc" :comments="true" />
          </Sidebar>
        </div>
      </div>
    </article>

    <div v-else class="py-32 text-center text-ob-dim">
      <h1 class="text-3xl text-ob-bright">{{ stateTitle }}</h1>
      <p class="mt-4">{{ stateMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useArticleStore } from '@/features/articles/store'
import { isSupportedLocale } from '@/shared/i18n/locale'
import Comment from '@/components/Comment.vue'
import { useAppStore } from '@/stores/app'
import { useCommonStore } from '@/stores/common'
import { useMetaStore } from '@/stores/meta'
import { Profile, Sidebar, Toc } from '@/components/Sidebar'
import PostStats from '@/components/Post/PostStats.vue'
import useLightBox from '@/hooks/useLightBox'
import { enhanceMarkdown } from '@/shared/markdown/enhance'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const articleStore = useArticleStore()
const commonStore = useCommonStore()
const metaStore = useMetaStore()
const { t } = useI18n()
const invalidRoute = ref(false)
const postHtml = ref<HTMLElement | null>(null)
const { initializeLightBox } = useLightBox()

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
  return ''
})

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

onMounted(load)
watch(
  () => [route.params.lang, route.params.id],
  ([nextLang, nextId], [previousLang, previousId]) => {
    if (nextLang !== previousLang || nextId !== previousId) void load()
  }
)
watch(
  () => article.value?.bodyHtml,
  async value => {
    if (!value) return
    await nextTick()
    if (!postHtml.value) return
    initializeLightBox()
    await enhanceMarkdown(
      postHtml.value,
      appStore.theme === 'theme-dark',
      locale.value ?? appStore.locale
    )
  }
)
watch(
  () => appStore.theme,
  () => {
    if (postHtml.value)
      void enhanceMarkdown(
        postHtml.value,
        appStore.theme === 'theme-dark',
        locale.value ?? appStore.locale
      )
  }
)
onBeforeUnmount(() => commonStore.resetHeaderImage())
</script>
