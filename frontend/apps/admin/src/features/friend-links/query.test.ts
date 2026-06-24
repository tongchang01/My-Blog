import { describe, expect, it } from "vitest";
import { buildFriendLinkListParams } from "./query";

describe("friend link list query", () => {
  it("trims keyword and keeps explicit status filter", () => {
    expect(
      buildFriendLinkListParams({
        keyword: "  example  ",
        status: "VISIBLE",
        page: 2,
        size: 50
      })
    ).toEqual({
      keyword: "example",
      status: "VISIBLE",
      page: 2,
      size: 50
    });
  });

  it("omits empty keyword and ALL status while preserving pagination", () => {
    expect(
      buildFriendLinkListParams({
        keyword: "   ",
        status: "ALL",
        page: 1,
        size: 20
      })
    ).toEqual({
      page: 1,
      size: 20
    });
  });
});
