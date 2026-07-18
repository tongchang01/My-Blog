import { beforeEach, describe, expect, it, vi } from 'vitest'
import { requestApi } from '@/shared/http/client'
import { ApiError } from '@/shared/http/error'
import {
  createArticleComment,
  createGuestbookComment,
  loadArticleComments,
  loadGuestbookComments
} from './api'

vi.mock('@/shared/http/client', () => ({
  requestApi: vi.fn()
}))

const mockedRequestApi = vi.mocked(requestApi)

describe('comments api', () => {
  beforeEach(() => {
    mockedRequestApi.mockReset()
  })

  it('loads article comments with pagination', async () => {
    mockedRequestApi.mockResolvedValueOnce({
      records: [],
      total: 0,
      page: 1,
      size: 20
    })

    await expect(
      loadArticleComments({
        articleId: '9007199254740993',
        page: 1,
        size: 20
      })
    ).resolves.toEqual({
      records: [],
      total: 0,
      page: 1,
      size: 20
    })
    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'GET',
      url: '/public/articles/9007199254740993/comments',
      params: { page: 1, size: 20 },
      signal: undefined,
      headers: undefined
    })
  })

  it('rejects empty comment page responses', async () => {
    mockedRequestApi.mockResolvedValueOnce(null)

    await expect(
      loadArticleComments({
        articleId: '1',
        page: 1,
        size: 20
      })
    ).rejects.toBeInstanceOf(ApiError)
  })

  it('submits article comments with string reply ids', async () => {
    mockedRequestApi.mockResolvedValueOnce({
      id: '9007199254740994',
      auditStatus: 'PENDING'
    })

    await expect(
      createArticleComment('9007199254740993', {
        nickname: 'TYB',
        email: 'tyb@example.com',
        site: null,
        contentMd: 'hello',
        replyToCommentId: '9007199254740994'
      })
    ).resolves.toEqual({
      id: '9007199254740994',
      auditStatus: 'PENDING'
    })
    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'POST',
      url: '/public/articles/9007199254740993/comments',
      data: {
        nickname: 'TYB',
        email: 'tyb@example.com',
        site: null,
        contentMd: 'hello',
        replyToCommentId: '9007199254740994'
      },
      headers: undefined
    })
  })

  it('sends the article access token for protected comments', async () => {
    mockedRequestApi
      .mockResolvedValueOnce({ records: [], total: 0, page: 1, size: 20 })
      .mockResolvedValueOnce({ id: '1', auditStatus: 'PENDING' })

    await loadArticleComments({
      articleId: '1',
      page: 1,
      size: 20,
      articleAccessToken: 'article-token'
    })
    await createArticleComment(
      '1',
      {
        nickname: 'TYB',
        email: 'tyb@example.com',
        site: null,
        contentMd: 'hello',
        replyToCommentId: null
      },
      'article-token'
    )

    expect(mockedRequestApi).toHaveBeenNthCalledWith(1, {
      method: 'GET',
      url: '/public/articles/1/comments',
      params: { page: 1, size: 20 },
      signal: undefined,
      headers: { 'X-Article-Access-Token': 'article-token' }
    })
    expect(mockedRequestApi).toHaveBeenNthCalledWith(2, {
      method: 'POST',
      url: '/public/articles/1/comments',
      data: {
        nickname: 'TYB',
        email: 'tyb@example.com',
        site: null,
        contentMd: 'hello',
        replyToCommentId: null
      },
      headers: { 'X-Article-Access-Token': 'article-token' }
    })
  })

  it('uses the guestbook endpoints without an article id', async () => {
    mockedRequestApi.mockResolvedValueOnce({
      records: [],
      total: 0,
      page: 1,
      size: 20
    })

    await loadGuestbookComments(1, 20)

    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'GET',
      url: '/public/guestbook/comments',
      params: { page: 1, size: 20 }
    })

    mockedRequestApi.mockResolvedValueOnce({ id: '10', auditStatus: 'PASS' })
    await createGuestbookComment({
      nickname: '访客',
      email: 'guest@example.com',
      site: null,
      contentMd: '你好',
      replyToCommentId: null
    })

    expect(mockedRequestApi).toHaveBeenCalledWith({
      method: 'POST',
      url: '/public/guestbook/comments',
      data: {
        nickname: '访客',
        email: 'guest@example.com',
        site: null,
        contentMd: '你好',
        replyToCommentId: null
      }
    })
  })
})
