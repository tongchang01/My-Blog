import { describe, expect, it } from "vitest";
import type { UserProfile } from "@/features/auth/model";
import {
  createUserProfileForm,
  userProfileFormToPayload,
  userProfileToForm,
  validateUserProfileForm
} from "./form";

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

describe("user profile form", () => {
  it("creates defaults and maps nullable profile fields to editable strings", () => {
    expect(createUserProfileForm()).toEqual({
      nickname: "",
      avatarUrl: "",
      bioZh: "",
      bioJa: "",
      bioEn: "",
      location: "",
      website: "",
      emailPublic: "",
      githubUrl: "",
      twitterUrl: "",
      linkedinUrl: "",
      zhihuUrl: "",
      qiitaUrl: "",
      juejinUrl: ""
    });
    expect(userProfileToForm(profile)).toMatchObject({
      nickname: "Admin",
      avatarUrl: "",
      bioJa: "",
      website: "https://example.com"
    });
  });

  it("requires nickname", () => {
    expect(validateUserProfileForm(createUserProfileForm())).toEqual({
      nickname: "required"
    });
  });

  it("validates public-profile lengths, URL fields and email", () => {
    expect(
      validateUserProfileForm({
        ...userProfileToForm(profile),
        nickname: "x".repeat(65),
        website: "not-a-url",
        emailPublic: "not-an-email",
        bioZh: "x".repeat(5_001)
      })
    ).toEqual({
      nickname: "maxLength",
      website: "url",
      emailPublic: "email",
      bioZh: "maxLength"
    });
  });

  it("normalizes whitespace and emits nullable optional fields", () => {
    expect(
      userProfileFormToPayload({
        ...userProfileToForm(profile),
        nickname: " Admin ",
        avatarUrl: " ",
        location: " Tokyo "
      })
    ).toEqual({
      nickname: "Admin",
      avatarUrl: null,
      bioZh: "中文简介",
      bioJa: null,
      bioEn: "English bio",
      location: "Tokyo",
      website: "https://example.com",
      emailPublic: null,
      githubUrl: "https://github.com/example",
      twitterUrl: null,
      linkedinUrl: null,
      zhihuUrl: null,
      qiitaUrl: null,
      juejinUrl: null
    });
  });
});
