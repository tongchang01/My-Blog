import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import { ApiClientError } from "@/utils/http/error";
import type {
  ArticleDetail,
  CategoryItem,
  DeletedArticleListItem,
  PageResponse
} from "../model";
import {
  useArticleRecycleBin,
  type ArticleRecycleBinApi
} from "./useArticleRecycleBin";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function deletedPage(
  id: string,
  page = 1,
  total = 1
): PageResponse<DeletedArticleListItem> {
  return {
    records: [
      {
        id,
        titleZh: `已删除文章 ${id}`,
        titleJa: null,
        titleEn: null,
        status: "DRAFT",
        categoryId: "10",
        deletedAt: "2026-06-22T12:00:00",
        deletedBy: "1001"
      }
    ],
    total,
    page,
    size: 20
  };
}

function api(overrides: Partial<ArticleRecycleBinApi> = {}): ArticleRecycleBinApi {
  return {
    listDeletedArticles: vi.fn().mockResolvedValue(ok(deletedPage("1"))),
    listCategories: vi.fn().mockResolvedValue(ok([] as CategoryItem[])),
    restoreArticle: vi.fn().mockResolvedValue(
      ok({ id: "1" } as ArticleDetail)
    ),
    ...overrides
  };
}

describe("article recycle bin state", () => {
  it("loads deleted articles and categories together", async () => {
    const source = api();
    const state = useArticleRecycleBin(source);

    await state.initialize();

    expect(source.listDeletedArticles).toHaveBeenCalledWith(1, 20);
    expect(source.listCategories).toHaveBeenCalledOnce();
    expect(state.items.value[0].id).toBe("1");
  });

  it("keeps the recycle bin usable when categories fail", async () => {
    const state = useArticleRecycleBin(
      api({ listCategories: vi.fn().mockRejectedValue(new Error("offline")) })
    );

    await state.initialize();

    expect(state.items.value[0].id).toBe("1");
    expect(state.categories.value).toEqual([]);
    expect(state.error.value).toBeNull();
  });

  it("changes page and refreshes the current page", async () => {
    const source = api();
    const state = useArticleRecycleBin(source);

    await state.changePage(2, 10);
    expect(source.listDeletedArticles).toHaveBeenLastCalledWith(2, 10);
    await state.refresh();
    expect(source.listDeletedArticles).toHaveBeenLastCalledWith(2, 10);
  });

  it("exposes a list failure and retries it", async () => {
    const listDeletedArticles = vi
      .fn()
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce(ok(deletedPage("1")));
    const state = useArticleRecycleBin(api({ listDeletedArticles }));

    await state.refresh();
    expect(state.error.value?.message).toBe("offline");

    await state.retry();
    expect(state.error.value).toBeNull();
    expect(state.items.value[0].id).toBe("1");
  });

  it("restores an article and returns from an emptied tail page", async () => {
    const listDeletedArticles = vi
      .fn()
      .mockResolvedValueOnce(ok(deletedPage("21", 2, 21)))
      .mockResolvedValueOnce(
        ok({ records: [], total: 20, page: 2, size: 20 })
      )
      .mockResolvedValueOnce(ok(deletedPage("20", 1, 20)));
    const source = api({ listDeletedArticles });
    const state = useArticleRecycleBin(source);
    state.page.value = 2;
    await state.refresh();

    await expect(state.restore("21")).resolves.toBe(true);

    expect(source.restoreArticle).toHaveBeenCalledWith("21");
    expect(state.page.value).toBe(1);
    expect(state.items.value[0].id).toBe("20");
  });

  it("classifies restore reference conflicts without dropping current data", async () => {
    const source = api({
      restoreArticle: vi
        .fn()
        .mockRejectedValue(new ApiClientError("conflict", "90004", 409))
    });
    const state = useArticleRecycleBin(source);
    await state.refresh();

    await expect(state.restore("1")).resolves.toBe(false);

    expect(state.items.value[0].id).toBe("1");
    expect(state.operationError.value?.kind).toBe("conflict");
    expect(state.restoringId.value).toBeNull();
  });
});
