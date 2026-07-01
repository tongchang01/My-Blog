import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import {
  createArticle,
  deleteArticle,
  getArticle,
  listArticles,
  listCategories,
  listDeletedArticles,
  listTags,
  restoreArticle,
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
      homepageSlot: "NONE",
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

  it("deletes, lists and restores articles through lifecycle endpoints", async () => {
    mock.onDelete("/api/admin/articles/9007199254740993").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });
    mock.onGet("/api/admin/articles/recycle-bin").reply(config => {
      expect(config.params).toEqual({ page: 2, size: 10 });
      return [
        200,
        {
          code: "00000",
          msg: "success",
          data: {
            records: [
              {
                id: "9007199254740993",
                titleZh: "已删除文章",
                titleJa: null,
                titleEn: null,
                status: "DRAFT",
                categoryId: "9007199254740992",
                deletedAt: "2026-06-22T12:00:00",
                deletedBy: "9007199254740991"
              }
            ],
            total: 1,
            page: 2,
            size: 10
          }
        }
      ];
    });
    mock.onPost("/api/admin/articles/9007199254740993/restore").reply(200, {
      code: "00000",
      msg: "success",
      data: { id: "9007199254740993" }
    });

    await expect(deleteArticle("9007199254740993")).resolves.toMatchObject({
      data: null
    });
    await expect(listDeletedArticles(2, 10)).resolves.toMatchObject({
      data: {
        records: [
          {
            id: "9007199254740993",
            categoryId: "9007199254740992",
            deletedBy: "9007199254740991"
          }
        ]
      }
    });
    await expect(restoreArticle("9007199254740993")).resolves.toMatchObject({
      data: { id: "9007199254740993" }
    });
  });
});
