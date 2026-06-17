<template>
  <div class="mt-20">
    <Breadcrumbs :current="pageTitle" />
    <PageContent :post="pageData" :title="pageTitle">
      <template v-if="enabledComment">
        <div id="comments">
          <Comment
            :title="pageData.title"
            :body="pageData.text"
            :uid="pageData.uid"
          />
        </div>
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeMount, ref, watch } from 'vue'
import { useArticleStore } from '@/stores/article'
import { Page } from '@/models/Article.class'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useMetaStore } from '@/stores/meta'
import PageContent from '@/components/PageContent.vue'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import Comment from '@/components/Comment.vue'
import useCommentPlugin from '@/hooks/useCommentPlugin'
import { Locales } from '@/models/ThemeConfig.class'

const articleStore = useArticleStore()
const appStore = useAppStore()
const metaStore = useMetaStore()
const pageData = ref(new Page())
const route = useRoute()
const pageTitleRef = ref()
const { enabledCommentPlugin } = useCommentPlugin()

const updateTitle = (locale?: Locales) => {
  const currentLocale = locale ?? 'en'
  const routeInfo = appStore.themeConfig.menu.menus[String(route.params.slug)]
  pageTitleRef.value =
    (routeInfo.i18n && routeInfo.i18n[currentLocale]) || routeInfo.name
  metaStore.setTitle(pageTitleRef.value)
}

const fetchArticle = async () => {
  const response = await articleStore.fetchArticle(String(route.params.slug))
  pageData.value = response
  pageTitleRef.value = response.title
  updateTitle(appStore.locale)
}

watch(
  () => appStore.locale,
  value => {
    if (value) {
      updateTitle(value)
    }
  }
)

onBeforeMount(fetchArticle)

const enabledComment = computed(
  () => pageData.value.comments && enabledCommentPlugin.value.plugin !== ''
)
const pageTitle = computed(() => pageTitleRef.value)
</script>

<style lang="scss" scoped></style>
