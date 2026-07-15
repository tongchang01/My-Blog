import { describe, expect, it } from "vitest";
import type { CategoryItem, TagItem } from "./model";
import {
  categoryFormToPayload,
  categoryToForm,
  createCategoryForm,
  createTagForm,
  tagFormToPayload,
  tagToForm,
  validateCategoryForm,
  validateTagForm
} from "./form";

const category: CategoryItem = {
  id: "9007199254741202",
  nameZh: "后端",
  nameJa: null,
  nameEn: "Backend",
  slug: "backend",
  sortOrder: 20,
  createdAt: "2026-06-20T10:00:00",
  createdBy: "1001",
  updatedAt: "2026-06-21T10:00:00",
  updatedBy: "1001"
};

const tag: TagItem = {
  id: "9007199254741203",
  nameZh: "Vue",
  nameJa: null,
  nameEn: "Vue",
  slug: "vue",
  createdAt: "2026-06-20T10:00:00",
  createdBy: "1001",
  updatedAt: "2026-06-21T10:00:00",
  updatedBy: "1001"
};

describe("taxonomy forms", () => {
  it("creates defaults and maps detail while preserving string ids", () => {
    expect(createCategoryForm()).toEqual({
      nameZh: "",
      nameJa: "",
      nameEn: "",
      slug: "",
      sortOrder: 0
    });
    expect(createTagForm()).toEqual({
      nameZh: "",
      nameJa: "",
      nameEn: "",
      slug: ""
    });
    expect(categoryToForm(category)).toMatchObject({
      nameZh: "后端",
      nameJa: "",
      sortOrder: 20
    });
    expect(tagToForm(tag)).toMatchObject({ nameZh: "Vue", nameJa: "" });
    expect(category.id).toBe("9007199254741202");
  });

  it("validates required fields, normalized slug format and category sort range", () => {
    expect(validateCategoryForm(createCategoryForm())).toEqual({
      nameZh: "required",
      slug: "required"
    });
    expect(
      validateCategoryForm({
        ...createCategoryForm(),
        nameZh: "分类",
        slug: "category",
        sortOrder: 1_000_001
      })
    ).toEqual({ sortOrder: "sortOrderRange" });
    expect(validateTagForm(createTagForm())).toEqual({
      nameZh: "required",
      slug: "required"
    });
    expect(
      validateTagForm({ ...createTagForm(), nameZh: "标签", slug: "bad_slug" })
    ).toEqual({ slug: "slugFormat" });
  });

  it("normalizes whitespace and optional language names", () => {
    expect(
      categoryFormToPayload({
        nameZh: " 后端 ",
        nameJa: "  ",
        nameEn: " Backend ",
        slug: " Backend ",
        sortOrder: 20
      })
    ).toEqual({
      nameZh: "后端",
      nameJa: null,
      nameEn: "Backend",
      slug: "backend",
      sortOrder: 20
    });
    expect(
      tagFormToPayload({
        nameZh: " Vue ",
        nameJa: " ",
        nameEn: " Vue ",
        slug: " Vue "
      })
    ).toEqual({
      nameZh: "Vue",
      nameJa: null,
      nameEn: "Vue",
      slug: "vue"
    });
  });
});
