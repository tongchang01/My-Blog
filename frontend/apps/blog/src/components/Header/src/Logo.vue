<template>
  <div
    class="header-logo flex items-center self-stretch relative cursor-pointer hover:scale-110 transition-transform transform-gpu duration-500"
    @click="handleLogoClick"
  >
    <span class="flex mr-3">
      <img
        v-if="logoImage"
        :class="avatarClass"
        :src="logoImage"
        alt="site-logo"
      />
      <ob-skeleton v-else width="2rem" height="2rem" circle />
    </span>

    <div class="flex flex-col justify-center">
      <span
        class="text-invert flex text-xl leading-tight text-white font-extrabold"
        v-if="appStore.siteTitle"
      >
        {{ appStore.siteTitle }}
      </span>
      <span
        v-else
        class="text-invert flex text-xl leading-tight text-white font-extrabold"
      >
        LOADING
      </span>
      <span
        class="text-invert font-extrabold text-[0.45rem] leading-tight uppercase text-white"
      >
        {{ appStore.siteSubtitle || 'BLOG' }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { useAuthorProfileStore } from '@/features/author-profile/store'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const appStore = useAppStore()
const authorProfileStore = useAuthorProfileStore()
const router = useRouter()

const handleLogoClick = () => {
  router.push('/')
}

const avatarClass = computed(() => {
  return {
    'logo-image': true,
    [appStore.themeConfig.theme.profile_shape]: true
  }
})
const themeConfig = computed(() => appStore.themeConfig)
const logoImage = computed(
  () => themeConfig.value.site.logo || authorProfileStore.profile.avatar
)
</script>

<style lang="scss">
.logo-image {
  @apply w-8 h-8 scale-125;
  transition: 0.3s all ease;
}

.header-active {
  .logo-image {
    @apply scale-100;
  }
}
</style>
