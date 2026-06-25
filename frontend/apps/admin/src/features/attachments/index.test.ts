import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import AttachmentManagement from "./index.vue";

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const stubs = {
  "el-alert": true,
  "el-button": { template: "<button><slot /></button>" },
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-empty": true,
  "el-image": true,
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

const ok = (data: unknown) => ({ code: "00000", msg: "success", data });

function page() {
  return {
    records: [
      {
        id: "9007199254743001",
        publicUrl: "http://localhost/media/a.png",
        contentType: "image/png",
        fileSize: 1024,
        width: 800,
        height: 450,
        originalFilename: "a.png",
        createdAt: "2026-06-25T12:00:00",
        createdBy: "1001"
      }
    ],
    total: 1,
    page: 1,
    size: 20
  };
}

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
  vi.unstubAllGlobals();
  useUserStoreHook().CLEAR_USER();
});

describe("attachment management page", () => {
  it("renders upload controls and attachment cards for admin users", async () => {
    setUser("ADMIN");
    mock.onGet("/api/admin/attachments").reply(200, ok(page()));
    const wrapper = mount(AttachmentManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="attachment-upload-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="attachment-file-input"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="attachment-readonly"]').exists()).toBe(false);
    expect(wrapper.text()).toContain("a.png");
    expect(wrapper.text()).toContain("800 × 450");
    expect(wrapper.text()).toContain("1 KB");
  });

  it("keeps demo users read-only", async () => {
    setUser("DEMO");
    mock.onGet("/api/admin/attachments").reply(200, ok(page()));
    const wrapper = mount(AttachmentManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="attachment-file-input"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="attachment-readonly"]').exists()).toBe(true);
  });

  it("uploads the selected file and refreshes the list", async () => {
    setUser("ADMIN");
    mock
      .onGet("/api/admin/attachments")
      .reply(200, ok(page()))
      .onPost("/api/admin/attachments")
      .reply(200, ok(page().records[0]));
    const wrapper = mount(AttachmentManagement, { global: { stubs } });
    await flushPromises();

    const input = wrapper.get<HTMLInputElement>(
      '[data-testid="attachment-file-input"]'
    );
    Object.defineProperty(input.element, "files", {
      value: [new File(["png"], "a.png", { type: "image/png" })]
    });
    await input.trigger("change");
    await flushPromises();

    expect(mock.history.post).toHaveLength(1);
    expect(mock.history.get).toHaveLength(2);
  });

  it("copies the public URL", async () => {
    setUser("ADMIN");
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    mock.onGet("/api/admin/attachments").reply(200, ok(page()));
    const wrapper = mount(AttachmentManagement, { global: { stubs } });
    await flushPromises();

    await wrapper.get('[data-testid="attachment-copy-9007199254743001"]').trigger("click");

    expect(writeText).toHaveBeenCalledWith("http://localhost/media/a.png");
  });
});
