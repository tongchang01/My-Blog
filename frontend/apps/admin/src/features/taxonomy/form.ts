import type {
  CategoryItem,
  CategoryWritePayload,
  TagItem,
  TagWritePayload
} from "./model";

export type TaxonomyFormErrorCode =
  | "required"
  | "slugFormat"
  | "sortOrderRange"
  | "maxLength";

const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

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
  for (const field of ["nameZh", "nameJa", "nameEn"] as const) {
    if (form[field].trim().length > 64) errors[field] = "maxLength";
  }
  const slug = normalizeSlug(form.slug);
  if (!slug) errors.slug = "required";
  else if (slug.length > 64 || !SLUG_PATTERN.test(slug)) {
    errors.slug = "slugFormat";
  }
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
    slug: normalizeSlug(form.slug)
  };
}

export function categoryFormToPayload(
  form: CategoryForm
): CategoryWritePayload {
  return { ...tagFormToPayload(form), sortOrder: form.sortOrder };
}

export function normalizeSlug(value: string): string {
  return value.trim().toLowerCase();
}
