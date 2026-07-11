<template>
  <div class="mt-20">
    <Breadcrumbs :current="pageTitle" />
    <PageContent :post="pageData" :title="pageTitle" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { Page } from '@/models/Article.class'
import PageContent from '@/components/PageContent.vue'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import usePageTitle from '@/hooks/usePageTitle'
import { useCommonStore } from '@/stores/common'
import defaultCover from '@/assets/default-cover.jpg'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import { renderArticleMarkdown } from '@/shared/markdown/render'
import { useAppStore } from '@/stores/app'

const commonStore = useCommonStore()
const siteSettingsStore = useSiteSettingsStore()
const appStore = useAppStore()
const { pageTitle, updateTitle } = usePageTitle()

const pageData = computed(() => {
  const page = new Page()
  const rendered = renderArticleMarkdown(
    siteSettingsStore.settings.aboutMd ?? '',
    appStore.locale
  )
  page.title = pageTitle.value
  page.content = rendered.html
  page.count_time = {
    symbolsTime: rendered.readingTime,
    symbolsCount: rendered.wordCount
  }
  page.toc = rendered.toc
  page.comments = false
  return page
})

const initializePage = () => {
  commonStore.setHeaderImage(defaultCover)
  updateTitle()
}

onMounted(initializePage)

onUnmounted(() => {
  commonStore.resetHeaderImage()
})
</script>

<style lang="scss" scoped></style>
