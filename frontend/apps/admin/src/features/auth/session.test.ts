import { describe, expect, it, vi } from "vitest";
import { loadSession } from "./session-storage";
import { articleDraftKey } from "@/features/articles/editor/draftStorage";
import type { CurrentUser, TokenPair } from "./model";
import { createSessionService, type SessionUserStore } from "./session";

const credentials = { username: "admin", password: "secret" };
const tokens: TokenPair = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  accessExpiresIn: 900,
  refreshExpiresIn: 604800
};
const currentUser: CurrentUser = {
  id: "9007199254740993",
  username: "admin",
  type: "ADMIN",
  profile: {
    nickname: "Admin",
    avatarUrl: null,
    bioZh: null,
    bioJa: null,
    bioEn: null,
    location: null,
    website: null,
    emailPublic: null,
    githubUrl: null,
    twitterUrl: null,
    linkedinUrl: null,
    zhihuUrl: null,
    qiitaUrl: null,
    juejinUrl: null
  }
};

function createUserStore(): SessionUserStore {
  return {
    currentUser: null,
    initialized: false,
    SET_CURRENT_USER(user) {
      this.currentUser = user;
    },
    SET_INITIALIZED(initialized) {
      this.initialized = initialized;
    },
    CLEAR_USER() {
      this.currentUser = null;
    }
  };
}

describe("admin session service", () => {
  it("keeps login atomic when loading the current user fails", async () => {
    const userStore = createUserStore();
    const service = createSessionService({
      userStore,
      api: {
        login: vi.fn().mockResolvedValue({ data: tokens }),
        getCurrentUser: vi.fn().mockRejectedValue(new Error("me failed")),
        refreshSession: vi.fn(),
        logout: vi.fn()
      }
    });

    await expect(service.signIn(credentials)).rejects.toThrow("me failed");
    expect(loadSession()).toBeNull();
    expect(userStore.currentUser).toBeNull();
  });

  it("persists tokens and user only after login and current-user succeed", async () => {
    const userStore = createUserStore();
    const service = createSessionService({
      userStore,
      api: {
        login: vi.fn().mockResolvedValue({ data: tokens }),
        getCurrentUser: vi.fn().mockResolvedValue({ data: currentUser }),
        refreshSession: vi.fn(),
        logout: vi.fn()
      }
    });

    await service.signIn(credentials);

    expect(loadSession()).toMatchObject({
      accessToken: "access-token",
      refreshToken: "refresh-token"
    });
    expect(userStore.currentUser).toEqual(currentUser);
  });

  it("clears local state even when server logout fails", async () => {
    const userStore = createUserStore();
    const api = {
      login: vi.fn().mockResolvedValue({ data: tokens }),
      getCurrentUser: vi.fn().mockResolvedValue({ data: currentUser }),
      refreshSession: vi.fn(),
      logout: vi.fn().mockRejectedValue(new Error("logout failed"))
    };
    const service = createSessionService({ userStore, api });
    await service.signIn(credentials);
    localStorage.setItem(articleDraftKey(currentUser.id, "create"), "draft");

    await expect(service.signOut()).rejects.toThrow("logout failed");
    expect(api.logout).toHaveBeenCalledWith();
    expect(loadSession()).toBeNull();
    expect(
      localStorage.getItem(articleDraftKey(currentUser.id, "create"))
    ).toBeNull();
    expect(userStore.currentUser).toBeNull();
  });
});
