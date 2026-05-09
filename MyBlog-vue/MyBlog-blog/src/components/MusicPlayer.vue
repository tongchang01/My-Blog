<template>
  <teleport to="body">
    <div
      v-if="shouldRender"
      class="music-player-host"
      :class="{ 'music-player-host-expanded': isExpanded }"
      @mouseenter="handlePlayerInteraction"
      @mousemove="handlePlayerInteraction"
      @mouseleave="scheduleCollapse"
      @focusin="handlePlayerInteraction"
      @focusout="scheduleCollapse"
      @pointerdown="handlePlayerInteraction">
      <div ref="playerRef" class="music-player-container" @click="handlePlayerInteraction"></div>
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
    const isExpanded = ref(true)
    let collapseTimer: number | null = null

    const websiteConfig = computed<Record<string, any>>(() => appStore.websiteConfig || {})
    const isEnabled = computed(() => getConfigFlag(websiteConfig.value, 'musicPlayerEnable', true))
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

    const clearCollapseTimer = () => {
      if (collapseTimer !== null) {
        clearTimeout(collapseTimer)
        collapseTimer = null
      }
    }

    const scheduleCollapse = () => {
      clearCollapseTimer()
      collapseTimer = <any>setTimeout(() => {
        isExpanded.value = false
      }, 1200)
    }

    const handlePlayerInteraction = () => {
      isExpanded.value = true
      scheduleCollapse()
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
        autoplay: false,
        theme: defaultTheme.value,
        loop: loop.value,
        order: order.value,
        preload: 'metadata',
        listFolded: true,
        listMaxHeight: '180px',
        audio: playlist.value
      })
      handlePlayerInteraction()
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
      clearCollapseTimer()
      destroyPlayer()
    })

    return {
      playerRef,
      shouldRender,
      isExpanded,
      handlePlayerInteraction,
      scheduleCollapse
    }
  }
})
</script>

<style lang="scss">
.music-player-host {
  position: fixed;
  right: 24px;
  top: 66vh;
  z-index: 1300;
  width: 360px;
  max-width: calc(100vw - 32px);
  transform: translateX(calc(100% - 54px));
  transition: transform 0.25s ease;
}

.music-player-host-expanded,
.music-player-host:focus-within {
  transform: translateX(0);
}

.music-player-container {
  width: 100%;
}

.music-player-host .aplayer {
  margin: 0;
  border-radius: 18px;
  overflow: hidden;
  background: rgba(35, 31, 34, 0.9);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.music-player-host .aplayer .aplayer-body {
  height: 78px;
  background: transparent;
}

.music-player-host .aplayer .aplayer-pic {
  width: 78px;
  height: 78px;
}

.music-player-host .aplayer .aplayer-info {
  height: 78px;
  margin-left: 78px;
  padding: 10px 12px 0 12px;
  border-bottom: none;
  background: transparent;
  box-sizing: border-box;
}

.music-player-host .aplayer .aplayer-music {
  height: 22px;
  line-height: 22px;
  margin-bottom: 6px;
}

.music-player-host .aplayer .aplayer-title {
  color: #f7f3ef;
  font-size: 14px;
  font-weight: 600;
}

.music-player-host .aplayer .aplayer-author {
  color: rgba(247, 243, 239, 0.65);
  font-size: 12px;
}

.music-player-host .aplayer .aplayer-controller {
  display: flex;
  align-items: center;
}

.music-player-host .aplayer .aplayer-bar-wrap {
  padding: 4px 0;
  margin: 0 8px 0 0;
}

.music-player-host .aplayer .aplayer-bar {
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
}

.music-player-host .aplayer .aplayer-loaded {
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.25);
}

.music-player-host .aplayer .aplayer-played {
  height: 4px;
  border-radius: 999px;
}

.music-player-host .aplayer .aplayer-thumb {
  width: 10px;
  height: 10px;
  margin-top: -3px;
}

.music-player-host .aplayer .aplayer-time {
  color: rgba(247, 243, 239, 0.72);
  font-size: 12px;
}

.music-player-host .aplayer .aplayer-icon path {
  fill: rgba(247, 243, 239, 0.72);
}

.music-player-host .aplayer .aplayer-icon:hover path {
  fill: #ffffff;
}

.music-player-host .aplayer .aplayer-list {
  background: rgba(35, 31, 34, 0.96);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.music-player-host .aplayer .aplayer-list ol li {
  color: rgba(247, 243, 239, 0.78);
  border-top-color: rgba(255, 255, 255, 0.06);
}

.music-player-host .aplayer .aplayer-list ol li:hover {
  background: rgba(255, 255, 255, 0.08);
}

.music-player-host .aplayer .aplayer-list ol li.aplayer-list-light {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

.music-player-host .aplayer .aplayer-list ol li .aplayer-list-index,
.music-player-host .aplayer .aplayer-list ol li .aplayer-list-author {
  color: rgba(247, 243, 239, 0.5);
}

@media (max-width: 768px) {
  .music-player-host {
    right: 12px;
    bottom: 84px;
    width: calc(100vw - 24px);
    transform: none;
  }
}
</style>
