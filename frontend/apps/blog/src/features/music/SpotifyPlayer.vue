<template>
  <details
    v-if="playlistId"
    ref="panel"
    class="spotify-player"
    @toggle="handleToggle"
  >
    <summary>{{ t('music.entry') }}</summary>
    <div class="spotify-player-content">
      <p>{{ t('music.playback-note') }}</p>
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
      <p>{{ t('music.unavailable-help') }}</p>
      <div class="spotify-player-actions">
        <a :href="playlistUrl" target="_blank" rel="noopener noreferrer">
          {{ t('music.open-spotify') }}
        </a>
        <button v-if="activated" type="button" @click="frameKey++">
          {{ t('music.reload') }}
        </button>
        <button type="button" @click="stop">
          {{ t('music.stop') }}
        </button>
      </div>
    </div>
  </details>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSiteSettingsStore } from '@/features/site-settings/store'

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
  if (panel.value) panel.value.open = false
}

watch(playlistId, stop)

const handleToggle = () => {
  // 折叠只隐藏面板，停止操作才销毁播放器；不接管 Spotify 的播放状态。
  if (panel.value?.open) activated.value = true
}
</script>

<style scoped>
.spotify-player {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  z-index: 50;
  max-width: calc(100vw - 2rem);
  max-height: calc(100dvh - 2rem);
  overflow: auto;
  border: 1px solid currentColor;
  border-radius: 0.75rem;
  background: var(--background-primary, white);
  color: var(--text-normal, #222);
}

.spotify-player[open] {
  width: 24rem;
}

summary {
  cursor: pointer;
  padding: 0.75rem 1rem;
}

.spotify-player-content {
  padding: 0 1rem 1rem;
}

iframe {
  display: block;
  border: 0;
  border-radius: 12px;
}

p {
  font-size: 0.875rem;
  margin: 0.5rem 0;
}

.spotify-player-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

button,
a {
  text-decoration: underline;
}
</style>
