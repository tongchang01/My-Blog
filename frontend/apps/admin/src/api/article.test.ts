import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import {
  createArticle,
  getArticle,
  listArticles,
  listCategories,
  listTags,
  updateArticle
} from "./article";
import type { ArticleWritePayload } from "@/features/articles/model";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

describe("article API", () => {
  it("requests the admin article page with normalized params", async () => {
    mock.onGet("/api/admin/articles").reply(config => {
      expect(config.params).toEqual({
        titleKeyword: "Vue",
        status: "PUBLISHED",
        page: 1,
        size: 20
      });
      return [
        200,
        {
          code: "00000",
          msg: "success",
          data: { records: [], total: 0, page: 1, size: 20 }
        }
      ];
    });

    await expect(
      listArticles({
        titleKeyword: " Vue ",
        status: "PUBLISHED",
        page: 1,
        size: 20
      })
    ).resolves.toMatchObject({ code: "00000" });
  });

  it("requests category and tag dictionaries", async () => {
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

    await expect(Promise.all([listCategories(), listTags()])).resolves.toEqual([
      { code: "00000", msg: "success", data: [] },
      { code: "00000", msg: "success", data: [] }
    ]);
  });

  it("requests article detail and submits complete writes", async () => {
    const payload: ArticleWritePayload = {
      titleZh: "标题",
      titleJa: null,
      titleEn: null,
      summaryZh: "摘要",
      summaryJa: null,
      summaryEn: null,
      body: "# 正文",
      categoryId: "10",
      tagIds: ["20"],
      slug: "hello",
      status: "DRAFT",
      password: null,
      publishAt: null,
      coverAttachmentId: null
    };
    mock.onGet("/api/admin/articles/100").reply(200, {
      code: "00000",
      msg: "success",
      data: { id: "100", ...payload }
    });
    mock.onPost("/api/admin/articles").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, { code: "00000", msg: "success", data: { id: "101" } }];
    });
    mock.onPut("/api/admin/articles/100").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, { code: "00000", msg: "success", data: { id: "100" } }];
    });

    await expect(getArticle("100")).resolves.toMatchObject({
      data: { id: "100" }
    });
    await expect(createArticle(payload)).resolves.toMatchObject({
      data: { id: "101" }
    });
    await expect(updateArticle("100", payload)).resolves.toMatchObject({
      data: { id: "100" }
    });
  });
});
