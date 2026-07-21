<template>
  <div :class="wrapperClasses">
    <MainTitle
      :title="title"
      icon="quote"
      paddings="pb-2 pt-0"
      text-size="text-2xl md:text-3xl"
    />

    <div v-if="!effectiveEnabled" class="comment-state">
      {{ t('comments.disabled') }}
    </div>
    <template v-else>
      <form class="comment-form" @submit.prevent="handleSubmit">
        <div v-if="commentStore.replyTarget" class="comment-reply-target">
          <span>
            {{
              t('comments.replying-to', {
                name: commentStore.replyTarget.authorNickname
              })
            }}
          </span>
          <button type="button" @click="commentStore.clearReplyTarget()">
            {{ t('comments.cancel') }}
          </button>
        </div>

        <textarea
          v-model="form.contentMd"
          class="comment-editor"
          rows="5"
          required
          :placeholder="t('comments.content-placeholder')"
        />
        <div class="comment-meta-inputs">
          <input
            v-model="form.nickname"
            class="comment-input"
            required
            :placeholder="t('comments.nickname')"
          />
          <input
            v-model="form.email"
            class="comment-input"
            required
            type="email"
            :placeholder="t('comments.email')"
          />
          <input
            v-model="form.site"
            class="comment-input"
            type="url"
            :placeholder="t('comments.website')"
          />
        </div>
        <div class="comment-form-footer">
          <p v-if="commentStore.notice" class="comment-notice">
            {{ t(commentStore.notice) }}
          </p>
          <p v-else-if="commentStore.error" class="comment-error">
            {{ t('comments.error') }}
          </p>
          <button class="comment-submit" :disabled="submitting">
            {{ submitting ? t('comments.submitting') : t('comments.submit') }}
          </button>
        </div>
      </form>

      <div
        v-if="
          commentStore.status === 'loading' || commentStore.status === 'error'
        "
        class="comment-state"
      >
        <ob-skeleton tag="div" :count="4" height="16px" width="100%" />
      </div>
      <div v-else-if="commentStore.status === 'empty'" class="comment-state">
        {{ t('comments.empty') }}
      </div>
      <div v-else class="comment-list">
        <article
          v-for="comment in commentStore.comments"
          :key="comment.id"
          class="comment-card"
        >
          <div class="comment-card-main">
            <div class="comment-avatar">
              {{ avatarText(comment.authorNickname) }}
            </div>
            <div class="comment-body">
              <div class="comment-head">
                <a
                  v-if="comment.authorSite"
                  :href="comment.authorSite"
                  target="_blank"
                  rel="noreferrer"
                >
                  {{ comment.authorNickname }}
                </a>
                <span v-else>{{ comment.authorNickname }}</span>
                <time>{{ comment.createdAt }}</time>
              </div>
              <div class="comment-content" v-html="comment.contentHtml"></div>
              <button
                class="comment-reply-button"
                type="button"
                @click="replyTo(comment.id, comment.authorNickname)"
              >
                {{ t('comments.reply') }}
              </button>
            </div>
          </div>

          <div
            v-if="flattenReplies(comment).length > 0"
            class="comment-replies"
          >
            <article
              v-for="reply in flattenReplies(comment)"
              :key="reply.id"
              class="comment-card comment-card-reply"
            >
              <div class="comment-avatar">
                {{ avatarText(reply.authorNickname) }}
              </div>
              <div class="comment-body">
                <div class="comment-head">
                  <a
                    v-if="reply.authorSite"
                    :href="reply.authorSite"
                    target="_blank"
                    rel="noreferrer"
                  >
                    {{ reply.authorNickname }}
                  </a>
                  <span v-else>{{ reply.authorNickname }}</span>
                  <time>{{ reply.createdAt }}</time>
                </div>
                <p v-if="reply.replyToNickname" class="comment-reply-to">
                  {{
                    t('comments.reply-to', {
                      name: reply.replyToNickname
                    })
                  }}
                </p>
                <div class="comment-content" v-html="reply.contentHtml"></div>
                <button
                  class="comment-reply-button"
                  type="button"
                  @click="replyTo(reply.id, reply.authorNickname)"
                >
                  {{ t('comments.reply') }}
                </button>
              </div>
            </article>
          </div>
        </article>
      </div>

      <div v-if="commentStore.page.pages > 1" class="comment-pagination">
        <button
          type="button"
          :disabled="commentStore.page.page <= 1"
          @click="loadPage(commentStore.page.page - 1)"
        >
          {{ t('comments.previous') }}
        </button>
        <span
          >{{ commentStore.page.page }} / {{ commentStore.page.pages }}</span
        >
        <button
          type="button"
          :disabled="commentStore.page.page >= commentStore.page.pages"
          @click="loadPage(commentStore.page.page + 1)"
        >
          {{ t('comments.next') }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MainTitle } from '@/components/Title'
import { useAppStore } from '@/stores/app'
import { useCommentStore } from '@/features/comments/store'
import type {
  CommentFormState,
  CommentViewModel
} from '@/features/comments/model'

const props = withDefaults(
  defineProps<{
    articleId?: string
    enabled?: boolean
    guestbook?: boolean
    title?: string
    body?: string
    uid?: string
    articleAccessToken?: string | null
  }>(),
  {
    articleId: '',
    enabled: true,
    guestbook: false,
    title: 'titles.comment',
    articleAccessToken: null
  }
)

const appStore = useAppStore()
const commentStore = useCommentStore()
const { t } = useI18n()
const form = reactive<CommentFormState>({
  nickname: '',
  email: '',
  site: '',
  contentMd: ''
})

