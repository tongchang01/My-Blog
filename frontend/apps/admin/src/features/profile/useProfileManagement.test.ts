import { describe, expect, it, vi, beforeEach } from "vitest";
import type { ApiResponse } from "@/api/contract";
import type { CurrentUser, UserProfile } from "@/features/auth/model";
import { useUserStoreHook } from "@/store/modules/user";
import {
  type ProfileManagementApi,
  useProfileManagement
} from "./useProfileManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

const profile: UserProfile = {
  nickname: "Admin",
  avatarUrl: null,
  bioZh: "中文简介",
  bioJa: null,
  bioEn: "English bio",
  location: null,
  website: "https://example.com",
  emailPublic: null,
  githubUrl: "https://github.com/example",
  twitterUrl: null,
  linkedinUrl: null,
  zhihuUrl: null,
  qiitaUrl: null,
  juejinUrl: null
};

const currentUser: CurrentUser = {
  id: "1001",
  username: "admin",
  type: "ADMIN",
  profile
};

function api(
  overrides: Partial<ProfileManagementApi> = {}
): ProfileManagementApi {
  return {
    getCurrentUser: vi.fn().mockResolvedValue(ok(currentUser)),
    updateCurrentUserProfile: vi
      .fn()
      .mockResolvedValue(ok({ ...profile, nickname: "Updated" })),
    ...overrides
  };
}

beforeEach(() => {
  useUserStoreHook().CLEAR_USER();
});

describe("profile management state", () => {
  it("loads current user into store and editable form", async () => {
    const source = api();
    const state = useProfileManagement(source);

    await state.initialize();

    expect(source.getCurrentUser).toHaveBeenCalledOnce();
    expect(state.currentUser.value?.username).toBe("admin");
    expect(state.form.nickname).toBe("Admin");
    expect(useUserStoreHook().currentUser?.id).toBe("1001");
  });

  it("saves profile and synchronizes the user store", async () => {
    const source = api();
    const state = useProfileManagement(source);
    await state.initialize();
    state.form.nickname = " Updated ";

    await expect(state.save()).resolves.toBe(true);

    expect(source.updateCurrentUserProfile).toHaveBeenCalledWith(
      expect.objectContaining({ nickname: "Updated", avatarUrl: null })
    );
    expect(state.currentUser.value?.profile.nickname).toBe("Updated");
    expect(useUserStoreHook().currentUser?.profile.nickname).toBe("Updated");
  });

  it("keeps form data and exposes save errors", async () => {
    const source = api({
      updateCurrentUserProfile: vi.fn().mockRejectedValue(new Error("offline"))
    });
    const state = useProfileManagement(source);
    await state.initialize();
    state.form.nickname = "离线昵称";

    await expect(state.save()).resolves.toBe(false);

    expect(state.form.nickname).toBe("离线昵称");
    expect(state.saveError.value?.message).toBe("offline");
  });
});
