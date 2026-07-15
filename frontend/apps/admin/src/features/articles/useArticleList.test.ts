import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import type {
  ArticleListItem,
  CategoryItem,
  PageResponse,
  TagItem
} from "./model";
import { useArticleList, type ArticleListApi } from "./useArticleList";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function page(id: string, currentPage = 1): PageResponse<ArticleListItem> {
  return {
    records: [
      {
        id,
        titleZh: `标题 ${id}`,
        titleJa: null,
        titleEn: null,
        summaryZh: null,
        summaryJa: null,
        summaryEn: null,
        categoryId: null,
        categoryNameZh: null,
        slug: `article-${id}`,
        status: "DRAFT",
        homepageSlot: "NONE",
        publishAt: null,
        coverAttachmentId: null,
        coverUrl: null,
        commentCount: 0,
        tagIds: [],
        createdAt: "2026-06-21T09:00:00",
        createdBy: null,
        updatedAt: "2026-06-21T09:00:00",
        updatedBy: null
      }
    ],
    total: 1,
    page: currentPage,
    size: 20
  };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function api(overrides: Partial<ArticleListApi> = {}): ArticleListApi {
  return {
    listArticles: vi.fn().mockResolvedValue(ok(page("1"))),
    listCategories: vi.fn().mockResolvedValue(ok([] as CategoryItem[])),
    listTags: vi.fn().mockResolvedValue(ok([] as TagItem[])),
    deleteArticle: vi.fn().mockResolvedValue(ok(null)),
    ...overrides
  };
}

describe("article list state", () => {
  it("starts article, category and tag requests together", async () => {
    const articles = deferred<ApiResponse<PageResponse<ArticleListItem>>>();
    const categories = deferred<ApiResponse<CategoryItem[]>>();
    const tags = deferred<ApiResponse<TagItem[]>>();
    const source = api({
      listArticles: vi.fn(() => articles.promise),
      listCategories: vi.fn(() => categories.promise),
      listTags: vi.fn(() => tags.promise)
    });
    const state = useArticleList(source);

    const initialization = state.initialize();

    expect(source.listArticles).toHaveBeenCalledOnce();
    expect(source.listCategories).toHaveBeenCalledOnce();
    expect(source.listTags).toHaveBeenCalledOnce();
    articles.resolve(ok(page("1")));
    categories.resolve(ok([]));
    tags.resolve(ok([]));
    await initialization;
  });

  it("searches from page one, resets defaults and refreshes current filters", async () => {
    const source = api();
    const state = useArticleList(source);
    state.filters.titleKeyword = "Vue";
    state.filters.status = "PUBLISHED";
    state.filters.categoryId = "100";
    state.filters.tagId = "200";
    state.filters.createdFrom = "2026-07-01T00:00:00";
    state.filters.createdTo = "2026-07-02T00:00:00";
    state.filters.publishFrom = "2026-07-03T00:00:00";
    state.filters.publishTo = "2026-07-04T00:00:00";
    state.filters.page = 3;

    await state.search();
    expect(source.listArticles).toHaveBeenLastCalledWith(
      expect.objectContaining({
        page: 1,
        titleKeyword: "Vue",
        categoryId: "100",
        tagId: "200",
        createdFrom: "2026-07-01T00:00:00",
        createdTo: "2026-07-02T00:00:00",
        publishFrom: "2026-07-03T00:00:00",
        publishTo: "2026-07-04T00:00:00"
      })
    );

    state.filters.page = 2;
    await state.refresh();
    expect(source.listArticles).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 2, titleKeyword: "Vue" })
    );

    await state.reset();
    expect(state.filters).toMatchObject({
      titleKeyword: "",
      status: "ALL",
      categoryId: "",
      tagId: "",
      createdFrom: "",
      createdTo: "",
      publishFrom: "",
      publishTo: "",
      page: 1,
      size: 20
    });
  });

  it("keeps the list usable when dictionaries fail", async () => {
    const source = api({
      listCategories: vi.fn().mockRejectedValue(new Error("categories")),
      listTags: vi.fn().mockRejectedValue(new Error("tags"))
    });
    const state = useArticleList(source);

    await state.initialize();

    expect(state.items.value[0].id).toBe("1");
    expect(state.categories.value).toEqual([]);
    expect(state.tags.value).toEqual([]);
    expect(state.error.value).toBeNull();
  });

  it("exposes article request failure for retry", async () => {
    const source = api({
      listArticles: vi.fn().mockRejectedValue(new Error("offline"))
    });
    const state = useArticleList(source);

    await state.refresh();

    expect(state.error.value?.message).toBe("offline");
    expect(state.loading.value).toBe(false);
  });

  it("does not send an invalid date range", async () => {
    const source = api();
    const state = useArticleList(source);
    state.filters.createdFrom = "2026-07-02T00:00:00";
    state.filters.createdTo = "2026-07-01T00:00:00";

    await state.search();

    expect(source.listArticles).not.toHaveBeenCalled();
    expect(state.filterError.value).toBe(true);
  });

  it("keeps the latest response when an older request finishes last", async () => {
    const first = deferred<ApiResponse<PageResponse<ArticleListItem>>>();
    const second = deferred<ApiResponse<PageResponse<ArticleListItem>>>();
    const listArticles = vi
      .fn()
      .mockImplementationOnce(() => first.promise)
      .mockImplementationOnce(() => second.promise);
    const state = useArticleList(api({ listArticles }));

    const older = state.refresh();
    const newer = state.refresh();
    second.resolve(ok(page("new")));
    await newer;
    first.resolve(ok(page("old")));
    await older;

    expect(state.items.value[0].id).toBe("new");
  });

  it("deletes one article and refreshes the current page", async () => {
    const source = api();
    const state = useArticleList(source);
    await state.refresh();

    await expect(state.remove("100")).resolves.toBe(true);

    expect(source.deleteArticle).toHaveBeenCalledWith("100");
    expect(source.listArticles).toHaveBeenCalledTimes(2);
    expect(state.deletingId.value).toBeNull();
    expect(state.operationError.value).toBeNull();
  });

  it("returns to the previous page when deletion empties the last page", async () => {
    const listArticles = vi
      .fn()
      .mockResolvedValueOnce(ok({ ...page("21", 2), total: 21 }))
      .mockResolvedValueOnce(
        ok({ records: [], total: 20, page: 2, size: 20 })
      )
      .mockResolvedValueOnce(ok({ ...page("20"), total: 20 }));
    const source = api({ listArticles });
    const state = useArticleList(source);
    state.filters.page = 2;
    await state.refresh();

    await expect(state.remove("21")).resolves.toBe(true);

    expect(state.filters.page).toBe(1);
    expect(state.items.value[0].id).toBe("20");
    expect(listArticles).toHaveBeenCalledTimes(3);
  });

  it("keeps current data and exposes a deletion error", async () => {
    const source = api({
      deleteArticle: vi.fn().mockRejectedValue(new Error("delete failed"))
    });
    const state = useArticleList(source);
    await state.refresh();

    await expect(state.remove("1")).resolves.toBe(false);

    expect(state.items.value[0].id).toBe("1");
    expect(state.operationError.value?.message).toBe("delete failed");
    expect(source.listArticles).toHaveBeenCalledOnce();
  });
});
