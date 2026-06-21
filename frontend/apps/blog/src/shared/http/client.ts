import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from './contract'
import { ApiError } from './error'

const SUCCESS_CODE = '00000'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 5000
})

export const unwrapApiResponse = <T>(response: ApiResponse<T>): T | null => {
  if (response.code !== SUCCESS_CODE) {
    throw new ApiError(response.msg, undefined, response.code)
  }
  return response.data
}

const isApiResponse = (value: unknown): value is ApiResponse<unknown> => {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Partial<ApiResponse<unknown>>
  return typeof candidate.code === 'string' && typeof candidate.msg === 'string'
}

export const normalizeApiError = (cause: unknown): ApiError => {
  if (cause instanceof ApiError) return cause
  if (axios.isAxiosError(cause)) {
    const payload = cause.response?.data
    return new ApiError(
      isApiResponse(payload) ? payload.msg : cause.message,
      cause.response?.status,
      isApiResponse(payload) ? payload.code : cause.code,
      cause
    )
  }
  return new ApiError(
    cause instanceof Error ? cause.message : 'Request failed',
    undefined,
    undefined,
    cause
  )
}

export const requestApi = async <T>(
  config: AxiosRequestConfig
): Promise<T | null> => {
  try {
    const response = await client.request<ApiResponse<T>>(config)
    return unwrapApiResponse(response.data)
  } catch (cause) {
    throw normalizeApiError(cause)
  }
}
