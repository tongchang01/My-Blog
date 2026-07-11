<template>
  <div class="flex flex-col mt-20">
    <Breadcrumbs :current="pageTitle" />
    <div class="post-header">
      <h1 class="post-title text-white uppercase">{{ pageTitle }}</h1>
    </div>
    <div class="bg-ob-deep-800 px-14 py-16 rounded-2xl shadow-xl block">
      <ul class="flex flex-row flex-wrap justify-center gap-3">
        <template v-if="categories && categories.length > 0">
          <li v-for="category in categories" :key="category.slug">
            <router-link
              class="category-item"
              :to="{
                name: 'category-articles',
                params: { lang: appStore.locale, slug: category.slug }
              }"
            >
              {{ category.name }}
              <sub>{{ category.count }}</sub>
            </router-link>
          </li>
        </template>
        <template v-else-if="categories">
          <li><ob-skeleton :count="10" height="20px" width="3rem" /></li>
        </template>
        <template v-else>
          <li class="flex flex-row justify-center items-center">
            <SvgIcon class="stroke-ob-bright mr-2" icon-class="warning" />
            {{ t('settings.empty-category') }}
          </li>
        </template>
      </ul>
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

<style lang="scss" scoped>
.category-item {
  @apply flex py-2 px-4 rounded-md bg-ob-deep-900 text-ob-bright text-base hover:opacity-100;

  sub {
    @apply block ml-2 rounded-full text-xs text-ob;
  }
}
</style>
