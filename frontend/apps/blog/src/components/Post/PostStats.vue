<template>
  <div
    class="post-stats"
    v-if="postTimeCount !== undefined && postWordCount !== undefined"
  >
    <span>
      <SvgIcon
        class="opacity-70"
        icon-class="clock"
        fill="none"
        stroke="white"
        height="1.25em"
        width="1.25em"
      />
      <span class="pl-2 opacity-70">
        {{ postTimeCount }}
      </span>
    </span>
    <span>
      <SvgIcon
        class="opacity-70"
        icon-class="text"
        fill="none"
        stroke="white"
        height="1.25em"
        width="1.25em"
      />
      <span class="pl-2 opacity-70">
        {{ postWordCount }}
      </span>
    </span>
  </div>

  <div v-else class="post-stats">
    <span>
      <SvgIcon
        class="opacity-70"
        icon-class="clock"
        fill="none"
        stroke="white"
        height="1.25em"
        width="1.25em"
      />
      <span class="pl-2">
        <ob-skeleton width="40px" height="16px" />
      </span>
    </span>
    <span>
      <SvgIcon
        class="opacity-70"
        icon-class="text"
        fill="white"
        stroke="white"
        height="1.25em"
        width="1.25em"
      />
      <span class="pl-2">
        <ob-skeleton width="40px" height="16px" />
      </span>
    </span>
  </div>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import SvgIcon from '@/components/SvgIcon/index.vue'
import { PluginsData, ThemeConfig } from '@/models/ThemeConfig.class'

defineProps({
  postWordCount: {
    type: Number || undefined
  },
  postTimeCount: {
    type: String || undefined
  },
  postTitle: {
    type: String
  },
  pluginConfigs: {
    type: Object as PropType<PluginsData>,
    default: () => new ThemeConfig().plugins,
    required: true
  },
  currentPath: {
    type: String,
    default: '/',
    required: true
  },
  comments: Boolean
})
</script>
