import { describe, expect, it } from "vitest";
import { buildArticleListParams } from "./query";

describe("article list query", () => {
  it("trims the title and preserves the backend one-based page", () => {
    expect(
      buildArticleListParams({
        titleKeyword: "  Vue 3  ",
        status: "PUBLISHED",
        page: 2,
        size: 20
      })
    ).toEqual({
      titleKeyword: "Vue 3",
      status: "PUBLISHED",
      page: 2,
      size: 20
    });
  });

  it("omits empty title and all-status filters", () => {
    expect(
      buildArticleListParams({
        titleKeyword: "   ",
        status: "ALL",
        page: 1,
        size: 10
      })
    ).toEqual({ page: 1, size: 10 });
  });
});
