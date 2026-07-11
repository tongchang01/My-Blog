import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import type { UserProfilePayload } from "@/features/profile/form";
import { http } from "@/utils/http";
import { changeCurrentUserPassword, updateCurrentUserProfile } from "./auth";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

const ok = (data: unknown = null) => ({
  code: "00000",
  msg: "success",
  data
});

describe("auth API", () => {
  it("patches the current user profile with complete profile payload", async () => {
    const payload: UserProfilePayload = {
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
    mock.onPatch("/api/auth/me/profile").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok(payload)];
    });

    await expect(updateCurrentUserProfile(payload)).resolves.toMatchObject({
      data: { nickname: "Admin" }
    });
  });

  it("changes the current user password through the existing endpoint", async () => {
    const payload = {
      currentPassword: "old-password",
      newPassword: "new-password"
    };
    mock.onPut("/api/auth/me/password").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok()];
    });

    await expect(changeCurrentUserPassword(payload)).resolves.toMatchObject({
      code: "00000"
    });
  });
});
