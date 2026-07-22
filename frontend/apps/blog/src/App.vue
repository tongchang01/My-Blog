<template>
  <div id="App-Wrapper" :class="[appWrapperClass, theme]" :style="wrapperStyle">
    <HeaderMain />
    <div
      id="App-Container"
      class="app-container lg:max-w-screen-2xl px-3 lg:px-8"
      tabindex="-1"
      :style="cssVariables"
    >
      <div class="app-banner bg-ob-screen" />
      <div class="app-banner app-banner-image" :style="headerImage" />
      <div class="app-banner app-banner-screen" :style="headerBaseBackground" />
      <div class="app-banner app-banner-cover" />
      <div class="relative z-10">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide-y" mode="out-in">
            <component :is="Component" :key="pageKey" />
          </transition>
        </router-view>
      </div>
    </div>
    <div id="loading-bar-wrapper" :class="loadingBarClass"></div>
  </div>
  <FooterContainer :style="cssVariables" />
  <template v-if="isMobile">
    <MobileMenu />
  </template>
  <SearchModal />
  <VueEasyLightbox
    v-if="lightBoxVisible"
    :visible="lightBoxVisible"
    :imgs="lightBoxImages"
    :index="lightBoxIndex"
    :moveDisabled="true"
    :rotateDisabled="true"
    :scrollDisabled="false"
    @hide="onHideLightBox"
  ></VueEasyLightbox>
</template>

<script setup lang="ts">
import {
  StyleValue,
  computed,
  defineAsyncComponent,
  onBeforeMount,
  onMounted,
  onUnmounted,
  ref,
  watch,
  watchEffect
} from 'vue'
import { useAppStore } from '@/stores/app'
import { useCommonStore } from '@/stores/common'
import { useLightBoxStore } from '@/stores/lightbox'
import { useMetaStore } from '@/stores/meta'
import { useAuthorProfileStore } from '@/features/author-profile/store'
import { useRoute } from 'vue-router'
import HeaderMain from '@/components/Header/src/Header.vue'
import FooterContainer from '@/components/Footer/FooterContainer.vue'
import MobileMenu from '@/components/MobileMenu.vue'
import defaultCover from '@/assets/default-cover.jpg'

const SearchModal = defineAsyncComponent(
  () => import('@/components/SearchModal.vue')
)
const VueEasyLightbox = defineAsyncComponent(() => import('vue-easy-lightbox'))

const appStore = useAppStore()
const route = useRoute()
const lightBoxStore = useLightBoxStore()
const commonStore = useCommonStore()
const metaStore = useMetaStore()
const authorProfileStore = useAuthorProfileStore()
const MOBILE_WITH = 1024 // Using the mobile width by Bootstrap design.

const appWrapperClass = 'app-wrapper'
const loadingBarClass = ref({
  'nprogress-custom-parent': false
})

const copyLabelDefaults = {
  zh: { author: '作者', link: '原文链接' },
  ja: { author: '著者', link: '元リンク' },
  en: { author: 'Author', link: 'Source' }
} as const

/** Initializing App config and other setups */
const initialApp = async () => {
  initResizeEvent()
  await Promise.all([
    appStore.fetchConfig(),
    authorProfileStore.load(appStore.locale)
  ]).then(() => {
    metaStore.addScripts(...appStore.themeConfig.site_meta.cdn.prismjs)
    // Change the favicon dynamically.
    if (
      appStore.themeConfig.site_meta.favicon &&
      appStore.themeConfig.site_meta.favicon !== ''
    ) {
      const link = document.querySelector("link[rel~='icon']")
      if (link)
        link.setAttribute('href', appStore.themeConfig.site_meta.favicon)
    }

    if (appStore.themeConfig.plugins.copy_protection.enable)
      initialCopyrightScript()
  })
}

const copyAttribution = () => {
  const locale = appStore.locale
  const labels = copyLabelDefaults[locale]
  const configuredLabels = appStore.themeConfig.plugins.copy_protection
  const configLocale = locale === 'zh' ? 'cn' : 'en'
  const authorLabel = configuredLabels.author[configLocale] || labels.author
  const linkLabel = configuredLabels.link[configLocale] || labels.link

  return `\n\n---------------------------------\n${authorLabel}: ${authorProfileStore.profile.name}\n${linkLabel}: ${document.location.href}`
}

const copyEventHandler = (event: ClipboardEvent) => {
  if (document.getSelection() instanceof Selection) {
    if (document.getSelection()?.toString() !== '' && event.clipboardData) {
      event.clipboardData.setData(
        'text',
        document.getSelection() + copyAttribution()
      )
      event.preventDefault()
    }
  }
}

const onHideLightBox = () => lightBoxStore.hideLightBox()

