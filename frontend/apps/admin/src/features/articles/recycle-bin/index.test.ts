import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { http } from "@/utils/http";
import { useUserStoreHook } from "@/store/modules/user";
import ArticleRecycleBin from "./index.vue";

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
  id: "100",
  titleZh: "已删除文章",
  titleJa: null,
  titleEn: null,
  status: "PUBLISHED",
  categoryId: "10",
  deletedAt: "2026-06-22T12:00:00",
  deletedBy: "1001"
};
const stubs = {
  "el-alert": true,
  "el-button": true,
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-empty": true,
  "el-pagination": true,
  "el-skeleton": true,
  "el-table": true,
  "el-table-column": {
    data: () => ({ row }),
    template: "<div><slot :row='row' /></div>"
  },
  "el-tag": true
};

function replyPage() {
  mock.onGet("/api/admin/articles/recycle-bin").reply(200, {
    code: "00000",
    msg: "success",
    data: { records: [row], total: 1, page: 1, size: 20 }
  });
  mock.onGet("/api/admin/categories").reply(200, {
    code: "00000",
    msg: "success",
    data: []
  });
}

afterEach(() => {
  mock.reset();
  confirm.mockClear();
  showMessage.mockReset();
});

describe("article recycle bin page", () => {
  it("lets ADMIN confirm and restore an article", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: null
    });
    replyPage();
    mock.onPost("/api/admin/articles/100/restore").reply(200, {
      code: "00000",
      msg: "success",
      data: { id: "100" }
    });

    const wrapper = mount(ArticleRecycleBin, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="article-recycle-card"]').exists()).toBe(
      true
    );
    expect(
      wrapper.find('[data-testid="article-restore-100"]').exists()
    ).toBe(true);

    await wrapper.get('[data-testid="article-restore-100"]').trigger("click");
    await flushPromises();

    expect(confirm).toHaveBeenCalledOnce();
    expect(mock.history.post).toHaveLength(1);
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

    const wrapper = mount(ArticleRecycleBin, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.find('[data-testid="article-operation-column"]').exists()
    ).toBe(false);
    expect(wrapper.find('[data-testid^="article-restore-"]').exists()).toBe(
      false
    );
  });
});
