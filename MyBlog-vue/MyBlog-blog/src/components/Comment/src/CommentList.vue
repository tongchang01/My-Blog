<template>
  <transition-group name="fade">
    <CommentItem v-for="(comment, index) in comments" :key="comment.id" :comment="comment" :index="index">
    </CommentItem>
  </transition-group>
  <button
    class="load-more-button mt-7 w-32 text-white p-2 rounded-lg shadow-lg transition transform hover:scale-105 flex mx-auto"
    v-if="haveMore">
    <span class="text-center flex-grow" @click="loadMore">{{ t('settings.load_more') }}</span>
  </button>
</template>

<script lang="ts">
// @ts-nocheck
import { defineComponent, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import CommentItem from './CommentItem.vue'
import { useCommentStore } from '@/stores/comment'
import emitter from '@/utils/mitt'

export default defineComponent({
  components: {
    CommentItem
  },
  setup() {
    const { t } = useI18n()
    const commentStore = useCommentStore()
    const loadMore = async () => {
      switch (commentStore.type) {
        case 1:
          emitter.emit('articleLoadMore')
          break
        case 2:
          emitter.emit('messageLoadMore')
          break
        case 3:
          emitter.emit('aboutLoadMore')
          break
        case 4:
          emitter.emit('friendLinkLoadMore')
          break
        case 5:
          emitter.emit('talkLoadMore')
      }
    }
    return {
      comments: inject('comments'),
      haveMore: inject('haveMore'),
      loadMore,
      t
    }
  }
})
</script>
<style lang="scss" scoped>
.load-more-button {
  outline: none;
  background: var(--main-gradient);
}
</style>
