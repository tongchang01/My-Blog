export type ApiErrorKind =
  | "badCredentials"
  | "sessionExpired"
  | "forbidden"
  | "validation"
  | "rateLimited"
  | "server"
  | "network"
  | "unknown";

const CODE_KIND: Readonly<Record<string, ApiErrorKind>> = {
  "10001": "badCredentials",
  "10002": "sessionExpired",
  "10003": "forbidden",
  "90001": "validation",
  "90002": "rateLimited",
  "99999": "server"
};

export class ApiClientError extends Error {
  constructor(
    public readonly kind: ApiErrorKind,
    public readonly code: string,
    public readonly status?: number
  ) {
    super(`API request failed: ${code}`);
    this.name = "ApiClientError";
  }
}

export function apiErrorFromCode(
  code: string,
  status?: number
): ApiClientError {
  return new ApiClientError(CODE_KIND[code] ?? "unknown", code, status);
}
