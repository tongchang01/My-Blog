import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { describe, expect, it } from 'vitest'
import { unwrapApiResponse, normalizeApiError } from './client'
import { ApiError } from './error'

describe('HTTP contract', () => {
  it('unwraps a successful response', () => {
    expect(unwrapApiResponse({ code: '00000', msg: 'success', data: 1 })).toBe(
      1
    )
  })

  it('throws the backend code and message for a failed response', () => {
    expect(() =>
      unwrapApiResponse({ code: '20001', msg: '不存在', data: null })
    ).toThrowError(
      expect.objectContaining({
        name: 'ApiError',
        code: '20001',
        message: '不存在'
      })
    )
  })

  it('preserves HTTP and backend error details from Axios', () => {
    const response = {
      status: 403,
      data: { code: '10003', msg: '禁止访问', data: null }
    } as AxiosResponse
    const cause = new AxiosError(
      'Request failed',
      'ERR_BAD_RESPONSE',
      { headers: new AxiosHeaders() },
      {},
      response
    )

    expect(normalizeApiError(cause)).toEqual(
      expect.objectContaining<ApiError>({
        name: 'ApiError',
        status: 403,
        code: '10003',
        message: '禁止访问'
      })
    )
  })
})
