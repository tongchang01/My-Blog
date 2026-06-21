import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ElMessageBox } from "element-plus";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import TagManagement from "./index.vue";

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;
const stubs = {
  "el-alert": true,
  "el-button": { template: "<button @click='$emit(\"click\")'><slot /></button>" },
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-dialog": { template: "<div><slot /><slot name='footer' /></div>" },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-skeleton": true,
  "el-table": true,
  "el-table-column": { template: "<div />" },
  "el-tag": true
};

const profile = {
  nickname: "管理员",
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

function replyList() {
  mock.onGet("/api/admin/tags").reply(200, {
    code: "00000",
    msg: "success",
    data: [
      {
        id: "200",
        nameZh: "Vue",
        nameJa: null,
        nameEn: "Vue",
        slug: "vue",
        createdAt: "2026-06-20T10:00:00",
        createdBy: "1001",
        updatedAt: "2026-06-21T10:00:00",
        updatedBy: "1001"
      }
    ]
  });
}

afterEach(() => {
  mock.reset();
  vi.restoreAllMocks();
});

describe("tag management page", () => {
  it("renders admin write controls without category sorting", async () => {
    useUserStoreHook().SET_CURRENT_USER({ id: "1001", username: "admin", type: "ADMIN", profile });
    replyList();
    const wrapper = mount(TagManagement, { global: { stubs } });
    await flushPromises();
    expect(wrapper.find('[data-testid="tag-filter-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="tag-result-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="tag-create"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="tag-operation-column"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="tag-save-sort"]').exists()).toBe(false);
  });

  it("keeps demo users read-only", async () => {
    useUserStoreHook().SET_CURRENT_USER({ id: "1002", username: "demo", type: "DEMO", profile });
    replyList();
    const wrapper = mount(TagManagement, { global: { stubs } });
    await flushPromises();
    expect(wrapper.find('[data-testid="tag-create"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="tag-operation-column"]').exists()).toBe(false);
  });

  it("confirms before delegating tag deletion", async () => {
    useUserStoreHook().SET_CURRENT_USER({ id: "1001", username: "admin", type: "ADMIN", profile });
    replyList();
    vi.spyOn(ElMessageBox, "confirm").mockResolvedValue({ action: "confirm" } as any);
    const wrapper = mount(TagManagement, { global: { stubs } });
    await flushPromises();
    const remove = vi.spyOn((wrapper.vm as any).state, "remove");
    await (wrapper.vm as any).confirmRemove("200");
    expect(ElMessageBox.confirm).toHaveBeenCalledOnce();
    expect(remove).toHaveBeenCalledWith("200");
  });
});
