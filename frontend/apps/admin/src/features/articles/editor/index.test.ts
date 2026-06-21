import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { http } from "@/utils/http";
import ArticleEditor from "./index.vue";

const routerState = vi.hoisted(() => ({
  push: vi.fn(),
  route: { name: "ArticleCreate", params: {} as Record<string, string> }
}));

vi.mock("vue-router", () => ({
  useRoute: () => routerState.route,
  useRouter: () => ({ push: routerState.push })
}));

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;
const stubs = {
  "el-alert": true,
  "el-button": true,
  "el-card": true,
  "el-date-picker": true,
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-option": true,
  "el-select": true,
  "el-skeleton": true
};

function dictionaries() {
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
  routerState.push.mockReset();
  routerState.route = { name: "ArticleCreate", params: {} };
});

describe("article editor page", () => {
  it("renders the create form and navigates after a successful save", async () => {
    dictionaries();
    mock.onPost("/api/admin/articles").reply(200, {
      code: "00000",
      msg: "success",
      data: { id: "101" }
    });
    const wrapper = mount(ArticleEditor, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="article-editor"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="article-save"]').exists()).toBe(true);
    Object.assign((wrapper.vm as any).form, {
      titleZh: "标题",
      summaryZh: "摘要",
      body: "正文"
    });
    await wrapper.get('[data-testid="article-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.post).toHaveLength(1);
    expect(routerState.push).toHaveBeenCalledWith("/articles/list");
  });

  it("loads an existing article in edit mode", async () => {
    routerState.route = { name: "ArticleEdit", params: { id: "100" } };
    dictionaries();
    mock.onGet("/api/admin/articles/100").reply(200, {
      code: "00000",
      msg: "success",
      data: {
        id: "100",
        titleZh: "已有标题",
        titleJa: null,
        titleEn: null,
        summaryZh: "摘要",
        summaryJa: null,
        summaryEn: null,
        body: "正文",
        categoryId: null,
        categoryNameZh: null,
        authorId: "1001",
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
    });

    const wrapper = mount(ArticleEditor, { global: { stubs } });
    await flushPromises();

    expect((wrapper.vm as any).form.titleZh).toBe("已有标题");
    expect(mock.history.get.some(item => item.url?.endsWith("/100"))).toBe(true);
  });
});
