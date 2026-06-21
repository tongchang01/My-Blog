import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { http } from "@/utils/http";
import { useUserStoreHook } from "@/store/modules/user";
import ArticleList from "./index.vue";

vi.mock("vue-router", () => ({ useRouter: () => ({ push: vi.fn() }) }));

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;
const stubs = {
  "el-alert": true,
  "el-button": true,
  "el-card": {
    template: "<div><slot name='header' /><slot /></div>"
  },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-option": true,
  "el-pagination": true,
  "el-select": true,
  "el-skeleton": true,
  "el-table": true,
  "el-table-column": { template: "<div />" },
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

afterEach(() => mock.reset());

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
    expect(wrapper.find('[data-testid="article-empty"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="article-create"]').exists()).toBe(true);
    expect(
      wrapper.find('[data-testid="article-operation-column"]').exists()
    ).toBe(true);
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
