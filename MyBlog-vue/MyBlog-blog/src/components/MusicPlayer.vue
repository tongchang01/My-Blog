<template>
  <teleport to="body">
    <div
      v-if="shouldRender"
      class="music-player-host"
      :class="{
        'music-player-host-fixed': isFixed,
        'music-player-host-docked': !isFixed
      }">
      <div ref="playerRef" class="music-player-container"></div>
    </div>
  </teleport>
</template>

<script lang="ts">
import { computed, defineComponent, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import APlayer from 'aplayer'
import 'aplayer/dist/APlayer.min.css'
import api from '@/api/api'
import { useAppStore } from '@/stores/app'

interface RawMusicItem {
  id?: number | string
  musicName?: string
  name?: string
  title?: string
  artist?: string
  author?: string
  album?: string
  cover?: string
  pic?: string
  url?: string
  src?: string
  theme?: string
  sort?: number
}

interface PlayerAudio {
  name: string
  artist: string
  url: string
  cover?: string
  theme?: string
}

type PlayerInstance = InstanceType<typeof APlayer>

const getConfigFlag = (config: Record<string, any>, key: string, defaultValue: boolean) => {
  if (!(key in config)) {
    return defaultValue
  }
  return Number(config[key]) === 1
}

export default defineComponent({
  name: 'MusicPlayer',
  setup() {
    const appStore = useAppStore()
    const playerRef = ref<HTMLElement | null>(null)
    const player = ref<PlayerInstance | null>(null)
    const playlist = ref<PlayerAudio[]>([])

    const websiteConfig = computed<Record<string, any>>(() => appStore.websiteConfig || {})
    const isEnabled = computed(() => getConfigFlag(websiteConfig.value, 'musicPlayerEnable', true))
    const isFixed = computed(() => getConfigFlag(websiteConfig.value, 'musicPlayerFixed', true))
    const shouldAutoplay = computed(() => getConfigFlag(websiteConfig.value, 'musicPlayerAutoPlay', false))
    const shouldRender = computed(() => isEnabled.value && playlist.value.length > 0)
    const order = computed(() => (websiteConfig.value.musicPlayerOrder === 'random' ? 'random' : 'list'))
    const loop = computed(() => {
      const currentLoop = websiteConfig.value.musicPlayerLoop
      return currentLoop === 'one' || currentLoop === 'none' ? currentLoop : 'all'
    })
    const defaultTheme = computed(() => websiteConfig.value.musicPlayerTheme || '#409eff')

    const normalizeTrack = (item: RawMusicItem): PlayerAudio | null => {
      const url = item.url || item.src
      if (!url) {
        return null
      }
      return {
        name: item.musicName || item.name || item.title || 'Untitled',
        artist: item.artist || item.author || 'Unknown Artist',
        url,
        cover: item.cover || item.pic,
        theme: item.theme || defaultTheme.value
      }
    }

    const destroyPlayer = () => {
      if (!player.value) {
        return
      }
      player.value.destroy()
      player.value = null
    }

    const createPlayer = async () => {
      destroyPlayer()
      if (!playerRef.value || !shouldRender.value) {
        return
      }
      await nextTick()
      player.value = new APlayer({
        container: playerRef.value,
        // Keep APlayer in normal mode and let our wrapper control positioning.
        fixed: false,
        autoplay: shouldAutoplay.value,
        theme: defaultTheme.value,
        loop: loop.value,
        order: order.value,
        preload: 'metadata',
        listFolded: false,
        listMaxHeight: '156px',
        audio: playlist.value
      })
    }

    const fetchMusics = async () => {
      const { data } = await api.getMusics()
      const rawList = Array.isArray(data?.data)
        ? data.data
        : Array.isArray(data)
          ? data
          : Array.isArray(data?.records)
            ? data.records
            : []
      playlist.value = rawList
        .map((item: RawMusicItem) => normalizeTrack(item))
        .filter((item: PlayerAudio | null): item is PlayerAudio => item !== null)
    }

    watch(
      () => [
        shouldRender.value,
        isFixed.value,
        shouldAutoplay.value,
        order.value,
        loop.value,
        defaultTheme.value,
        playlist.value.map((item) => `${item.name}-${item.artist}-${item.url}`).join('|')
      ],
      async () => {
        await nextTick()
        await createPlayer()
      },
      {
        flush: 'post'
      }
    )

    onMounted(async () => {
      try {
        await fetchMusics()
        await createPlayer()
      } catch (error) {
        console.error('Failed to initialize music player:', error)
      }
    })

    onBeforeUnmount(() => {
      destroyPlayer()
    })

    return {
      playerRef,
      shouldRender,
      isFixed
    }
  }
})
</script>

<style lang="scss">
.music-player-host {
  z-index: 1300;
  width: 560px;
  max-width: calc(100vw - 24px);
}

.music-player-host-fixed,
.music-player-host-docked {
  position: fixed;
  right: 20px;
}

.music-player-host-fixed {
  bottom: 20px;
}

.music-player-host-docked {
  bottom: 96px;
  transform: translateX(calc(100% - 56px));
  transform-origin: right center;
  transition: transform 0.28s ease, right 0.28s ease, bottom 0.28s ease;
}

.music-player-host-docked:hover,
.music-player-host-docked:focus-within {
  transform: translateX(0);
}

.music-player-container {
  width: 100%;
}

.music-player-host .aplayer {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 26px;
  overflow: hidden;
  background: rgba(52, 45, 48, 0.88);
  box-shadow: 0 24px 64px rgba(9, 10, 16, 0.34);
  backdrop-filter: blur(22px);
}

.music-player-host .aplayer .aplayer-body {
  display: grid;
  grid-template-columns: 124px minmax(0, 1fr);
  align-items: stretch;
  background: linear-gradient(180deg, rgba(72, 63, 67, 0.92), rgba(62, 54, 58, 0.88));
}

.music-player-host .aplayer .aplayer-pic {
  width: 124px;
  height: 124px;
  margin: 18px 0 18px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.music-player-host .aplayer .aplayer-pic .aplayer-button {
  transform: scale(1.1);
}

.music-player-host .aplayer .aplayer-pic img {
  object-fit: cover;
}

.music-player-host .aplayer .aplayer-info {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 12px;
  min-width: 0;
  padding: 18px 18px 16px 16px;
  background: transparent;
  border-bottom: none;
}

.music-player-host .aplayer .aplayer-info .aplayer-music {
  margin: 0;
  min-width: 0;
}

.music-player-host .aplayer .aplayer-info .aplayer-music .aplayer-title {
  color: #f8f5f2;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.25;
}

.music-player-host .aplayer .aplayer-info .aplayer-music .aplayer-author {
  color: rgba(244, 238, 233, 0.8);
  font-size: 13px;
  margin-left: 8px;
}

.music-player-host .aplayer .aplayer-info .aplayer-controller {
  margin-top: auto;
}

.music-player-host .aplayer .aplayer-bar-wrap {
  margin: 0 0 8px;
  padding: 4px 0 6px;
}

.music-player-host .aplayer .aplayer-bar {
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.music-player-host .aplayer .aplayer-loaded {
  height: 4px;
  border-radius: inherit;
  background: rgba(255, 255, 255, 0.08);
}

.music-player-host .aplayer .aplayer-played {
  height: 4px;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--text-accent, #60a5fa), rgba(255, 255, 255, 0.9));
}

.music-player-host .aplayer .aplayer-thumb {
  width: 10px;
  height: 10px;
  margin-top: -3px;
  background: #f8f5f2;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.14);
}

.music-player-host .aplayer .aplayer-time {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(244, 238, 233, 0.8);
}

.music-player-host .aplayer .aplayer-time .aplayer-icon path,
.music-player-host .aplayer .aplayer-icon path {
  fill: rgba(244, 238, 233, 0.78);
}

.music-player-host .aplayer .aplayer-time .aplayer-icon:hover path,
.music-player-host .aplayer .aplayer-icon:hover path {
  fill: #ffffff;
}

.music-player-host .aplayer .aplayer-time-inner {
  font-size: 12px;
}

.music-player-host .aplayer .aplayer-list {
  margin: 0 18px 18px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: rgba(24, 19, 22, 0.18);
  overflow: hidden;
}

.music-player-host .aplayer .aplayer-list {
  max-height: 156px !important;
}

.music-player-host .aplayer .aplayer-list ol li {
  height: 36px;
  line-height: 36px;
  padding: 0 12px;
  border-top-color: rgba(255, 255, 255, 0.06);
  color: rgba(248, 245, 242, 0.82);
  font-size: 13px;
}

.music-player-host .aplayer .aplayer-list ol li:hover {
  background: rgba(255, 255, 255, 0.08);
}

.music-player-host .aplayer .aplayer-list ol li.aplayer-list-light {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.music-player-host .aplayer .aplayer-notice {
  left: 18px;
  bottom: 18px;
  width: 96px;
  padding: 6px 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(26, 22, 24, 0.9);
  color: #ffffff;
  font-size: 12px;
  line-height: 1.3;
  box-shadow: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 996px) {
  .music-player-host {
    width: calc(100vw - 24px);
  }

  .music-player-host-fixed,
  .music-player-host-docked {
    right: 12px;
  }

  .music-player-host-fixed {
    bottom: 12px;
  }

  .music-player-host-docked {
    bottom: 84px;
    transform: none;
  }

  .music-player-host .aplayer .aplayer-pic {
    width: 92px;
    height: 92px;
    margin: 14px 0 14px 14px;
  }

  .music-player-host .aplayer .aplayer-info {
    padding: 14px 14px 14px 12px;
  }

  .music-player-host .aplayer .aplayer-body {
    grid-template-columns: 106px minmax(0, 1fr);
  }

  .music-player-host .aplayer .aplayer-list {
    margin: 0 14px 14px 12px;
  }
}
</style>
