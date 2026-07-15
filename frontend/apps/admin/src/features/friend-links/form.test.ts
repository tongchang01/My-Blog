import { describe, expect, it } from "vitest";
import type { FriendLinkItem } from "./model";
import {
  createFriendLinkForm,
  friendLinkFormToPayload,
  friendLinkToForm,
  validateFriendLinkForm
} from "./form";

const item: FriendLinkItem = {
  id: "9007199254742501",
  name: "Example",
  url: "https://example.com",
  avatarUrl: null,
  description: "A site",
  sortOrder: 10,
  status: "VISIBLE",
  createdAt: "2026-06-24T10:00:00",
  createdBy: "1001",
  updatedAt: "2026-06-24T11:00:00",
  updatedBy: "1001"
};

describe("friend link form", () => {
  it("creates defaults and maps existing item without losing string id", () => {
    expect(createFriendLinkForm()).toEqual({
      name: "",
      url: "",
      avatarUrl: "",
      description: "",
      sortOrder: 0,
      status: "VISIBLE"
    });
    expect(friendLinkToForm(item)).toEqual({
      name: "Example",
      url: "https://example.com",
      avatarUrl: "",
      description: "A site",
      sortOrder: 10,
      status: "VISIBLE"
    });
    expect(item.id).toBe("9007199254742501");
  });

  it("validates required fields, URL, length and sort range", () => {
    expect(validateFriendLinkForm(createFriendLinkForm())).toEqual({
      name: "required",
      url: "required"
    });
    expect(
      validateFriendLinkForm({
        ...createFriendLinkForm(),
        name: "Example",
        url: "not-a-url",
        sortOrder: -1
      })
    ).toEqual({
      url: "url",
      sortOrder: "sortOrderRange"
    });
    expect(
      validateFriendLinkForm({
        ...createFriendLinkForm(),
        name: "Example",
        url: "https://user:password@example.com",
        description: "x".repeat(256)
      })
    ).toEqual({ url: "url", description: "maxLength" });
  });

  it("normalizes whitespace and keeps nullable fields in complete payload", () => {
    expect(
      friendLinkFormToPayload({
        name: " Example ",
        url: " https://example.com ",
        avatarUrl: " ",
        description: " Description ",
        sortOrder: 20,
        status: "HIDDEN"
      })
    ).toEqual({
      name: "Example",
      url: "https://example.com",
      avatarUrl: null,
      description: "Description",
      sortOrder: 20,
      status: "HIDDEN"
    });
  });
});
