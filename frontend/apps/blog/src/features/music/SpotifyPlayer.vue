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
      <div class="spotify-player-actions">
        <a
          :href="playlistUrl"
          target="_blank"
          rel="noopener noreferrer"
          :title="t('music.open-spotify')"
          :aria-label="t('music.open-spotify')"
          >Spotify</a
        >
        <button
          v-if="activated"
          type="button"
          :title="t('music.reload')"
          :aria-label="t('music.reload')"
          @click="frameKey++"
        >
          <SvgIcon
            icon-class="reload"
            fill="currentColor"
            stroke="none"
            width="0.875rem"
            height="0.875rem"
          />
        </button>
        <button
          type="button"
          :title="t('music.stop')"
          :aria-label="t('music.stop')"
          @click="handleStop"
        >
          <SvgIcon
            icon-class="close"
            fill="currentColor"
            stroke="none"
            width="0.75rem"
            height="0.75rem"
          />
        </button>
      </div>
      <iframe
        v-if="activated"
        :key="frameKey"
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
const frameKey = ref(0)
const playlistUrl = computed(
  () =>
    `https://open.spotify.com/playlist/${encodeURIComponent(playlistId.value ?? '')}`
)
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

const stop = () => {
  activated.value = false
  collapse()
}

watch(playlistId, stop)

const collapse = (restoreFocus = false) => {
  if (!panel.value) return
  panel.value.open = false
  if (restoreFocus) panel.value.querySelector('summary')?.focus()
}

const handleStop = () => {
  stop()
  panel.value?.querySelector('summary')?.focus()
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
  // 折叠只隐藏面板，停止操作才销毁播放器；不接管 Spotify 的播放状态。
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

summary:focus-visible,
button:focus-visible,
a:focus-visible {
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
  background: var(--background-primary);
  color: var(--text-normal);
  box-shadow: 0 12px 40px rgb(0 0 0 / 20%);
}

iframe {
  display: block;
  border: 0;
  border-radius: 12px;
}

.spotify-player-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
}

.spotify-player-actions a {
  margin-right: auto;
  padding: 0.25rem 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.spotify-player-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: 0;
  border-radius: 0.375rem;
  background: transparent;
  cursor: pointer;
}

button,
a {
  color: inherit;
}

.spotify-player-actions button:hover {
  background: var(--background-trans);
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
