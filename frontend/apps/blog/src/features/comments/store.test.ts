import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createArticleComment,
  loadArticleComments
} from './api'
import { useCommentStore } from './store'

vi.mock('./api', () => ({
  loadArticleComments: vi.fn(),
  createArticleComment: vi.fn()
}))

const mockedLoad = vi.mocked(loadArticleComments)
const mockedCreate = vi.mocked(createArticleComment)

const page = (
  records: Awaited<ReturnType<typeof loadArticleComments>>['records']
) => ({
  records,
  total: records.length,
  page: 1,
  size: 20
})

describe('comment store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedLoad.mockReset()
    mockedCreate.mockReset()
  })

  it('loads article comments and distinguishes empty pages', async () => {
    mockedLoad
      .mockResolvedValueOnce(
        page([
          {
            id: '9007199254740993',
            parentId: null,
            replyToCommentId: null,
            replyToNickname: null,
            authorNickname: 'TYB',
            authorSite: null,
            contentHtml: '<p>Hello</p>',
            createdAt: '2026-07-06T10:00:00',
            replies: []
          }
        ])
      )
      .mockResolvedValueOnce(page([]))
    const store = useCommentStore()

    await store.load({ articleId: '9007199254740993', page: 1, size: 20 })
    expect(store.status).toBe('ready')
    expect(store.comments[0].id).toBe('9007199254740993')

    await store.load({ articleId: '9007199254740993', page: 2, size: 20 })
    expect(store.status).toBe('empty')
  })

  it('aborts previous comment requests', async () => {
    let firstSignal: AbortSignal | undefined
    mockedLoad
      .mockImplementationOnce(params => {
        firstSignal = params.signal
        return new Promise(() => undefined)
      })
      .mockResolvedValueOnce(page([]))
    const store = useCommentStore()

    void store.load({ articleId: '1', page: 1, size: 20 })
    await store.load({ articleId: '1', page: 2, size: 20 })

    expect(firstSignal?.aborted).toBe(true)
    expect(store.status).toBe('empty')
  })

  it('sets and clears reply target', () => {
    const store = useCommentStore()

    store.setReplyTarget({
      id: '9007199254740993',
      authorNickname: 'TYB'
    })
    expect(store.replyTarget?.id).toBe('9007199254740993')

    store.clearReplyTarget()
    expect(store.replyTarget).toBeNull()
  })

  it('submits comments with reply target and refreshes after pass', async () => {
    mockedLoad.mockResolvedValue(page([]))
    mockedCreate.mockResolvedValueOnce({
      id: '9007199254740994',
      auditStatus: 'PASS'
    })
    const store = useCommentStore()
    store.setReplyTarget({
      id: '9007199254740993',
      authorNickname: 'TYB'
    })

    await store.submit('9007199254740993', {
      nickname: 'Reader',
      email: 'reader@example.com',
      site: '',
      contentMd: 'reply'
    })

    expect(mockedCreate).toHaveBeenCalledWith('9007199254740993', {
      nickname: 'Reader',
      email: 'reader@example.com',
      site: null,
      contentMd: 'reply',
      replyToCommentId: '9007199254740993'
    })
    expect(store.notice).toBe('评论已发布')
    expect(store.replyTarget).toBeNull()
    expect(mockedLoad).toHaveBeenCalledWith({
      articleId: '9007199254740993',
      page: 1,
      size: 20,
      signal: expect.any(AbortSignal)
    })
  })

  it('keeps pending comments out of the list', async () => {
    mockedCreate.mockResolvedValueOnce({
      id: '9007199254740994',
      auditStatus: 'PENDING'
    })
    const store = useCommentStore()

    await store.submit('9007199254740993', {
      nickname: 'Reader',
      email: 'reader@example.com',
      site: 'https://example.com',
      contentMd: 'pending'
    })

    expect(store.notice).toBe('评论已提交，等待审核')
    expect(mockedLoad).not.toHaveBeenCalled()
  })
})
