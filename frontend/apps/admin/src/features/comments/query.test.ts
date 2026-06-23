import { describe, expect, it } from "vitest";
import { buildCommentListParams } from "./query";

describe("comment list query", () => {
  it("trims keyword and keeps explicit filters", () => {
    expect(
      buildCommentListParams({
        targetType: "ARTICLE",
        targetId: "9007199254740993",
        auditStatus: "PENDING",
        keyword: "  hello  ",
        includeDeleted: true,
        page: 2,
        size: 50
      })
    ).toEqual({
      targetType: "ARTICLE",
      targetId: "9007199254740993",
      auditStatus: "PENDING",
      keyword: "hello",
      includeDeleted: true,
      page: 2,
      size: 50
    });
  });

  it("omits empty and all filters while preserving pagination", () => {
    expect(
      buildCommentListParams({
        targetType: "ALL",
        targetId: "   ",
        auditStatus: "ALL",
        keyword: "   ",
        includeDeleted: false,
        page: 1,
        size: 20
      })
    ).toEqual({
      includeDeleted: false,
      page: 1,
      size: 20
    });
  });
});
