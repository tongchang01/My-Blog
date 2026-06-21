import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import { ApiClientError } from "@/utils/http/error";
import type { CategoryItem } from "../model";
import {
  type CategoryManagementApi,
  useCategoryManagement
} from "./useCategoryManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function category(
  id: string,
  nameZh: string,
  slug: string,
  sortOrder: number,
  nameEn: string | null = null
): CategoryItem {
  return {
    id,
    nameZh,
    nameJa: null,
    nameEn,
    slug,
    sortOrder,
    createdAt: "2026-06-20T10:00:00",
    createdBy: "1001",
    updatedAt: "2026-06-21T10:00:00",
    updatedBy: "1001"
  };
}

function api(
  overrides: Partial<CategoryManagementApi> = {}
): CategoryManagementApi {
  return {
    listCategories: vi
      .fn()
      .mockResolvedValue(
        ok([
          category("100", "后端", "backend", 20, "Backend"),
          category("101", "前端", "frontend", 10, "Frontend")
        ])
      ),
    createCategory: vi.fn().mockResolvedValue(ok(category("102", "随笔", "essay", 30))),
    updateCategory: vi.fn().mockResolvedValue(ok(category("100", "服务端", "backend", 20))),
    updateCategorySortOrders: vi.fn().mockResolvedValue(ok(null)),
    deleteCategory: vi.fn().mockResolvedValue(ok(null)),
    ...overrides
  };
}

describe("category management state", () => {
  it("loads categories and filters all names plus slug locally", async () => {
    const state = useCategoryManagement(api());
    await state.initialize();

    state.keyword.value = "backend";
    expect(state.filteredItems.value.map(item => item.id)).toEqual(["100"]);
    state.keyword.value = "前端";
    expect(state.filteredItems.value.map(item => item.id)).toEqual(["101"]);
  });

  it("opens create and edit forms then refreshes after save", async () => {
    const source = api();
    const state = useCategoryManagement(source);
    await state.initialize();

    state.openCreate();
    expect(state.editingId.value).toBeNull();
    Object.assign(state.form, {
      nameZh: "随笔",
      slug: "essay",
      sortOrder: 30
    });
    await expect(state.save()).resolves.toBe(true);
    expect(source.createCategory).toHaveBeenCalledWith({
      nameZh: "随笔",
      nameJa: null,
      nameEn: null,
      slug: "essay",
      sortOrder: 30
    });

    state.openEdit(category("100", "后端", "backend", 20));
    state.form.nameZh = "服务端";
    await expect(state.save()).resolves.toBe(true);
    expect(source.updateCategory).toHaveBeenCalledWith(
      "100",
      expect.objectContaining({ nameZh: "服务端" })
    );
    expect(source.listCategories).toHaveBeenCalledTimes(3);
  });

  it("collects only dirty sort values and clears them after save", async () => {
    const source = api();
    const state = useCategoryManagement(source);
    await state.initialize();

    state.setSortOrder("100", 30);
    expect(state.dirtySortItems.value).toEqual([
      { id: "100", sortOrder: 30 }
    ]);
    await expect(state.saveSortOrders()).resolves.toBe(true);
    expect(source.updateCategorySortOrders).toHaveBeenCalledWith([
      { id: "100", sortOrder: 30 }
    ]);
    expect(state.dirtySortItems.value).toEqual([]);
  });

  it("keeps input and exposes save or delete conflicts", async () => {
    const conflict = new ApiClientError("conflict", "90004", 409);
    const source = api({
      createCategory: vi.fn().mockRejectedValue(conflict),
      deleteCategory: vi.fn().mockRejectedValue(conflict)
    });
    const state = useCategoryManagement(source);
    state.openCreate();
    Object.assign(state.form, {
      nameZh: "冲突分类",
      slug: "backend",
      sortOrder: 0
    });

    await expect(state.save()).resolves.toBe(false);
    expect(state.operationError.value).toEqual({
      action: "save",
      kind: "conflict"
    });
    expect(state.form.nameZh).toBe("冲突分类");

    await expect(state.remove("100")).resolves.toBe(false);
    expect(state.operationError.value).toEqual({
      action: "delete",
      kind: "conflict"
    });
  });
});
