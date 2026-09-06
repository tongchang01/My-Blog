<template>
  <details
    v-if="playlistId"
    ref="panel"
    class="spotify-player"
    @toggle="handleToggle"
    @keydown.esc.stop.prevent="collapse(true)"
  >
    <summary
      class="text-invert"
      :aria-label="t('music.entry')"
      :title="t('music.entry')"
    >
      <SvgIcon
        icon-class="music"
        fill="none"
        stroke="currentColor"
        width="1.2rem"
        height="1.2rem"
      />
    </summary>
    <div class="spotify-player-content">
      <iframe
        v-if="activated"
        :src="embedUrl"
        :title="t('music.player-title')"
        width="100%"
        height="352"
        frameborder="0"
        allowfullscreen
        allow="
          autoplay;
          clipboard-write;
          encrypted-media;
          fullscreen;
          picture-in-picture;
        "
        loading="lazy"
      />
    </div>
  </details>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import SvgIcon from '@/components/SvgIcon/index.vue'

const { t } = useI18n()
const settings = useSiteSettingsStore()
const panel = ref<HTMLDetailsElement>()
const playlistId = ref<string | null>(null)
const activated = ref(false)
const embedUrl = computed(
  () =>
    `https://open.spotify.com/embed/playlist/${encodeURIComponent(playlistId.value ?? '')}?utm_source=generator`
)

// 切换语言期间保留已确认的配置；仅成功响应可以替换或清空歌单。
watch(
  () => [settings.status, settings.settings.spotifyPlaylistId] as const,
  ([status, id]) => {
    if (status === 'ready') playlistId.value = id?.trim() || null
  },
  { immediate: true }
)

watch(playlistId, () => {
  activated.value = false
  collapse()
})

const collapse = (restoreFocus = false) => {
  if (!panel.value) return
  panel.value.open = false
  if (restoreFocus) panel.value.querySelector('summary')?.focus()
}

const handleOutsidePointer = (event: PointerEvent) => {
  if (
    panel.value?.open &&
    event.target instanceof Node &&
    !panel.value.contains(event.target)
  )
    collapse()
}

onMounted(() => document.addEventListener('pointerdown', handleOutsidePointer))
onUnmounted(() =>
  document.removeEventListener('pointerdown', handleOutsidePointer)
)

const handleToggle = () => {
  // 折叠只隐藏面板；暂停等播放操作由 Spotify 自身提供。
  if (panel.value?.open) activated.value = true
}
</script>

<style scoped>
.spotify-player {
  position: relative;
  margin-right: 0.5rem;
}

summary {
  cursor: pointer;
  list-style: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
}

summary::-webkit-details-marker {
  display: none;
}

.spotify-player[open] > summary,
summary:hover {
  background: var(--background-trans);
}

summary:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 3px;
}

.spotify-player-content {
  position: absolute;
  right: 0;
  top: calc(100% + 0.75rem);
  width: 24rem;
  max-width: calc(100vw - 2rem);
  max-height: calc(100dvh - 6rem);
  overflow: auto;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgb(0 0 0 / 20%);
}

iframe {
  display: block;
  border: 0;
  border-radius: 12px;
}

@media (max-width: 639px) {
  .spotify-player-content {
    position: fixed;
    top: auto;
    right: 1rem;
    bottom: max(1rem, env(safe-area-inset-bottom));
    width: calc(100vw - 2rem);
  }
}
</style>
