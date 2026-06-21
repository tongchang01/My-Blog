import Axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type CustomParamsSerializer
} from "axios";
import { stringify } from "qs";
import type { ApiResponse } from "@/api/contract";
import { apiErrorFromCode } from "./error";
import type { RequestMethods } from "./types.d";

export interface AuthRefreshCoordinator {
  getAccessToken(): string | null;
  refresh(): Promise<string>;
  expire(): void;
}

export interface AuthRequestConfig extends AxiosRequestConfig {
  skipAuthRefresh?: boolean;
  retriedAfterRefresh?: boolean;
}

interface HttpClientOptions {
  baseURL?: string;
  timeout?: number;
  coordinator?: AuthRefreshCoordinator;
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    "code" in value &&
    typeof value.code === "string"
  );
}

function responseCode(response?: AxiosResponse): string | undefined {
  return isApiResponse(response?.data) ? response.data.code : undefined;
}

export class HttpClient {
  public readonly instance: AxiosInstance;
  private coordinator?: AuthRefreshCoordinator;
  private refreshPromise: Promise<string> | null = null;

  constructor(options: HttpClientOptions = {}) {
    this.coordinator = options.coordinator;
    this.instance = Axios.create({
      baseURL: options.baseURL,
      timeout: options.timeout ?? 10000,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest"
      },
      paramsSerializer: {
        serialize: stringify as unknown as CustomParamsSerializer
      }
    });
    this.installInterceptors();
  }

  public setAuthRefreshCoordinator(coordinator: AuthRefreshCoordinator): void {
    this.coordinator = coordinator;
  }

  private installInterceptors(): void {
    this.instance.interceptors.request.use(config => {
      const requestConfig = config as AuthRequestConfig;
      const token = this.coordinator?.getAccessToken();
      if (
        !requestConfig.skipAuthRefresh &&
        !requestConfig.retriedAfterRefresh &&
        token
      ) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    this.instance.interceptors.response.use(
      response => {
        if (isApiResponse(response.data) && response.data.code !== "00000") {
          throw apiErrorFromCode(response.data.code, response.status);
        }
        return response.data;
      },
      async (error: AxiosError) => {
        const config = error.config as AuthRequestConfig | undefined;
        const code = responseCode(error.response);
        const shouldRefresh =
          error.response?.status === 401 &&
          code === "10002" &&
          config !== undefined &&
          !config.skipAuthRefresh &&
          !config.retriedAfterRefresh &&
          this.coordinator !== undefined;

        if (!shouldRefresh) {
          return Promise.reject(
            code ? apiErrorFromCode(code, error.response?.status) : error
          );
        }

        const token = await this.refreshAccessToken();
        config.retriedAfterRefresh = true;
        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${token}`;
        return this.instance.request(config);
      }
    );
  }

  private refreshAccessToken(): Promise<string> {
    if (!this.refreshPromise) {
      const coordinator = this.coordinator;
      if (!coordinator) {
        return Promise.reject(new Error("Auth refresh coordinator is missing"));
      }
      this.refreshPromise = coordinator
        .refresh()
        .catch(error => {
          coordinator.expire();
          throw error;
        })
        .finally(() => {
          this.refreshPromise = null;
        });
    }
    return this.refreshPromise;
  }

  public request<T>(
    method: RequestMethods,
    url: string,
    param?: AxiosRequestConfig,
    axiosConfig?: AuthRequestConfig
  ): Promise<T> {
    return this.instance.request({ method, url, ...param, ...axiosConfig });
  }

  public post<T, P = unknown>(
    url: string,
    params?: AxiosRequestConfig<P>,
    config?: AuthRequestConfig
  ): Promise<T> {
    return this.request<T>("post", url, params, config);
  }

  public get<T, P = unknown>(
    url: string,
    params?: AxiosRequestConfig<P>,
    config?: AuthRequestConfig
  ): Promise<T> {
    return this.request<T>("get", url, params, config);
  }
}

export const createHttpClient = (options: HttpClientOptions = {}) =>
  new HttpClient(options);

export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL
});
