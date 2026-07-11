<template>
  <div class="mt-20">
    <Breadcrumbs :current="pageTitle" />
    <div class="flex flex-col">
      <div class="post-header">
        <h1 v-if="pageTitle" class="post-title text-white uppercase">
          {{ pageTitle }}
        </h1>
        <ob-skeleton
          v-else
          class="post-title text-white uppercase"
          width="100%"
          height="clamp(1.2rem, calc(1rem + 3.5vw), 4rem)"
        />

        <div class="mt-8 mb-4" />
      </div>

      <div class="mt-8">
        <div
          v-if="error"
          class="flex min-h-48 flex-col items-center justify-center gap-3 rounded-2xl bg-ob-deep-800 p-8 text-ob-dim shadow-xl"
        >
          <p>{{ error }}</p>
          <button
            type="button"
            class="rounded-lg px-4 py-2 text-sm text-white"
            style="background: var(--main-gradient)"
            @click="fetchLinks"
          >
            重试
          </button>
        </div>
        <LinkList v-else-if="!loading" :links="links" />
        <div
          v-else
          class="bg-ob-deep-800 px-14 py-16 rounded-2xl shadow-xl block min-h-screen"
        >
          <ob-skeleton
            tag="div"
            :count="1"
            height="36px"
            width="150px"
            class="mb-6"
          />
          <br />
          <ob-skeleton
            tag="div"
            :count="35"
            height="16px"
            width="100px"
            class="mr-2"
          />
          <br />
          <br />
          <ob-skeleton
            tag="div"
            :count="25"
            height="16px"
            width="100px"
            class="mr-2"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Link } from '@/models/Article.class'
import { useAppStore } from '@/stores/app'
import LinkList from '@/components/Link/LinkList.vue'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import usePageTitle from '@/hooks/usePageTitle'
import { loadPublicFriendLinks } from '@/features/friend-links/api'
import { mapFriendLinks } from '@/features/friend-links/model'

const appStore = useAppStore()
const links = ref<Link[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const { pageTitle, updateTitle } = usePageTitle()

const fetchLinks = async () => {
  loading.value = true
  error.value = null
  try {
    links.value = mapFriendLinks(await loadPublicFriendLinks())
  } catch {
    links.value = []
    error.value = '友情链接加载失败，请稍后重试'
  } finally {
    updateTitle(appStore.locale)
    loading.value = false
  }
}

onMounted(fetchLinks)
</script>
