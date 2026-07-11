<template>
  <div
    class="h-98 w-full rounded-2xl relative shadow-xl mb-8"
    :style="gradientBackground"
  >
    <div
      class="ob-gradient-cut-plate absolute bg-ob-deep-900 rounded-xl opacity-90 flex justify-center items-center pt-4 px-6 shadow-lg hover:shadow-2xl duration-300"
      data-dia="author"
    >
      <div
        class="profile absolute w-full flex flex-col justify-center items-center"
      >
        <div class="flex flex-col justify-center items-center">
          <img
            v-if="authorData.avatar !== ''"
            :class="avatarClass"
            :src="authorData.avatar"
            alt="avatar"
          />
          <ob-skeleton v-else width="6.4rem" height="6.4rem" circle />

          <h2 class="text-center pt-2 text-3xl font-semibold text-ob-bright">
            <template v-if="authorData.name">
              {{ authorData.name }}
            </template>
            <ob-skeleton v-else height="2.25rem" width="7rem" />
          </h2>
          <span
            class="h-1 w-14 rounded-full mt-2"
            :style="gradientBackground"
          />
        </div>
        <div
          class="h-full w-full flex flex-col flex-1 justify-center items-end"
        >
          <p
            v-if="authorData.description"
            class="flex-1 pt-6 px-8 w-full text-sm leading-8 text-center"
            v-html="authorData.description"
          />
          <p
            v-else
            class="pt-8 px-8 w-full text-sm text-center flex flex-col gap-2 justify-center"
          >
            <ob-skeleton :count="2" height="20px" width="100%" />
          </p>

          <Social :socials="authorData.socials" />
          <ul class="grid grid-cols-3 pt-2 w-full px-2 text-lg">
            <li class="col-span-1 text-center">
              <span class="text-ob-bright">
                {{ authorData.post_list.length }}
              </span>
              <p class="text-xs">{{ t('settings.articles') }}</p>
            </li>
            <li class="col-span-1 text-center">
              <span class="text-ob-bright">{{ authorData.categories }}</span>
              <p class="text-xs">{{ t('settings.categories') }}</p>
            </li>
            <li class="col-span-1 text-center">
              <span class="text-ob-bright">{{ authorData.tags }}</span>
              <p class="text-xs">{{ t('settings.tags') }}</p>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Social from '@/components/Social.vue'
import { useAuthorProfileStore } from '@/features/author-profile/store'

defineProps({
  author: {
    type: String,
    default: () => {
      return ''
    }
  }
})

const appStore = useAppStore()
const authorProfileStore = useAuthorProfileStore()
const { t } = useI18n()

watch(
  () => appStore.locale,
  locale => {
    void authorProfileStore.load(locale)
  }
)

onMounted(() => {
  void authorProfileStore.load(appStore.locale)
})

const authorData = computed(() => ({
  avatar: authorProfileStore.profile.avatar,
  name: authorProfileStore.profile.name,
  description: authorProfileStore.profile.description,
  socials: authorProfileStore.profile.socials,
  post_list: { length: authorProfileStore.profile.articleCount },
  categories: authorProfileStore.profile.categoryCount,
  tags: authorProfileStore.profile.tagCount
}))

const avatarClass = computed(() => {
  return {
    'ob-avatar': true,
    [appStore.themeConfig.theme.profile_shape]: true
  }
})
const gradientBackground = computed(() => {
  return { background: appStore.themeConfig.theme.header_gradient_css }
})
</script>

<style lang="scss" scoped>
.profile {
  top: -7%;
  height: 100%;
  max-height: 100%;
}
</style>
