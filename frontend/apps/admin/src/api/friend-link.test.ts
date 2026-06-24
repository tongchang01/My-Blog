import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import type { FriendLinkWritePayload } from "@/features/friend-links/model";
import { http } from "@/utils/http";
import {
  createFriendLink,
  deleteFriendLink,
  getFriendLink,
  listFriendLinks,
  updateFriendLink,
  updateFriendLinkSortOrders,
  updateFriendLinkStatus
} from "./friend-link";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

const ok = (data: unknown = null) => ({
  code: "00000",
  msg: "success",
  data
});

describe("friend link API", () => {
  it("requests list, detail and complete writes", async () => {
    const payload: FriendLinkWritePayload = {
      name: "Example",
      url: "https://example.com",
      avatarUrl: null,
      description: null,
      sortOrder: 10,
      status: "VISIBLE"
    };
    mock.onGet("/api/admin/friend-links").reply(config => {
      expect(config.params).toEqual({ page: 2, size: 20 });
      return [200, ok({ records: [], total: 0, page: 2, size: 20 })];
    });
    mock
      .onGet("/api/admin/friend-links/9007199254742501")
      .reply(200, ok({ id: "9007199254742501" }));
    mock.onPost("/api/admin/friend-links").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok({ id: "9007199254742502" })];
    });
    mock
      .onPut("/api/admin/friend-links/9007199254742501")
      .reply(config => {
        expect(JSON.parse(config.data)).toEqual(payload);
        return [200, ok({ id: "9007199254742501" })];
      });
    mock
      .onPatch("/api/admin/friend-links/9007199254742501/status")
      .reply(config => {
        expect(JSON.parse(config.data)).toEqual({ status: "HIDDEN" });
        return [200, ok({ id: "9007199254742501", status: "HIDDEN" })];
      });
    mock.onPut("/api/admin/friend-links/sort-orders").reply(config => {
      expect(JSON.parse(config.data)).toEqual({
        items: [{ id: "9007199254742501", sortOrder: 30 }]
      });
      return [200, ok([{ id: "9007199254742501", sortOrder: 30 }])];
    });
    mock
      .onDelete("/api/admin/friend-links/9007199254742501")
      .reply(200, ok());

    await expect(
      listFriendLinks({ keyword: "", status: "ALL", page: 2, size: 20 })
    ).resolves.toMatchObject({ data: { page: 2 } });
    await expect(getFriendLink("9007199254742501")).resolves.toMatchObject({
      data: { id: "9007199254742501" }
    });
    await expect(createFriendLink(payload)).resolves.toMatchObject({
      data: { id: "9007199254742502" }
    });
    await expect(
      updateFriendLink("9007199254742501", payload)
    ).resolves.toMatchObject({ data: { id: "9007199254742501" } });
    await expect(
      updateFriendLinkStatus("9007199254742501", "HIDDEN")
    ).resolves.toMatchObject({ data: { status: "HIDDEN" } });
    await expect(
      updateFriendLinkSortOrders([
        { id: "9007199254742501", sortOrder: 30 }
      ])
    ).resolves.toMatchObject({ data: [{ id: "9007199254742501" }] });
    await expect(deleteFriendLink("9007199254742501")).resolves.toMatchObject({
      code: "00000"
    });
  });
});
