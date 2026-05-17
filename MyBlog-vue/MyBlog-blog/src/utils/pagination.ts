type PaginatedResponse<T> = {
  data?: {
    records?: T[] | null
    count?: number | null
  } | null
}

type ListResponse<T> = {
  data?: T[] | null
}

export function normalizePageRecords<T>(payload: PaginatedResponse<T>) {
  const page = payload?.data
  const pageRecords = page?.records
  const records = Array.isArray(pageRecords) ? pageRecords : []
  const count = typeof page?.count === 'number' ? page.count : records.length

  return {
    records,
    count
  }
}

export function normalizeListData<T>(payload: ListResponse<T>) {
  return Array.isArray(payload?.data) ? payload.data : []
}
