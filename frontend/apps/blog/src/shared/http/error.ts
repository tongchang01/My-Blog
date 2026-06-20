export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string,
    readonly cause?: unknown
  ) {
    super(message, { cause })
    this.name = 'ApiError'
  }
}
