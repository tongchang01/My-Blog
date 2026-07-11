import type { ApiResponse } from "./contract";
import type {
  CurrentUser,
  LoginRequest,
  TokenPair
} from "@/features/auth/model";
import type { UserProfilePayload } from "@/features/profile/form";
import { http } from "@/utils/http";

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

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

export const logout = () => http.post<ApiResponse<null>>("/api/auth/logout");

export const getCurrentUser = () =>
  http.get<ApiResponse<CurrentUser>>("/api/auth/me");

export const updateCurrentUserProfile = (payload: UserProfilePayload) =>
  http.request<ApiResponse<CurrentUser["profile"]>>(
    "patch",
    "/api/auth/me/profile",
    { data: payload }
  );

export const changeCurrentUserPassword = (payload: ChangePasswordPayload) =>
  http.request<ApiResponse<null>>("put", "/api/auth/me/password", {
    data: payload
  });
