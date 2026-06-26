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
  "el-dialog": { template: "<div><slot /></div>" },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-image": true,
  "el-input": true,
  "el-pagination": true,
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

const attachmentPage = {
  records: [
    {
      id: "9007199254743001",
      publicUrl: "http://localhost/media/avatar.png",
      contentType: "image/png",
      fileSize: 1024,
      width: 400,
      height: 400,
      originalFilename: "avatar.png",
      createdAt: "2026-06-26T12:00:00",
      createdBy: "1001"
    }
  ],
  total: 1,
  page: 1,
  size: 20
};

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
    expect(wrapper.find('[data-testid="profile-avatar-choose"]').exists()).toBe(false);
  });

  it("selects an avatar attachment and saves the public URL", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("ADMIN")));
    mock.onGet("/api/admin/attachments").reply(200, ok(attachmentPage));
    mock.onPatch("/api/auth/me/profile").reply(config => {
      expect(JSON.parse(config.data).avatarUrl).toBe(
        "http://localhost/media/avatar.png"
      );
      return [200, ok(profile)];
    });
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    await wrapper.get('[data-testid="profile-avatar-choose"]').trigger("click");
    await flushPromises();
    await wrapper
      .get('[data-testid="attachment-picker-select-9007199254743001"]')
      .trigger("click");
    await wrapper.get('[data-testid="profile-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.patch).toHaveLength(1);
  });
});
