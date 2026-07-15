import { describe, expect, it } from "vitest";
import type { ArticleDetail } from "../model";
import {
  articleDetailToForm,
  articleFormToPayload,
  createEmptyArticleForm,
  validateArticleForm
} from "./form";

const detail: ArticleDetail = {
  id: "100",
  titleZh: "中文标题",
  titleJa: null,
  titleEn: "English title",
  summaryZh: "中文摘要",
  summaryJa: null,
  summaryEn: null,
  body: "# 正文",
  categoryId: "10",
  categoryNameZh: "分类",
  authorId: "1001",
  slug: "hello-world",
  status: "PASSWORD",
  homepageSlot: "FEATURED",
  publishAt: "2026-06-21T10:00:00",
  coverAttachmentId: "30",
  coverUrl: "http://localhost/media/cover.png",
  commentCount: 2,
  tagIds: ["20"],
  createdAt: "2026-06-20T10:00:00",
  createdBy: "1001",
  updatedAt: "2026-06-21T10:00:00",
  updatedBy: "1001"
};

describe("article editor form", () => {
  it("creates a draft form and maps detail without exposing a password", () => {
    expect(createEmptyArticleForm()).toMatchObject({
      status: "DRAFT",
      categoryId: null,
      tagIds: [],
      password: "",
      homepageSlot: "NONE",
      coverAttachmentId: null,
      coverUrl: null
    });

    expect(articleDetailToForm(detail)).toMatchObject({
      titleZh: "中文标题",
      titleJa: "",
      titleEn: "English title",
      body: "# 正文",
      categoryId: "10",
      tagIds: ["20"],
      homepageSlot: "FEATURED",
      password: "",
      coverAttachmentId: "30",
      coverUrl: "http://localhost/media/cover.png"
    });
  });

  it("normalizes optional values and preserves large id strings", () => {
    const form = articleDetailToForm(detail);
    form.titleJa = "  ";
    form.slug = "  Custom-Slug  ";
    form.password = "  ";
    form.tagIds = ["9007199254741202", "9007199254741202"];

    expect(articleFormToPayload(form)).toEqual({
      titleZh: "中文标题",
      titleJa: null,
      titleEn: "English title",
      summaryZh: "中文摘要",
      summaryJa: null,
      summaryEn: null,
      body: "# 正文",
      categoryId: "10",
      tagIds: ["9007199254741202"],
      slug: "custom-slug",
      status: "PASSWORD",
      homepageSlot: "FEATURED",
      password: null,
      publishAt: "2026-06-21T10:00:00",
      coverAttachmentId: "30"
    });
    expect(articleFormToPayload(form)).not.toHaveProperty("coverUrl");
  });

  it("keeps drafts minimal and validates non-draft rules", () => {
    const empty = createEmptyArticleForm();
    expect(validateArticleForm(empty, "create")).toEqual({});

    const scheduled = {
      ...empty,
      titleZh: "标题",
      summaryZh: "摘要",
      body: "正文",
      status: "SCHEDULED" as const
    };
    expect(validateArticleForm(scheduled, "create")).toEqual({
      categoryId: "categoryRequired",
      publishAt: "scheduledRequired"
    });

    const password = { ...scheduled, status: "PASSWORD" as const };
    expect(validateArticleForm(password, "create")).toEqual({
      categoryId: "categoryRequired",
      password: "passwordRequired"
    });
    expect(validateArticleForm(password, "edit")).toEqual({
      categoryId: "categoryRequired"
    });

    expect(
      validateArticleForm(
        { ...scheduled, status: "PUBLISHED" as const },
        "edit"
      )
    ).toEqual({ categoryId: "categoryRequired" });
  });

  it("checks scheduled time, tag limit and slug before sending", () => {
    const form = {
      ...createEmptyArticleForm(),
      titleZh: "标题",
      body: "正文",
      categoryId: "10",
      status: "SCHEDULED" as const,
      publishAt: "2000-01-01T00:00:00",
      tagIds: Array.from({ length: 21 }, (_, index) => String(index + 1)),
      slug: "not valid"
    };

    expect(validateArticleForm(form, "create")).toEqual({
      publishAt: "scheduledFuture",
      tagIds: "tagLimit",
      slug: "slugFormat"
    });
  });
});
