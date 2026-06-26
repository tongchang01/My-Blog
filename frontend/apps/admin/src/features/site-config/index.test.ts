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

const attachmentPage = {
  records: [
    {
      id: "9007199254743001",
      publicUrl: "http://localhost/media/logo.png",
      contentType: "image/png",
      fileSize: 1024,
      width: 400,
      height: 120,
      originalFilename: "logo.png",
      createdAt: "2026-06-26T12:00:00",
      createdBy: "1001"
    },
    {
      id: "9007199254743002",
      publicUrl: "http://localhost/media/favicon.png",
      contentType: "image/png",
      fileSize: 512,
      width: 64,
      height: 64,
      originalFilename: "favicon.png",
      createdAt: "2026-06-26T12:05:00",
      createdBy: "1001"
    }
  ],
  total: 2,
  page: 1,
  size: 20
};

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
    expect(wrapper.find('[data-testid="site-config-logo-choose"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="site-config-favicon-choose"]').exists()).toBe(false);
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

  it("selects logo and favicon attachments before saving", async () => {
    setUser("ADMIN");
    mock.onGet("/api/admin/site-config").reply(200, ok(siteConfig));
    mock.onGet("/api/admin/attachments").reply(200, ok(attachmentPage));
    mock.onPut("/api/admin/site-config").reply(config => {
      const payload = JSON.parse(config.data);
      expect(payload.logoUrl).toBe("http://localhost/media/logo.png");
      expect(payload.faviconUrl).toBe("http://localhost/media/favicon.png");
      return [200, ok(siteConfig)];
    });
    const wrapper = mount(SiteConfigManagement, { global: { stubs } });
    await flushPromises();

    await wrapper.get('[data-testid="site-config-logo-choose"]').trigger("click");
    await flushPromises();
    await wrapper
      .get('[data-testid="attachment-picker-select-9007199254743001"]')
      .trigger("click");
    await wrapper.get('[data-testid="site-config-favicon-choose"]').trigger("click");
    await flushPromises();
    await wrapper
      .get('[data-testid="attachment-picker-select-9007199254743002"]')
      .trigger("click");
    await wrapper.get('[data-testid="site-config-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.put).toHaveLength(1);
  });
});
