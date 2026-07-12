<template>
  <div class="flex flex-col mt-20">
    <Breadcrumbs :current="pageTitle" />
    <div class="post-header">
      <h1 class="post-title text-white uppercase">{{ pageTitle }}</h1>
    </div>
    <div class="bg-ob-deep-800 px-14 py-16 rounded-2xl shadow-xl block">
      <TagList>
        <template v-if="categories && categories.length > 0">
          <TagItem
            v-for="category in categories"
            :key="category.slug"
            :name="category.name"
            :slug="category.slug"
            :count="category.count"
            route-name="category-articles"
            size="large"
          />
        </template>
        <template v-else-if="categories">
          <ob-skeleton tag="li" :count="10" height="20px" width="3rem" />
        </template>
        <template v-else>
          <div class="flex flex-row justify-center items-center">
            <SvgIcon class="stroke-ob-bright mr-2" icon-class="warning" />
            {{ t('settings.empty-category') }}
          </div>
        </template>
      </TagList>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeMount, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import SvgIcon from '@/components/SvgIcon/index.vue'
import defaultCover from '@/assets/default-cover.jpg'
import { useCommonStore } from '@/stores/common'
import { useAppStore } from '@/stores/app'
import { useTaxonomyStore } from '@/features/taxonomy/store'
import { TagList, TagItem } from '@/components/Tag'
import usePageTitle from '@/hooks/usePageTitle'

const commonStore = useCommonStore()
const appStore = useAppStore()
const taxonomyStore = useTaxonomyStore()
const { t } = useI18n()
const { pageTitle, updateTitle } = usePageTitle()

const categories = computed(() => {
  if (taxonomyStore.categoryStatus === 'empty') return null
  return taxonomyStore.categories
})

const fetchData = async () => {
  await taxonomyStore.loadCategories(appStore.locale)
  updateTitle()
  commonStore.setHeaderImage(defaultCover)
}

onBeforeMount(fetchData)
onUnmounted(() => {
  commonStore.resetHeaderImage()
})
</script>
