import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { normalizeApiError } from '@/shared/http/client'
import type { ApiError } from '@/shared/http/error'
import type { SupportedLocale } from '@/shared/i18n/locale'
import {
  createArticleComment,
  createGuestbookComment,
  loadArticleComments,
  loadGuestbookComments,
  type LoadArticleCommentsParams
} from './api'
import { mapCommentPage } from './mapper'
import type {
  CommentFormState,
  CommentListStatus,
  CommentPageViewModel
} from './model'

interface ReplyTarget {
  id: string
  authorNickname: string
}

interface CommentQuery extends Omit<LoadArticleCommentsParams, 'signal'> {
  locale: SupportedLocale
  guestbook?: boolean
}

const DEFAULT_SIZE = 20

const emptyPage = (): CommentPageViewModel => ({
  records: [],
  total: 0,
  page: 1,
  size: DEFAULT_SIZE,
  pages: 0
})

const toNullableString = (value: string): string | null => {
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : null
}

export const useCommentStore = defineStore('public-comments', () => {
  const page = ref<CommentPageViewModel>(emptyPage())
  const status = ref<CommentListStatus>('idle')
  const error = ref<ApiError | null>(null)
  const replyTarget = ref<ReplyTarget | null>(null)
  const notice = ref<string | null>(null)
  let activeRequest: AbortController | null = null
  let lastQuery: CommentQuery | null = null

  const comments = computed(() => page.value.records)

  const load = async (query: CommentQuery): Promise<void> => {
    activeRequest?.abort()
    const request = new AbortController()
    activeRequest = request
    lastQuery = query
    status.value = 'loading'
    error.value = null

    try {
      page.value = mapCommentPage(
        await (query.guestbook
          ? loadGuestbookComments(query.page, query.size)
          : loadArticleComments({
              articleId: query.articleId,
              page: query.page,
              size: query.size,
              signal: request.signal
            })),
        query.locale
      )
      status.value = page.value.records.length === 0 ? 'empty' : 'ready'
    } catch (cause) {
      if (request.signal.aborted) return
      error.value = normalizeApiError(cause)
      status.value = 'error'
    } finally {
      if (activeRequest === request) activeRequest = null
    }
  }

  const retry = async (): Promise<void> => {
    if (lastQuery) await load(lastQuery)
  }

  const setReplyTarget = (target: ReplyTarget): void => {
    replyTarget.value = target
    notice.value = null
  }

  const clearReplyTarget = (): void => {
    replyTarget.value = null
  }

  const submit = async (
    articleId: string | null,
    form: CommentFormState
  ): Promise<void> => {
    status.value = 'submitting'
    error.value = null
    notice.value = null

    try {
      const payload = {
        nickname: form.nickname.trim(),
        email: form.email.trim(),
        site: toNullableString(form.site),
        contentMd: form.contentMd.trim(),
        replyToCommentId: replyTarget.value?.id ?? null
      }
      const result = lastQuery?.guestbook
        ? await createGuestbookComment(payload)
        : await createArticleComment(articleId ?? '', payload)
      replyTarget.value = null

      if (result.auditStatus === 'PASS') {
        notice.value = '评论已发布'
        await load({
          articleId: articleId ?? '',
          page: 1,
          size: lastQuery?.size ?? page.value.size,
          locale: lastQuery?.locale ?? 'zh',
          guestbook: lastQuery?.guestbook
        })
      } else {
        notice.value = '评论已提交，等待审核'
        status.value = comments.value.length === 0 ? 'empty' : 'ready'
      }
    } catch (cause) {
      error.value = normalizeApiError(cause)
      status.value = 'error'
    }
  }

  return {
    page,
    comments,
    status,
    error,
    replyTarget,
    notice,
    load,
    retry,
    setReplyTarget,
    clearReplyTarget,
    submit
  }
})
