import type { ApiResponse } from "@/api/contract";
import { getCurrentUser, login, logout, refreshSession } from "@/api/auth";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import type {
  CurrentUser,
  LoginRequest,
  StoredSession,
  TokenPair
} from "./model";
import { clearSession, loadSession, saveSession } from "./session-storage";
import { clearArticleDrafts } from "@/features/articles/editor/draftStorage";

export interface SessionUserStore {
  currentUser: CurrentUser | null;
  initialized: boolean;
  SET_CURRENT_USER(user: CurrentUser): void;
  SET_INITIALIZED(initialized: boolean): void;
  CLEAR_USER(): void;
}

interface AuthApi {
  login(request: LoginRequest): Promise<ApiResponse<TokenPair>>;
  refreshSession(refreshToken: string): Promise<ApiResponse<TokenPair>>;
  logout(): Promise<unknown>;
  getCurrentUser(): Promise<ApiResponse<CurrentUser>>;
}

interface SessionServiceDependencies {
  userStore: SessionUserStore;
  api: AuthApi;
  now?: () => number;
}

function toStoredSession(tokens: TokenPair, now: number): StoredSession {
  return {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    accessExpiresAt: now + tokens.accessExpiresIn * 1000,
    refreshExpiresAt: now + tokens.refreshExpiresIn * 1000
  };
}

export function createSessionService({
  userStore,
  api,
  now = Date.now
}: SessionServiceDependencies) {
  const expire = () => {
    clearArticleDrafts(userStore.currentUser?.id);
    clearSession();
    userStore.CLEAR_USER();
    userStore.SET_INITIALIZED(true);
  };

  return {
    getAccessToken(): string | null {
      return loadSession()?.accessToken ?? null;
    },

    async signIn(request: LoginRequest): Promise<CurrentUser> {
      expire();
      try {
        const tokenResponse = await api.login(request);
        saveSession(toStoredSession(tokenResponse.data, now()));
        const userResponse = await api.getCurrentUser();
        userStore.SET_CURRENT_USER(userResponse.data);
        userStore.SET_INITIALIZED(true);
        return userResponse.data;
      } catch (error) {
        expire();
        throw error;
      }
    },

    async restore(): Promise<CurrentUser | null> {
      if (!loadSession()) {
        expire();
        return null;
      }
      try {
        const response = await api.getCurrentUser();
        userStore.SET_CURRENT_USER(response.data);
        userStore.SET_INITIALIZED(true);
        return response.data;
      } catch (error) {
        expire();
        throw error;
      }
    },

    async refreshAccessToken(): Promise<string> {
      const current = loadSession();
      if (!current) {
        expire();
        throw new Error("Refresh session is unavailable");
      }
      const response = await api.refreshSession(current.refreshToken);
      const replacement = toStoredSession(response.data, now());
      saveSession(replacement);
      return replacement.accessToken;
    },

    async signOut(): Promise<void> {
      const current = loadSession();
      try {
        if (current) await api.logout();
      } finally {
        expire();
      }
    },

    expire
  };
}

export const sessionService = createSessionService({
  userStore: useUserStoreHook(),
  api: { login, refreshSession, logout, getCurrentUser }
});

http.setAuthRefreshCoordinator({
  getAccessToken: () => sessionService.getAccessToken(),
  refresh: () => sessionService.refreshAccessToken(),
  expire: () => sessionService.expire()
});