const submitting = computed(() => commentStore.status === 'submitting')
const effectiveEnabled = computed(
  () => props.enabled && (props.guestbook || props.articleId !== '')
)
const wrapperClasses = computed(() => ({
  'bg-ob-deep-800 p-4 mt-8 lg:px-14 lg:py-10 rounded-2xl shadow-xl mb-8 lg:mb-0': true,
  [`comment-${appStore.themeConfig.theme.profile_shape}`]: true
}))

const loadPage = (page: number): void => {
  void commentStore.load({
    articleId: props.articleId,
    page,
    size: commentStore.page.size,
    locale: appStore.locale,
    guestbook: props.guestbook,
    articleAccessToken: props.articleAccessToken
  })
}

const replyTo = (id: string, authorNickname: string): void => {
  commentStore.setReplyTarget({ id, authorNickname })
}

const handleSubmit = async (): Promise<void> => {
  if (!props.guestbook && !props.articleId) return
  await commentStore.submit(props.guestbook ? null : props.articleId, form)
  if (!commentStore.error) form.contentMd = ''
}

const avatarText = (nickname: string): string =>
  nickname.trim().slice(0, 1).toUpperCase() || '?'

const flattenReplies = (comment: CommentViewModel): CommentViewModel[] =>
  comment.replies.flatMap(reply => [reply, ...flattenReplies(reply)])

watch(
  () =>
    [
      props.articleId,
      props.enabled,
      props.articleAccessToken,
      appStore.locale
    ] as const,
  ([articleId, enabled, articleAccessToken, locale]) => {
    if (enabled && (props.guestbook || articleId)) {
      void commentStore.load({
        articleId,
        page: 1,
        size: commentStore.page.size,
        locale,
        guestbook: props.guestbook,
        articleAccessToken
      })
    }
  },
  { immediate: true }
)
</script>

<style lang="scss">
.comment-form {
  @apply mb-6 rounded-xl bg-ob-deep-900 p-4;
}

.comment-editor {
  @apply box-border w-full rounded-lg bg-ob-deep-800 p-3 text-ob-normal opacity-70 outline-none;
  transition: var(--trans-ease);
  resize: vertical;

  &:focus {
    @apply opacity-100;
  }
}

.comment-meta-inputs {
  @apply mt-3 grid gap-2 md:grid-cols-3;
}

.comment-input {
  @apply min-w-0 rounded-lg border-none bg-ob-deep-800 px-3 py-2 text-sm text-ob-normal opacity-70 outline-none;
  transition: var(--trans-ease);

  &:focus {
    @apply opacity-100;
  }
}

.comment-form-footer {
  @apply mt-3 flex flex-col gap-3 md:flex-row md:items-center md:justify-between;
}

.comment-submit,
.comment-pagination button {
  @apply rounded-lg px-4 py-2 text-sm text-white;
  background: var(--main-gradient);
  transition: var(--trans-ease);

  &:disabled {
    @apply cursor-not-allowed opacity-50;
  }

  &:not(:disabled):hover {
    opacity: 0.65;
  }
}

.comment-notice {
  color: var(--text-sub-accent);
}

.comment-error {
  @apply text-red-300;
}

.comment-reply-target {
  @apply mb-3 flex items-center justify-between rounded-lg bg-ob-deep-800 px-3 py-2 text-sm;
  color: var(--text-sub-accent);

  button {
    color: var(--text-accent);
  }
}

.comment-state {
  @apply flex items-center justify-center gap-3 rounded-lg bg-ob-deep-900 p-4 text-sm text-ob-dim;
  min-height: 6rem;
}

.comment-list {
  @apply flex flex-col gap-3;
}

.comment-card {
  @apply rounded-lg bg-ob-deep-900 p-4;
  transition: var(--trans-ease);

  &:hover {
    box-shadow: var(--accent-shadow);
  }
}

.comment-card-main,
.comment-card-reply {
  @apply flex gap-3;
}

.comment-replies {
  @apply mt-3 flex flex-col gap-2 pl-8;
}

.comment-avatar {
  @apply flex h-9 w-9 shrink-0 items-center justify-center bg-ob-deep-800 text-sm font-bold;
  color: var(--text-accent);
}

.comment-circle .comment-avatar,
.comment-circle-avatar .comment-avatar {
  @apply rounded-full;
}

.comment-rounded .comment-avatar,
.comment-rounded-avatar .comment-avatar {
  @apply rounded-2xl;
}

.comment-diamond .comment-avatar,
.comment-diamond-avatar .comment-avatar {
  clip-path: polygon(50% 3%, 91% 25%, 91% 75%, 50% 97%, 9% 75%, 9% 25%);
}

.comment-body {
  @apply min-w-0 flex-1;
}

.comment-head {
  @apply flex flex-wrap items-center gap-x-3 gap-y-1 text-sm font-bold;

  a,
  span {
    color: var(--text-sub-accent);
  }

  time {
    @apply text-xs font-normal text-ob-dim;
  }
}

.comment-content {
  @apply mt-2 text-sm leading-7 text-ob-normal;

  a {
    color: var(--text-sub-accent);
  }

  blockquote {
    border-left: 0.25rem solid var(--bg-accent-55);
  }
}

.comment-reply-to {
  @apply mt-2 text-xs text-ob-dim;
}

.comment-reply-button {
  @apply mt-2 text-xs;
  color: var(--text-accent);
  transition: var(--trans-ease);

  &:hover {
    opacity: 0.6;
  }
}

.comment-pagination {
  @apply mt-4 flex items-center justify-center gap-3 text-sm text-ob-normal;
}
</style>
