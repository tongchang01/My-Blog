import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import ArticleList from "./index.vue";

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;
const stubs = {
  "el-alert": true,
  "el-button": true,
  "el-card": true,
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-input": true,
  "el-option": true,
  "el-pagination": true,
  "el-select": true,
  "el-skeleton": true,
  "el-table": true,
  "el-table-column": true,
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
  it("renders independent filter and result cards without write controls", async () => {
    replyDictionaries();
    mock.onGet("/api/admin/articles").reply(200, {
      code: "00000",
      msg: "success",
      data: { records: [], total: 0, page: 1, size: 20 }
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
    expect(wrapper.find('[data-testid="article-empty"]').exists()).toBe(true);
    expect(
      wrapper.find('[data-testid="article-operation-column"]').exists()
    ).toBe(false);
    expect(wrapper.text()).not.toMatch(/编辑|删除|Edit|Delete/);
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
