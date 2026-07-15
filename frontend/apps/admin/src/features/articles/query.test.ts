import { describe, expect, it } from "vitest";
import { buildArticleListParams } from "./query";

describe("article list query", () => {
  it("trims the title and preserves the backend one-based page", () => {
    expect(
      buildArticleListParams({
        titleKeyword: "  Vue 3  ",
        status: "PUBLISHED",
        categoryId: "100",
        tagId: "200",
        createdFrom: "2026-07-01T00:00:00",
        createdTo: "2026-07-02T23:59:59",
        publishFrom: "2026-07-03T00:00:00",
        publishTo: "2026-07-04T23:59:59",
        page: 2,
        size: 20
      })
    ).toEqual({
      titleKeyword: "Vue 3",
      status: "PUBLISHED",
      categoryId: "100",
      tagId: "200",
      createdFrom: "2026-07-01T00:00:00",
      createdTo: "2026-07-02T23:59:59",
      publishFrom: "2026-07-03T00:00:00",
      publishTo: "2026-07-04T23:59:59",
      page: 2,
      size: 20
    });
  });

  it("omits empty title and all-status filters", () => {
    expect(
      buildArticleListParams({
        titleKeyword: "   ",
        status: "ALL",
        categoryId: "",
        tagId: "",
        createdFrom: "",
        createdTo: "",
        publishFrom: "",
        publishTo: "",
        page: 1,
        size: 10
      })
    ).toEqual({ page: 1, size: 10 });
  });
});
