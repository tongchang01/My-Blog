import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { nextTick } from "vue";
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
  "el-dialog": { template: "<div><slot /></div>" },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-image": true,
  "el-pagination": true,
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
        coverAttachmentId: "9007199254743001",
        coverUrl: "http://localhost/media/cover.png",
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
    expect(wrapper.text()).toContain("9007199254743001");
    expect(wrapper.text()).toContain("http://localhost/media/cover.png");
    expect(mock.history.get.some(item => item.url?.endsWith("/100"))).toBe(true);
  });

  it("selects a cover attachment and includes it in the save payload", async () => {
    dictionaries();
    mock.onGet("/api/admin/attachments").reply(200, {
      code: "00000",
      msg: "success",
      data: {
        records: [
          {
            id: "9007199254743001",
            publicUrl: "http://localhost/media/cover.png",
            contentType: "image/png",
            fileSize: 1024,
            width: 800,
            height: 450,
            originalFilename: "cover.png",
            createdAt: "2026-06-25T12:00:00",
            createdBy: "1001"
          }
        ],
        total: 1,
        page: 1,
        size: 20
      }
    });
    mock.onPost("/api/admin/articles").reply(config => {
      expect(JSON.parse(config.data).coverAttachmentId).toBe(
        "9007199254743001"
      );
      return [200, { code: "00000", msg: "success", data: { id: "101" } }];
    });

    const wrapper = mount(ArticleEditor, { global: { stubs } });
    await flushPromises();
    await wrapper.get('[data-testid="article-cover-open-picker"]').trigger("click");
    await flushPromises();
    await wrapper
      .get('[data-testid="attachment-picker-select-9007199254743001"]')
      .trigger("click");

    Object.assign((wrapper.vm as any).form, {
      titleZh: "标题",
      summaryZh: "摘要",
      body: "正文"
    });
    await wrapper.get('[data-testid="article-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.post).toHaveLength(1);
  });

  it("clears a selected cover before saving", async () => {
    dictionaries();
    mock.onPost("/api/admin/articles").reply(config => {
      expect(JSON.parse(config.data).coverAttachmentId).toBeNull();
      return [200, { code: "00000", msg: "success", data: { id: "101" } }];
    });

    const wrapper = mount(ArticleEditor, { global: { stubs } });
    await flushPromises();
    Object.assign((wrapper.vm as any).form, {
      titleZh: "标题",
      summaryZh: "摘要",
      body: "正文",
      coverAttachmentId: "9007199254743001",
      coverUrl: "http://localhost/media/cover.png"
    });
    await nextTick();
    await wrapper.get('[data-testid="article-cover-clear"]').trigger("click");
    await wrapper.get('[data-testid="article-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.post).toHaveLength(1);
  });

  it("renders a safe markdown preview for the body", async () => {
    dictionaries();
    const wrapper = mount(ArticleEditor, { global: { stubs } });
    await flushPromises();

    Object.assign((wrapper.vm as any).form, {
      body: "# 标题\n\n<script>alert(1)</script>"
    });
    await nextTick();

    const preview = wrapper.get('[data-testid="article-markdown-preview"]');
    expect(preview.html()).toContain("<h1>标题</h1>");
    expect(preview.html()).not.toContain("<script>");
    expect(preview.text()).toContain("<script>alert(1)</script>");
  });
});
