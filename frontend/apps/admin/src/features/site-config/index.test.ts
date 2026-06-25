import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import SiteConfigManagement from "./index.vue";

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const stubs = {
  "el-alert": true,
  "el-button": { template: "<button><slot /></button>" },
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-skeleton": true,
  "el-tag": true
};

const profile = {
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
};

const siteConfig = {
  siteTitleZh: "中文标题",
  siteTitleJa: "日本語タイトル",
  siteTitleEn: "English title",
  siteSubtitleZh: "中文副标题",
  siteSubtitleJa: null,
  siteSubtitleEn: null,
  aboutMdZh: "中文关于",
  aboutMdJa: null,
  aboutMdEn: null,
  logoUrl: null,
  faviconUrl: null,
  icpNo: null,
  spotifyPlaylistId: null,
  updatedAt: "2026-06-25T10:00:00",
  updatedBy: "1001"
};

const ok = (data: unknown) => ({ code: "00000", msg: "success", data });

function setUser(type: "ADMIN" | "DEMO") {
  useUserStoreHook().SET_CURRENT_USER({
    id: type === "ADMIN" ? "1001" : "1002",
    username: type.toLowerCase(),
    type,
    profile
  });
}

afterEach(() => {
  mock.reset();
  useUserStoreHook().CLEAR_USER();
});

describe("site config management page", () => {
  it("renders editable save controls for admin users", async () => {
    setUser("ADMIN");
    mock.onGet("/api/admin/site-config").reply(200, ok(siteConfig));
    const wrapper = mount(SiteConfigManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="site-config-basic-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="site-config-about-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="site-config-save"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="site-config-readonly"]').exists()).toBe(false);
  });

  it("keeps demo users read-only", async () => {
    setUser("DEMO");
    mock.onGet("/api/admin/site-config").reply(200, ok(siteConfig));
    const wrapper = mount(SiteConfigManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="site-config-save"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="site-config-readonly"]').exists()).toBe(true);
  });

  it("shows loading failure and retry action", async () => {
    setUser("ADMIN");
    mock
      .onGet("/api/admin/site-config")
      .replyOnce(500)
      .onGet("/api/admin/site-config")
      .reply(200, ok(siteConfig));
    const wrapper = mount(SiteConfigManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="site-config-error"]').exists()).toBe(true);
    await wrapper.get('[data-testid="site-config-retry"]').trigger("click");
    await flushPromises();
    expect(mock.history.get).toHaveLength(2);
  });
});