/** Adding copy listner */
const initialCopyrightScript = () => {
  document.addEventListener('copy', copyEventHandler)
}

const isMobile = computed(() => {
  return commonStore.isMobile
})

const resizeHandler = () => {
  const rect = document.body.getBoundingClientRect()
  const mobileState = rect.width - 1 < MOBILE_WITH
  if (isMobile.value !== mobileState) commonStore.changeMobileState(mobileState)
}

const initResizeEvent = () => {
  resizeHandler()
  window.addEventListener('resize', resizeHandler)
}

onBeforeMount(initialApp)

onUnmounted(() => {
  document.removeEventListener('copy', copyEventHandler)
  window.removeEventListener('resize', resizeHandler)
})

const wrapperStyle = ref({ 'min-height': '100vh' })

onMounted(() => {
  let wrapperHeight = screen.height
  const footerEl = document.getElementById('footer')
  const footerHeight = footerEl?.getBoundingClientRect().height ?? 0
  wrapperHeight = wrapperHeight - footerHeight * 2
  wrapperStyle.value = {
    'min-height': wrapperHeight + 'px'
  }
})

/**
 * Watches the app loading status, adding the `nprogress-custom-parent`
 * class to the nprogress container when loading.
 */
watch(
  () => appStore.appLoading,
  newState => {
    loadingBarClass.value['nprogress-custom-parent'] = newState
  }
)

watch(
  () => appStore.locale,
  locale => {
    void authorProfileStore.load(locale)
  }
)

const title = computed(() => metaStore.getTitle)
watchEffect(() => {
  document.title = title.value
  document.documentElement.lang = appStore.locale
})
const pageKey = computed(() => `${route.fullPath}:${appStore.locale}`)
const theme = computed(() => appStore.theme)
const headerImage = computed(() => {
  return {
    backgroundImage: `url(${commonStore.headerImage}), url(${defaultCover})`,
    backgroundColor: '#0d0b12',
    opacity: commonStore.headerImage !== '' ? 0.2 : 0
  } as StyleValue
})
const headerBaseBackground = computed(() => {
  return {
    background: appStore.themeConfig.theme.header_gradient_css,
    opacity: commonStore.headerImage !== '' ? 0.8 : 0.99
  }
})
const cssVariables = computed(() => {
  if (appStore.theme === 'theme-dark') {
    return `
      --text-accent: ${appStore.themeConfig.theme.gradient.color_1};
      --text-sub-accent: ${appStore.themeConfig.theme.gradient.color_3};
      --main-gradient: ${appStore.themeConfig.theme.header_gradient_css};
    `
  }
  return `
    --text-accent: ${appStore.themeConfig.theme.gradient.color_3};
    --text-sub-accent: ${appStore.themeConfig.theme.gradient.color_2};
    --main-gradient: ${appStore.themeConfig.theme.header_gradient_css};
  `
})
const lightBoxVisible = computed(() => lightBoxStore.visible)
const lightBoxIndex = computed(() => lightBoxStore.index)
const lightBoxImages = computed(() => lightBoxStore.images)
</script>

<style lang="scss">
body {
  background: var(--background-primary-alt);
}

*:focus-visible {
  outline: 2px solid var(--text-accent);
  outline-offset: 3px;
}

#app {
  @apply relative min-w-full min-h-screen h-full;
  .app-wrapper {
    @apply bg-ob-deep-900 min-w-full h-full pb-12;
    transition-property: transform, border-radius;
    transition-duration: 350ms;
    transition-timing-function: ease;
    transform-origin: 0 42%;
    .app-container {
      color: var(--text-normal);
      margin: 0 auto;
    }
  }

  .header-wave {
    position: absolute;
    top: 100px;
    left: 0;
    z-index: 1;
  }
}

.app-banner {
  content: '';
  display: block;
  height: 600px;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1;
}

.app-banner-cover {
  pointer-events: none;
  position: absolute;
  top: 60px;
  z-index: 3;
  height: 540px;
  background: var(--banner-cover);
}

.theme-light {
  .app-banner-cover {
    top: 300px;
    height: 300px;
  }

  .app-banner-screen {
    @apply blur-0 rounded-none;
    width: 100%;
    height: 600px;
  }
}

.app-banner-image {
  /* @apply blur; */
  z-index: 1;
  background-size: cover;
  opacity: 1;
  transition: ease-in-out opacity 300ms;
  background-color: #1a1a1a;
}

.app-banner-screen {
  @apply blur-[72px] rounded;
  left: 50%;
  transform: translateX(-50%);
  transition: ease-in-out all 500ms;
  z-index: 2;
  opacity: 0.91;
  width: 85%;
  height: 400px;
}
</style>
