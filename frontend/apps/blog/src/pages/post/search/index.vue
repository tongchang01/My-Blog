<template>
  <div class="flex flex-col mt-10">
    <div class="post-header">
      <!-- <Breadcrumbs :current="t(pageType)" /> -->
      <div class="flex flex-row gap-8">
        <h1 v-if="categoryTitle" class="post-title text-white uppercase">
          <span class="opacity-60">
            <SvgIcon icon-class="category" stroke="white" />
          </span>
          {{ categoryTitle }}
        </h1>
        <h1 v-if="tagTitle" class="post-title text-white uppercase">
          <span class="opacity-60">
            <SvgIcon icon-class="tag" stroke="white" />
          </span>
          {{ tagTitle }}
        </h1>
      </div>
    </div>
    <div class="main-grid">
      <div class="relative">
        <transition name="fade-slide-y" mode="out-in">
          <div v-show="isEmpty" class="post-html flex flex-col items-center">
            <h1>{{ t('settings.no-search-result') }}</h1>
            <SvgIcon icon-class="empty-search" style="font-size: 35rem" />
          </div>
        </transition>
        <div class="flex flex-col relative">
          <ul class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
            <template v-if="isLoading || articles.length === 0">
              <li v-for="n in 12" :key="n">
                <ArticleCard :data="{}" />
              </li>
            </template>
            <template v-else>
              <li v-for="post in articles" :key="post.id">
                <ArticleCard :data="post" />
              </li>
            </template>
          </ul>

          <Paginator
            :pageSize="pagination.pageSize"
            :pageTotal="pagination.pageTotal"
            :page="pagination.page"
            @pageChange="pageChangeHandler"
          />
        </div>
      </div>
      <div>
        <Sidebar>
          <div class="sidebar-box flex flex-col gap-8">
            <CategoryBox
              :sidebar-box="false"
              :active-category="categoryTitle"
            />
            <TagBox :sidebar-box="false" :active-tag="tagTitle" />
          </div>
        </Sidebar>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeMount, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Sidebar, TagBox, CategoryBox } from '@/components/Sidebar'
import Paginator from '@/components/Paginator.vue'
import { ArticleCard } from '@/components/ArticleCard'
import { useRoute } from 'vue-router'
import { useMetaStore } from '@/stores/meta'
import SvgIcon from '@/components/SvgIcon/index.vue'
import { useArticleStore } from '@/features/articles/store'
import { useAppStore } from '@/stores/app'

const { t } = useI18n()
const route = useRoute()
const articleStore = useArticleStore()
const appStore = useAppStore()
const metaStore = useMetaStore()

const queryTagKey = 'aurora-query-tag'
const queryCategoryKey = 'aurora-query-category'
const queryTag = ref('')
const queryCategory = ref('')

const initPage = async (page = 1) => {
  if (queryTag.value) {
    await articleStore.load({
      page,
      size: 12,
      lang: appStore.locale,
      tagSlug: queryTag.value
    })
  } else if (queryCategory.value) {
    await articleStore.load({
      page,
      size: 12,
      lang: appStore.locale,
      categorySlug: queryCategory.value
    })
  }

  window.scrollTo({
    top: 0
  })

  metaStore.setTitle('search')
}

const firstQueryValue = (value: unknown): string => {
  if (Array.isArray(value)) return value[0] ?? ''
  return typeof value === 'string' ? value : ''
}

const pageChangeHandler = (page = 1) => {
  queryCategory.value = ''
  queryTag.value = ''
  const { tag, category } = route.query
  const slug = firstQueryValue(route.params.slug)

  if (route.name === 'category-articles' && slug) {
    queryCategory.value = slug
  } else if (route.name === 'tag-articles' && slug) {
    queryTag.value = slug
  } else if (category) {
    queryCategory.value = firstQueryValue(category)
  } else if (tag) {
    queryTag.value = firstQueryValue(tag)
  }

  if (queryTag.value || queryCategory.value) {
    void initPage(page)
  }
}

watch(
  () => route.fullPath,
  () => {
    pageChangeHandler()
  }
)

onBeforeMount(() => {
  pageChangeHandler()
})

onUnmounted(() => {
  localStorage.removeItem(queryTagKey)
  localStorage.removeItem(queryCategoryKey)
})

const articles = computed(() => articleStore.page.records)
const isLoading = computed(() => articleStore.status === 'loading')
const isEmpty = computed(() => articleStore.status === 'empty')
const pagination = computed(() => ({
  pageSize: articleStore.page.size,
  pageTotal: articleStore.page.total,
  page: articleStore.page.page
}))
const categoryTitle = computed(() => queryCategory.value)
const tagTitle = computed(() => queryTag.value)
</script>

<style lang="scss" scoped></style>
