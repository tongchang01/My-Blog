export interface ApiResponse<T> {
  code: string
  msg: string
  data: T | null
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}
