<template>
  <div id="footer" class="relative w-full pt-1" :style="gradientBackground">
    <footer class="bg-ob-deep-800 flex justify-center">
      <div
        class="bg-ob-deep-800 rounded-lg max-w-10/12 lg:max-w-screen-2xl text-sm text-ob-normal flex w-full flex-col items-center gap-5 px-6 py-6 lg:flex-row lg:justify-between"
      >
        <div class="flex flex-col items-center gap-4 lg:items-start">
          <ul class="grid grid-cols-1 gap-2 sm:grid-cols-3 sm:gap-6">
            <li v-if="siteStats" class="flex min-w-[10rem] items-center justify-between gap-3">
              <span>
                <SvgIcon
                  icon-class="hot"
                  class="mr-1 text-lg inline-block"
                  stroke="currentColor"
                />
                {{ t('settings.page-views-value') }}
              </span>
              <span class="flex-1 text-right">
                {{ siteStats.totalPv }}
              </span>
            </li>
            <li v-if="siteStats" class="flex min-w-[10rem] items-center justify-between gap-3">
              <span>
                <SvgIcon
                  icon-class="friends"
                  class="mr-1 text-lg inline-block"
                  stroke="currentColor"
                />
                {{ t('settings.unique_visitor-value') }}
              </span>
              <span class="flex-1 text-right">
                {{ siteStats.todayUv }}
              </span>
            </li>
            <li v-if="runningDays" class="flex min-w-[10rem] items-center justify-between gap-3">
              <span>
                <SvgIcon
                  icon-class="date"
                  class="mr-1 text-lg inline-block"
                  stroke="currentColor"
                />
                {{ t('settings.site-running-for') }}
              </span>
              <span class="flex-1 text-right"
                >{{ runningDays }}
                {{ t('settings.site-running-for-unit') }}</span
              >
            </li>
          </ul>
          <ul
            v-if="
              themeConfig.site.beian.number !== '' ||
              themeConfig.site.police_beian.number !== ''
            "
            class="flex flex-wrap justify-center gap-3 text-xs lg:justify-start"
          >
            <li v-if="themeConfig.site.police_beian.number !== ''">
              <img class="inline-block" :src="beianImg" alt="" width="15" />
              公安备案信息：
              <a :href="themeConfig.site.police_beian.link">
                <b class="font-extrabold border-b-2 border-ob hover:text-ob">
                  {{ themeConfig.site.police_beian.number }}
                </b>
              </a>
            </li>
            <li v-if="themeConfig.site.beian.number !== ''">
              备案信息：
              <a :href="themeConfig.site.beian.link">
                <b class="font-extrabold border-b-2 border-ob hover:text-ob">
                  {{ themeConfig.site.beian.number }}
                </b>
              </a>
            </li>
          </ul>
        </div>
        <div class="hidden shrink-0 lg:flex">
          <img
            v-show="themeConfig.site.avatar"
            :class="avatarClass"
            :src="themeConfig.site.avatar"
            alt="avatar"
          />
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { useI18n } from 'vue-i18n'
import SvgIcon from '@/components/SvgIcon/index.vue'
import beianImg from '@/assets/gongan-beian-40-40.png'
import { getDaysTillNow } from '@/utils'
import {
  loadSiteStatsSummary,
  type SiteStatsSummaryDto
} from '@/features/stats/api'

const appStore = useAppStore()
const { t } = useI18n()
const siteStats = ref<SiteStatsSummaryDto | null>(null)

onMounted(async () => {
  try {
    siteStats.value = await loadSiteStatsSummary()
  } catch {
    siteStats.value = null
  }
})

const avatarClass = computed(() => {
  return {
    'footer-avatar': true,
    [appStore.themeConfig.theme.profile_shape]: true
  }
})
const gradientBackground = computed(() => {
  return { background: appStore.themeConfig.theme.header_gradient_css }
})
const themeConfig = computed(() => appStore.themeConfig)
const runningDays = computed(() => {
  if (appStore.themeConfig.site.started_date === '') return undefined
  return getDaysTillNow(`${appStore.themeConfig.site.started_date}`)
})
</script>

<style lang="scss" scoped></style>
