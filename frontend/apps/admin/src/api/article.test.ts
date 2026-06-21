import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import { listArticles, listCategories, listTags } from "./article";

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
});
