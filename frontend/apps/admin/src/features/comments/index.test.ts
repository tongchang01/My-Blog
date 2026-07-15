import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { http } from "@/utils/http";
import { useUserStoreHook } from "@/store/modules/user";
import CommentManagement from "./index.vue";

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
  id: "9007199254740995",
  targetType: "ARTICLE",
  targetId: "9007199254740993",
  parentId: null,
  replyToCommentId: null,
  replyToNickname: null,
  authorNickname: "TYB",
  authorEmail: "tyb@example.com",
  authorSite: "https://example.com",
  authorIp: "127.0.0.1",
  authorUserAgent: "Vitest",
  contentMd: "hello comment",
  contentHtml: "<p>hello comment</p>",
  auditStatus: "PENDING",
  createdAt: "2026-06-23T12:00:00",
  deleted: false
};
const passedRow = {
  ...row,
  auditStatus: "PASS"
};
const replyRow = {
  ...passedRow,
  id: "9007199254740996",
  parentId: "9007199254740995",
  replyToCommentId: "9007199254740995",
  replyToNickname: "TYB",
  contentMd: "reply comment"
};
let tableRow = row;
const stubs = {
  "el-alert": true,
  "el-button": true,
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-checkbox": true,
  "el-dialog": { template: "<div><slot /><slot name='footer' /></div>" },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-option": true,
  "el-pagination": true,
  "el-select": true,
  "el-skeleton": true,
  "el-table": true,
  "el-table-column": {
    template: "<div><slot :row='tableRow' /></div>",
    setup() {
      return { tableRow };
    }
  },
  "el-tag": true
};

function replyPage(records = [row]) {
  mock.onGet("/api/admin/comments").reply(200, {
    code: "00000",
    msg: "success",
    data: { records, total: records.length, page: 1, size: 20 }
  });
}

afterEach(() => {
  mock.reset();
  confirm.mockClear();
  showMessage.mockClear();
  tableRow = row;
});

describe("comment management page", () => {
  it("renders filters, results and ADMIN moderation actions", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: null
    });
    replyPage();
    mock.onPost("/api/admin/comments/9007199254740995/approve").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });

    const wrapper = mount(CommentManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="comment-filter-card"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="comment-result-card"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="comment-target-type"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="comment-audit-status"]').exists()).toBe(
      true
    );
    expect(
      wrapper.find('[data-testid="comment-operation-column"]').exists()
    ).toBe(true);
    expect(
      wrapper.find('[data-testid="comment-approve-9007199254740995"]').exists()
    ).toBe(true);
    expect(wrapper.text()).toContain("Audit details");

    await wrapper
      .get('[data-testid="comment-approve-9007199254740995"]')
      .trigger("click");
    await flushPromises();

    expect(confirm).toHaveBeenCalledOnce();
    expect(mock.history.post[0].url).toBe(
      "/api/admin/comments/9007199254740995/approve"
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

    const wrapper = mount(CommentManagement, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.find('[data-testid="comment-operation-column"]').exists()
    ).toBe(false);
    expect(wrapper.find('[data-testid^="comment-approve-"]').exists()).toBe(
      false
    );
  });

  it("opens the reply dialog and submits an admin reply", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: null
    });
    tableRow = passedRow;
    replyPage([passedRow]);
    mock.onPost("/api/admin/comments/9007199254740995/reply").reply(200, {
      code: "00000",
      msg: "success",
      data: {
        id: "9007199254740997",
        auditStatus: "PASS"
      }
    });

    const wrapper = mount(CommentManagement, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.find('[data-testid="comment-reply-9007199254740995"]').exists()
    ).toBe(true);

    await wrapper
      .get('[data-testid="comment-reply-9007199254740995"]')
      .trigger("click");
    await flushPromises();

    expect(wrapper.find('[data-testid="comment-reply-dialog"]').exists()).toBe(
      true
    );
    expect(wrapper.text()).toContain("TYB");

    (wrapper.vm as unknown as { replyContent: string }).replyContent =
      "谢谢反馈";
    await wrapper.get('[data-testid="comment-reply-submit"]').trigger("click");
    await flushPromises();

    expect(mock.history.post[0].url).toBe(
      "/api/admin/comments/9007199254740995/reply"
    );
    expect(JSON.parse(mock.history.post[0].data)).toEqual({
      contentMd: "谢谢反馈"
    });
    expect(showMessage).toHaveBeenCalledWith(expect.any(String), {
      type: "success"
    });
  });

  it("renders reply target metadata", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: null
    });
    tableRow = replyRow;
    replyPage([replyRow]);

    const wrapper = mount(CommentManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.text()).toContain("Reply to @TYB");
  });

  it("supports retry after a list error and renders empty state", async () => {
    mock
      .onGet("/api/admin/comments")
      .replyOnce(500)
      .onGet("/api/admin/comments")
      .reply(200, {
        code: "00000",
        msg: "success",
        data: { records: [], total: 0, page: 1, size: 20 }
      });

    const wrapper = mount(CommentManagement, { global: { stubs } });
    await flushPromises();
    expect(wrapper.find('[data-testid="comment-error"]').exists()).toBe(true);

    await wrapper.get('[data-testid="comment-retry"]').trigger("click");
    await flushPromises();

    expect(
      mock.history.get.filter(item => item.url === "/api/admin/comments")
    ).toHaveLength(2);
    expect(wrapper.find('[data-testid="comment-empty"]').exists()).toBe(true);
  });
});
