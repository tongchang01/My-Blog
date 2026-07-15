import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import FriendLinkManagement from "./index.vue";

const { confirm, showMessage } = vi.hoisted(() => ({
  confirm: vi.fn().mockResolvedValue("confirm"),
  showMessage: vi.fn()
}));

vi.mock("element-plus", async importOriginal => ({
  ...(await importOriginal<typeof import("element-plus")>()),
  ElMessageBox: { confirm }
}));
vi.mock("@/utils/message", () => ({ message: showMessage }));

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const row = {
  id: "9007199254742501",
  name: "Example",
  url: "https://example.com",
  avatarUrl: null,
  description: "Example site",
  sortOrder: 10,
  status: "VISIBLE",
  createdAt: "2026-06-24T10:00:00",
  createdBy: "1001",
  updatedAt: "2026-06-24T11:00:00",
  updatedBy: "1001"
};

const attachmentPage = {
  records: [
    {
      id: "9007199254743001",
      publicUrl: "http://localhost/media/friend.png",
      contentType: "image/png",
      fileSize: 1024,
      width: 400,
      height: 400,
      originalFilename: "friend.png",
      createdAt: "2026-06-26T12:00:00",
      createdBy: "1001"
    }
  ],
  total: 1,
  page: 1,
  size: 20
};

const stubs = {
  "el-alert": true,
  "el-avatar": true,
  "el-button": true,
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-dialog": { template: "<div><slot /><slot name='footer' /></div>" },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-image": true,
  "el-input": true,
  "el-input-number": true,
  "el-option": true,
  "el-pagination": true,
  "el-select": true,
  "el-skeleton": true,
  "el-table": true,
  "el-table-column": {
    data: () => ({ row }),
    template: "<div><slot :row='row' /></div>"
  },
  "el-tag": true
};

function replyPage(records = [row]) {
  mock.onGet("/api/admin/friend-links").reply(200, {
    code: "00000",
    msg: "success",
    data: { records, total: records.length, page: 1, size: 20 }
  });
}

afterEach(() => {
  mock.reset();
  confirm.mockClear();
  showMessage.mockClear();
});

describe("friend link management page", () => {
  it("renders pagination and ADMIN actions without unsupported filters", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: null
    });
    replyPage();
    mock.onPatch("/api/admin/friend-links/9007199254742501/status").reply(200, {
      code: "00000",
      msg: "success",
      data: { ...row, status: "HIDDEN" }
    });

    const wrapper = mount(FriendLinkManagement, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.find('[data-testid="friend-link-result-card"]').exists()
    ).toBe(true);
    expect(wrapper.find('[data-testid="friend-link-filter-card"]').exists()).toBe(
      false
    );
    expect(
      wrapper.find('[data-testid="friend-link-operation-column"]').exists()
    ).toBe(true);
    expect(
      wrapper.find('[data-testid="friend-link-hide-9007199254742501"]').exists()
    ).toBe(true);

    await wrapper
      .get('[data-testid="friend-link-hide-9007199254742501"]')
      .trigger("click");
    await flushPromises();

    expect(confirm).toHaveBeenCalledOnce();
    expect(mock.history.patch[0].url).toBe(
      "/api/admin/friend-links/9007199254742501/status"
    );
    expect(showMessage).toHaveBeenCalledWith(expect.any(String), {
      type: "success"
    });
  });

  it("keeps DEMO users read-only", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1002",
      username: "demo",
      type: "DEMO",
      profile: null
    });
    replyPage();

    const wrapper = mount(FriendLinkManagement, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.find('[data-testid="friend-link-operation-column"]').exists()
    ).toBe(false);
    expect(wrapper.find('[data-testid="friend-link-create"]').exists()).toBe(
      false
    );
    expect(wrapper.find('[data-testid^="friend-link-hide-"]').exists()).toBe(
      false
    );
  });

  it("supports retry after a list error and renders empty state", async () => {
    mock
      .onGet("/api/admin/friend-links")
      .replyOnce(500)
      .onGet("/api/admin/friend-links")
      .reply(200, {
        code: "00000",
        msg: "success",
        data: { records: [], total: 0, page: 1, size: 20 }
      });

    const wrapper = mount(FriendLinkManagement, { global: { stubs } });
    await flushPromises();
    expect(wrapper.find('[data-testid="friend-link-error"]').exists()).toBe(
      true
    );

    await wrapper.get('[data-testid="friend-link-retry"]').trigger("click");
    await flushPromises();

    expect(
      mock.history.get.filter(item => item.url === "/api/admin/friend-links")
    ).toHaveLength(2);
    expect(wrapper.find('[data-testid="friend-link-empty"]').exists()).toBe(
      true
    );
  });

  it("selects an avatar attachment before creating a friend link", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: null
    });
    replyPage();
    mock.onGet("/api/admin/attachments").reply(200, {
      code: "00000",
      msg: "success",
      data: attachmentPage
    });
    mock.onPost("/api/admin/friend-links").reply(config => {
      expect(JSON.parse(config.data).avatarUrl).toBe(
        "http://localhost/media/friend.png"
      );
      return [200, { code: "00000", msg: "success", data: row }];
    });

    const wrapper = mount(FriendLinkManagement, { global: { stubs } });
    await flushPromises();

    await wrapper.get('[data-testid="friend-link-create"]').trigger("click");
    await wrapper
      .get('[data-testid="friend-link-avatar-choose"]')
      .trigger("click");
    await flushPromises();
    await wrapper
      .get('[data-testid="attachment-picker-select-9007199254743001"]')
      .trigger("click");
    const vm = wrapper.vm as unknown as {
      form: { name: string; url: string };
    };
    vm.form.name = "Example";
    vm.form.url = "https://example.com";
    await wrapper.get('[data-testid="friend-link-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.post).toHaveLength(1);
    expect(showMessage).toHaveBeenCalledWith(expect.any(String), {
      type: "success"
    });
  });
});
