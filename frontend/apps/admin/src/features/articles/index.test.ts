import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { http } from "@/utils/http";
import { useUserStoreHook } from "@/store/modules/user";
import ArticleList from "./index.vue";

const { confirm, showMessage } = vi.hoisted(() => ({
  confirm: vi.fn().mockResolvedValue("confirm"),
  showMessage: vi.fn()
}));

vi.mock("element-plus", async importOriginal => ({
  ...(await importOriginal<typeof import("element-plus")>()),
  ElMessageBox: { confirm }
}));

vi.mock("vue-router", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/utils/message", () => ({ message: showMessage }));

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;
const stubs = {
  "el-alert": true,
  "el-button": true,
  "el-card": {
    template: "<div><slot name='header' /><slot /></div>"
  },
  "el-date-picker": true,
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
    data: () => ({
      row: {
        id: "100",
        titleZh: "标题",
        titleJa: null,
        titleEn: null,
        summaryZh: null,
        summaryJa: null,
        summaryEn: null,
        categoryId: null,
        categoryNameZh: null,
        slug: "article-100",
        status: "DRAFT",
        homepageSlot: "NONE",
        publishAt: null,
        coverAttachmentId: null,
        coverUrl: null,
        commentCount: 0,
        tagIds: [],
        createdAt: "2026-06-20T10:00:00",
        createdBy: "1001",
        updatedAt: "2026-06-21T10:00:00",
        updatedBy: "1001"
      }
    }),
    template: "<div><slot :row='row' /></div>"
  },
  "el-tag": true
};

function replyDictionaries() {
  mock.onGet("/api/admin/categories").reply(200, {
    code: "00000",
    msg: "success",
    data: []
  });
  mock.onGet("/api/admin/tags").reply(200, {
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

describe("article list page", () => {
  it("renders independent cards and admin write controls", async () => {
    useUserStoreHook().SET_CURRENT_USER({
      id: "1001",
      username: "admin",
      type: "ADMIN",
      profile: {
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
      }
    });
    replyDictionaries();
    mock.onGet("/api/admin/articles").reply(200, {
      code: "00000",
      msg: "success",
      data: {
        records: [
          {
            id: "100",
            titleZh: "标题",
            titleJa: null,
            titleEn: null,
            summaryZh: "摘要",
            summaryJa: null,
            summaryEn: null,
            categoryId: null,
            categoryNameZh: null,
            slug: "article-100",
            status: "DRAFT",
            homepageSlot: "PINNED",
            publishAt: null,
            coverAttachmentId: null,
            coverUrl: null,
            commentCount: 0,
            tagIds: [],
            createdAt: "2026-06-20T10:00:00",
            createdBy: "1001",
            updatedAt: "2026-06-21T10:00:00",
            updatedBy: "1001"
          }
        ],
        total: 1,
        page: 1,
        size: 20
      }
    });

    const wrapper = mount(ArticleList, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="article-filter-card"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="article-result-card"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="title-filter"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="status-filter"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="category-filter"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="tag-filter"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="created-at-filter"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="publish-at-filter"]').exists()).toBe(
      true
    );
    expect(
      wrapper.find('[data-testid="article-homepage-slot-column"]').exists()
    ).toBe(true);
    expect(wrapper.find('[data-testid="article-empty"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="article-create"]').exists()).toBe(true);
    expect(
      wrapper.find('[data-testid="article-operation-column"]').exists()
    ).toBe(true);
    expect(wrapper.find('[data-testid="article-delete-100"]').exists()).toBe(
      true
    );

    mock.onDelete("/api/admin/articles/100").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });
    await wrapper.get('[data-testid="article-delete-100"]').trigger("click");
    await flushPromises();

    expect(confirm).toHaveBeenCalledOnce();
    expect(mock.history.delete).toHaveLength(1);
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
    replyDictionaries();
    mock.onGet("/api/admin/articles").reply(200, {
      code: "00000",
      msg: "success",
      data: { records: [], total: 0, page: 1, size: 20 }
    });

    const wrapper = mount(ArticleList, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.find('[data-testid="article-operation-column"]').exists()
    ).toBe(false);
    expect(wrapper.find('[data-testid^="article-delete-"]').exists()).toBe(
      false
    );
  });

  it("supports refresh and retry after a list error", async () => {
    replyDictionaries();
    mock
      .onGet("/api/admin/articles")
      .replyOnce(500)
      .onGet("/api/admin/articles")
      .reply(200, {
        code: "00000",
        msg: "success",
        data: { records: [], total: 0, page: 1, size: 20 }
      });

    const wrapper = mount(ArticleList, { global: { stubs } });
    await flushPromises();
    expect(wrapper.find('[data-testid="article-error"]').exists()).toBe(true);

    await wrapper.get('[data-testid="article-retry"]').trigger("click");
    await flushPromises();

    expect(
      mock.history.get.filter(item => item.url === "/api/admin/articles")
    ).toHaveLength(2);
    expect(wrapper.find('[data-testid="article-empty"]').exists()).toBe(true);
  });
});
