import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import ProfileManagement from "./index.vue";

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const stubs = {
  "el-alert": true,
  "el-avatar": true,
  "el-button": { template: "<button><slot /></button>" },
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-descriptions": true,
  "el-descriptions-item": true,
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-skeleton": true,
  "el-tag": true
};

const profile = {
  nickname: "Admin",
  avatarUrl: null,
  bioZh: "中文简介",
  bioJa: null,
  bioEn: "English bio",
  location: null,
  website: "https://example.com",
  emailPublic: null,
  githubUrl: null,
  twitterUrl: null,
  linkedinUrl: null,
  zhihuUrl: null,
  qiitaUrl: null,
  juejinUrl: null
};

const ok = (data: unknown) => ({ code: "00000", msg: "success", data });

function currentUser(type: "ADMIN" | "DEMO") {
  return {
    id: type === "ADMIN" ? "1001" : "1002",
    username: type.toLowerCase(),
    type,
    profile
  };
}

afterEach(() => {
  mock.reset();
  useUserStoreHook().CLEAR_USER();
});

describe("profile management page", () => {
  it("renders editable profile controls for admin users", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("ADMIN")));
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="profile-account-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-form-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-save"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-readonly"]').exists()).toBe(false);
  });

  it("keeps demo users read-only", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("DEMO")));
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="profile-save"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="profile-readonly"]').exists()).toBe(true);
  });
});
