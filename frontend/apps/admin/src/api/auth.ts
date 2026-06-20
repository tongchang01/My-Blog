import type { ApiResponse } from "./contract";
import type {
  CurrentUser,
  LoginRequest,
  TokenPair
} from "@/features/auth/model";
import { http } from "@/utils/http";

export const login = (request: LoginRequest) =>
  http.post<ApiResponse<TokenPair>>(
    "/api/auth/login",
    { data: request },
    {
      skipAuthRefresh: true
    }
  );

export const refreshSession = (refreshToken: string) =>
  http.post<ApiResponse<TokenPair>>(
    "/api/auth/refresh",
    { data: { refreshToken } },
    { skipAuthRefresh: true }
  );

export const logout = (refreshToken: string) =>
  http.post<ApiResponse<null>>("/api/auth/logout", {
    data: { refreshToken }
  });

export const getCurrentUser = () =>
  http.get<ApiResponse<CurrentUser>>("/api/auth/me");
