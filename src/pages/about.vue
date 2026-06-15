<template>
  <div class="mt-20">
    <Breadcrumbs :current="pageTitle" />
    <PageContent :post="pageData" :title="pageTitle" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useArticleStore } from '@/stores/article'
import { Page } from '@/models/Article.class'
import PageContent from '@/components/PageContent.vue'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import { useI18n } from 'vue-i18n'
import usePageTitle from '@/hooks/usePageTitle'
import { useCommonStore } from '@/stores/common'
import defaultCover from '@/assets/default-cover.jpg'

const commonStore = useCommonStore()
const articleStore = useArticleStore()
const pageData = ref(new Page())
const { t } = useI18n()
const { pageTitle, updateTitle } = usePageTitle()

const fetchArticle = async () => {
  pageData.value = await articleStore.fetchArticle('about')
  commonStore.setHeaderImage(defaultCover)
  updateTitle()
}

onMounted(fetchArticle)

onUnmounted(() => {
  commonStore.resetHeaderImage()
})
</script>

<style lang="scss" scoped></style>
