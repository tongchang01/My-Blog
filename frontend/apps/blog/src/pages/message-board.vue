<template>
  <div class="mt-20">
    <Breadcrumbs :current="pageTitle" />
    <Comment guestbook title="menu.message-board" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import Comment from '@/components/Comment.vue'
import { useMetaStore } from '@/stores/meta'
import { useCommonStore } from '@/stores/common'
import defaultCover from '@/assets/default-cover.jpg'

const { t, locale } = useI18n()
const metaStore = useMetaStore()
const commonStore = useCommonStore()
const pageTitle = computed(() => t('menu.message-board'))

const updateTitle = () => metaStore.setTitle(pageTitle.value)

onMounted(() => {
  commonStore.setHeaderImage(defaultCover)
  updateTitle()
})

watch(locale, updateTitle)

onUnmounted(() => {
  commonStore.resetHeaderImage()
})
</script>
