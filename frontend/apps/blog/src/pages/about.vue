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
import { renderMarkdown } from '@/shared/markdown/render'

const commonStore = useCommonStore()
const siteSettingsStore = useSiteSettingsStore()
const { pageTitle, updateTitle } = usePageTitle()

const pageData = computed(() => {
  const page = new Page()
  page.title = pageTitle.value
  page.content = siteSettingsStore.settings.aboutMd
    ? renderMarkdown(siteSettingsStore.settings.aboutMd)
    : ''
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
