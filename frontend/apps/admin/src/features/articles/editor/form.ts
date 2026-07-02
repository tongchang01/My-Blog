import type {
  ArticleDetail,
  ArticleHomepageSlot,
  ArticleStatus,
  ArticleWritePayload
} from "../model";

export type ArticleEditorMode = "create" | "edit";
export type ArticleFormErrorCode =
  | "required"
  | "categoryRequired"
  | "scheduledRequired"
  | "passwordRequired";

export interface ArticleForm {
  titleZh: string;
  titleJa: string;
  titleEn: string;
  summaryZh: string;
  summaryJa: string;
  summaryEn: string;
  body: string;
  categoryId: string | null;
  tagIds: string[];
  slug: string;
  status: ArticleStatus;
  homepageSlot: ArticleHomepageSlot;
  password: string;
  publishAt: string | null;
  coverAttachmentId: string | null;
  coverUrl: string | null;
}

export type ArticleFormErrors = Partial<
  Record<keyof ArticleForm, ArticleFormErrorCode>
>;

export function createEmptyArticleForm(): ArticleForm {
  return {
    titleZh: "",
    titleJa: "",
    titleEn: "",
    summaryZh: "",
    summaryJa: "",
    summaryEn: "",
    body: "",
    categoryId: null,
    tagIds: [],
    slug: "",
    status: "DRAFT",
    homepageSlot: "NONE",
    password: "",
    publishAt: null,
    coverAttachmentId: null,
    coverUrl: null
  };
}

export function articleDetailToForm(detail: ArticleDetail): ArticleForm {
  return {
    titleZh: detail.titleZh ?? "",
    titleJa: detail.titleJa ?? "",
    titleEn: detail.titleEn ?? "",
    summaryZh: detail.summaryZh ?? "",
    summaryJa: detail.summaryJa ?? "",
    summaryEn: detail.summaryEn ?? "",
    body: detail.body ?? "",
    categoryId: detail.categoryId,
    tagIds: [...detail.tagIds],
    slug: detail.slug ?? "",
    status: detail.status,
    homepageSlot: detail.homepageSlot,
    password: "",
    publishAt: detail.publishAt,
    coverAttachmentId: detail.coverAttachmentId,
    coverUrl: detail.coverUrl
  };
}

function optional(value: string): string | null {
  const normalized = value.trim();
  return normalized || null;
}

export function validateArticleForm(
  form: ArticleForm,
  mode: ArticleEditorMode
): ArticleFormErrors {
  const errors: ArticleFormErrors = {};
  if (!form.titleZh.trim()) errors.titleZh = "required";
  if (!form.summaryZh.trim()) errors.summaryZh = "required";
  if (!form.body.trim()) errors.body = "required";
  if (form.status !== "DRAFT" && !form.categoryId) {
    errors.categoryId = "categoryRequired";
  }
  if (form.status === "SCHEDULED" && !form.publishAt) {
    errors.publishAt = "scheduledRequired";
  }
  if (
    form.status === "PASSWORD" &&
    mode === "create" &&
    !form.password.trim()
  ) {
    errors.password = "passwordRequired";
  }
  return errors;
}

export function articleFormToPayload(form: ArticleForm): ArticleWritePayload {
  return {
    titleZh: form.titleZh.trim(),
    titleJa: optional(form.titleJa),
    titleEn: optional(form.titleEn),
    summaryZh: form.summaryZh.trim(),
    summaryJa: optional(form.summaryJa),
    summaryEn: optional(form.summaryEn),
    body: form.body.trim(),
    categoryId: form.categoryId,
    tagIds: [...new Set(form.tagIds)],
    slug: optional(form.slug),
    status: form.status,
    homepageSlot: form.homepageSlot,
    password: optional(form.password),
    publishAt: form.publishAt,
    coverAttachmentId: form.coverAttachmentId
  };
}
