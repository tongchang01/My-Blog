import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import { ApiClientError } from "@/utils/http/error";
import type {
  FriendLinkItem,
  FriendLinkPageResponse
} from "./model";
import {
  type FriendLinkManagementApi,
  useFriendLinkManagement
} from "./useFriendLinkManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function friendLink(
  id: string,
  name = "Example",
  sortOrder = 10,
  status: FriendLinkItem["status"] = "VISIBLE"
): FriendLinkItem {
  return {
    id,
    name,
    url: `https://example.com/${id}`,
    avatarUrl: null,
    description: `${name} description`,
    sortOrder,
    status,
    createdAt: "2026-06-24T10:00:00",
    createdBy: "1001",
    updatedAt: "2026-06-24T11:00:00",
    updatedBy: "1001"
  };
}

function page(
  records: FriendLinkItem[] = [friendLink("9007199254742501")],
  currentPage = 1,
  total = records.length
): FriendLinkPageResponse {
  return {
    records,
    total,
    page: currentPage,
    size: 20
  };
}

function api(
  overrides: Partial<FriendLinkManagementApi> = {}
): FriendLinkManagementApi {
  return {
    listFriendLinks: vi.fn().mockResolvedValue(ok(page())),
    createFriendLink: vi
      .fn()
      .mockResolvedValue(ok(friendLink("9007199254742503"))),
    updateFriendLink: vi
      .fn()
      .mockResolvedValue(ok(friendLink("9007199254742501"))),
    updateFriendLinkStatus: vi
      .fn()
      .mockResolvedValue(ok(friendLink("9007199254742501", "Example", 10, "HIDDEN"))),
    updateFriendLinkSortOrders: vi.fn().mockResolvedValue(ok([])),
    deleteFriendLink: vi.fn().mockResolvedValue(ok(null)),
    ...overrides
  };
}

describe("friend link management state", () => {
  it("loads the first page with pagination only", async () => {
    const source = api();
    const state = useFriendLinkManagement(source);

    await state.initialize();

    expect(source.listFriendLinks).toHaveBeenCalledWith({
      page: 1,
      size: 20
    });
    expect(state.items.value[0].id).toBe("9007199254742501");
    expect(state.total.value).toBe(1);
    expect(state.sortDrafts["9007199254742501"]).toBe(10);
  });

  it("changes the page without inventing unsupported filters", async () => {
    const source = api();
    const state = useFriendLinkManagement(source);

    await state.changePage(2, 50);
    expect(source.listFriendLinks).toHaveBeenLastCalledWith({
      page: 2,
      size: 50
    });
  });

  it("opens create and edit forms then refreshes after save", async () => {
    const source = api();
    const state = useFriendLinkManagement(source);
    await state.initialize();

    state.openCreate();
    Object.assign(state.form, {
      name: "New Site",
      url: "https://new.example.com",
      sortOrder: 30
    });
    await expect(state.save()).resolves.toBe(true);
    expect(source.createFriendLink).toHaveBeenCalledWith({
      name: "New Site",
      url: "https://new.example.com",
      avatarUrl: null,
      description: null,
      sortOrder: 30,
      status: "VISIBLE"
    });

    state.openEdit(friendLink("9007199254742501"));
    state.form.name = "Updated";
    await expect(state.save()).resolves.toBe(true);
    expect(source.updateFriendLink).toHaveBeenCalledWith(
      "9007199254742501",
      expect.objectContaining({ name: "Updated" })
    );
    expect(source.listFriendLinks).toHaveBeenCalledTimes(3);
  });

  it("toggles status, saves only dirty sort values and removes records", async () => {
    const source = api();
    const state = useFriendLinkManagement(source);
    await state.initialize();

    await expect(
      state.updateStatus("9007199254742501", "HIDDEN")
    ).resolves.toBe(true);
    expect(source.updateFriendLinkStatus).toHaveBeenCalledWith(
      "9007199254742501",
      "HIDDEN"
    );

    state.setSortOrder("9007199254742501", 30);
    expect(state.dirtySortItems.value).toEqual([
      { id: "9007199254742501", sortOrder: 30 }
    ]);
    await expect(state.saveSortOrders()).resolves.toBe(true);
    expect(source.updateFriendLinkSortOrders).toHaveBeenCalledWith([
      { id: "9007199254742501", sortOrder: 30 }
    ]);

    await expect(state.remove("9007199254742501")).resolves.toBe(true);
    expect(source.deleteFriendLink).toHaveBeenCalledWith(
      "9007199254742501"
    );
  });

  it("keeps current data and exposes operation errors", async () => {
    const conflict = new ApiClientError("conflict", "90004", 409);
    const source = api({
      updateFriendLinkStatus: vi.fn().mockRejectedValue(conflict)
    });
    const state = useFriendLinkManagement(source);
    await state.initialize();

    await expect(
      state.updateStatus("9007199254742501", "HIDDEN")
    ).resolves.toBe(false);

    expect(state.items.value[0].id).toBe("9007199254742501");
    expect(state.operationError.value).toEqual({
      action: "status",
      kind: "conflict"
    });
  });

  it("returns to previous page when deletion empties the last page", async () => {
    const listFriendLinks = vi
      .fn()
      .mockResolvedValueOnce(ok(page([friendLink("21")], 2, 21)))
      .mockResolvedValueOnce(ok(page([], 2, 20)))
      .mockResolvedValueOnce(ok(page([friendLink("20")], 1, 20)));
    const source = api({ listFriendLinks });
    const state = useFriendLinkManagement(source);
    state.filters.page = 2;
    await state.refresh();

    await expect(state.remove("21")).resolves.toBe(true);

    expect(state.filters.page).toBe(1);
    expect(state.items.value[0].id).toBe("20");
    expect(listFriendLinks).toHaveBeenCalledTimes(3);
  });
});
