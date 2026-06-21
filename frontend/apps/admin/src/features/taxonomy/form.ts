import type {
  CategoryItem,
  CategoryWritePayload,
  TagItem,
  TagWritePayload
} from "./model";

export type TaxonomyFormErrorCode = "required" | "sortOrderRange";

export interface TagForm {
  nameZh: string;
  nameJa: string;
  nameEn: string;
  slug: string;
}

export interface CategoryForm extends TagForm {
  sortOrder: number;
}

export type CategoryFormErrors = Partial<
  Record<keyof CategoryForm, TaxonomyFormErrorCode>
>;
export type TagFormErrors = Partial<
  Record<keyof TagForm, TaxonomyFormErrorCode>
>;

export function createTagForm(): TagForm {
  return { nameZh: "", nameJa: "", nameEn: "", slug: "" };
}

export function createCategoryForm(): CategoryForm {
  return { ...createTagForm(), sortOrder: 0 };
}

export function tagToForm(tag: TagItem): TagForm {
  return {
    nameZh: tag.nameZh,
    nameJa: tag.nameJa ?? "",
    nameEn: tag.nameEn ?? "",
    slug: tag.slug
  };
}

export function categoryToForm(category: CategoryItem): CategoryForm {
  return { ...tagToForm(category), sortOrder: category.sortOrder };
}

function validateBase(form: TagForm): TagFormErrors {
  const errors: TagFormErrors = {};
  if (!form.nameZh.trim()) errors.nameZh = "required";
  if (!form.slug.trim()) errors.slug = "required";
  return errors;
}

export function validateTagForm(form: TagForm): TagFormErrors {
  return validateBase(form);
}

export function validateCategoryForm(
  form: CategoryForm
): CategoryFormErrors {
  const errors: CategoryFormErrors = validateBase(form);
  if (
    !Number.isInteger(form.sortOrder) ||
    form.sortOrder < 0 ||
    form.sortOrder > 1_000_000
  ) {
    errors.sortOrder = "sortOrderRange";
  }
  return errors;
}

function optional(value: string): string | null {
  const normalized = value.trim();
  return normalized || null;
}

export function tagFormToPayload(form: TagForm): TagWritePayload {
  return {
    nameZh: form.nameZh.trim(),
    nameJa: optional(form.nameJa),
    nameEn: optional(form.nameEn),
    slug: form.slug.trim()
  };
}

export function categoryFormToPayload(
  form: CategoryForm
): CategoryWritePayload {
  return { ...tagFormToPayload(form), sortOrder: form.sortOrder };
}
