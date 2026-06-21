import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import type {
  ArticleDetail,
  CategoryItem,
  TagItem
} from "../model";
import {
  useArticleEditor,
  type ArticleEditorApi
} from "./useArticleEditor";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

const detail = {
  id: "100",
  titleZh: "标题",
  titleJa: null,
  titleEn: null,
  summaryZh: "摘要",
  summaryJa: null,
  summaryEn: null,
  body: "正文",
  categoryId: null,
  categoryNameZh: null,
  authorId: "1001",
  slug: "article-100",
  status: "DRAFT",
  publishAt: null,
  coverAttachmentId: null,
  coverUrl: null,
  commentCount: 0,
  tagIds: [],
  createdAt: "2026-06-20T10:00:00",
  createdBy: "1001",
  updatedAt: "2026-06-21T10:00:00",
  updatedBy: "1001"
} satisfies ArticleDetail;

function api(): ArticleEditorApi {
  return {
    getArticle: vi.fn().mockResolvedValue(ok(detail)),
    createArticle: vi.fn().mockResolvedValue(ok({ ...detail, id: "101" })),
    updateArticle: vi.fn().mockResolvedValue(ok(detail)),
    listCategories: vi.fn().mockResolvedValue(ok([] as CategoryItem[])),
    listTags: vi.fn().mockResolvedValue(ok([] as TagItem[]))
  };
}

describe("article editor state", () => {
  it("loads dictionaries and detail in edit mode", async () => {
    const source = api();
    const state = useArticleEditor("edit", "100", source);

    await state.initialize();

    expect(source.getArticle).toHaveBeenCalledWith("100");
    expect(state.form.titleZh).toBe("标题");
    expect(state.loading.value).toBe(false);
  });

  it("validates before create and submits normalized payload", async () => {
    const source = api();
    const state = useArticleEditor("create", undefined, source);

    await expect(state.save()).resolves.toBeNull();
    expect(source.createArticle).not.toHaveBeenCalled();
    expect(state.errors.value.titleZh).toBe("required");

    Object.assign(state.form, {
      titleZh: " 标题 ",
      summaryZh: " 摘要 ",
      body: " 正文 "
    });
    await expect(state.save()).resolves.toMatchObject({ id: "101" });
    expect(source.createArticle).toHaveBeenCalledWith(
      expect.objectContaining({
        titleZh: "标题",
        summaryZh: "摘要",
        body: "正文"
      })
    );
  });

  it("updates an existing article and keeps errors visible on failure", async () => {
    const source = api();
    const state = useArticleEditor("edit", "100", source);
    await state.initialize();
    await state.save();
    expect(source.updateArticle).toHaveBeenCalledWith(
      "100",
      expect.objectContaining({ titleZh: "标题" })
    );

    source.updateArticle = vi.fn().mockRejectedValue(new Error("offline"));
    await expect(state.save()).rejects.toThrow("offline");
    expect(state.requestError.value?.message).toBe("offline");
    expect(state.form.titleZh).toBe("标题");
  });
});
