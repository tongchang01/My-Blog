import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import { ApiClientError } from "@/utils/http/error";
import type { TagItem } from "../model";
import { type TagManagementApi, useTagManagement } from "./useTagManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function tag(
  id: string,
  nameZh: string,
  slug: string,
  nameEn: string | null = null
): TagItem {
  return {
    id,
    nameZh,
    nameJa: null,
    nameEn,
    slug,
    createdAt: "2026-06-20T10:00:00",
    createdBy: "1001",
    updatedAt: "2026-06-21T10:00:00",
    updatedBy: "1001"
  };
}

function api(overrides: Partial<TagManagementApi> = {}): TagManagementApi {
  return {
    listTags: vi
      .fn()
      .mockResolvedValue(
        ok([tag("200", "Vue", "vue", "Vue"), tag("201", "数据库", "database", "Database")])
      ),
    createTag: vi.fn().mockResolvedValue(ok(tag("202", "测试", "test"))),
    updateTag: vi.fn().mockResolvedValue(ok(tag("200", "Vue.js", "vue"))),
    deleteTag: vi.fn().mockResolvedValue(ok(null)),
    ...overrides
  };
}

describe("tag management state", () => {
  it("loads and filters tags by all names or slug", async () => {
    const state = useTagManagement(api());
    await state.initialize();
    state.keyword.value = "database";
    expect(state.filteredItems.value.map(item => item.id)).toEqual(["201"]);
    state.keyword.value = "Vue";
    expect(state.filteredItems.value.map(item => item.id)).toEqual(["200"]);
  });

  it("creates and edits tags then reloads the list", async () => {
    const source = api();
    const state = useTagManagement(source);
    await state.initialize();
    state.openCreate();
    Object.assign(state.form, { nameZh: "测试", slug: "test" });
    await expect(state.save()).resolves.toBe(true);
    expect(source.createTag).toHaveBeenCalledWith({
      nameZh: "测试",
      nameJa: null,
      nameEn: null,
      slug: "test"
    });

    state.openEdit(tag("200", "Vue", "vue"));
    state.form.nameZh = "Vue.js";
    await expect(state.save()).resolves.toBe(true);
    expect(source.updateTag).toHaveBeenCalledWith(
      "200",
      expect.objectContaining({ nameZh: "Vue.js" })
    );
    expect(source.listTags).toHaveBeenCalledTimes(3);
  });

  it("preserves input and distinguishes save from delete conflicts", async () => {
    const conflict = new ApiClientError("conflict", "90004", 409);
    const state = useTagManagement(
      api({
        createTag: vi.fn().mockRejectedValue(conflict),
        deleteTag: vi.fn().mockRejectedValue(conflict)
      })
    );
    state.openCreate();
    Object.assign(state.form, { nameZh: "冲突标签", slug: "vue" });

    await expect(state.save()).resolves.toBe(false);
    expect(state.operationError.value).toEqual({
      action: "save",
      kind: "conflict"
    });
    expect(state.form.nameZh).toBe("冲突标签");

    await expect(state.remove("200")).resolves.toBe(false);
    expect(state.operationError.value).toEqual({
      action: "delete",
      kind: "conflict"
    });
  });
});
