import type {
  AdminLocale,
  ArticleStatus,
  LocalizedNames
} from "./model";

const STATUS_KEYS: Record<ArticleStatus, string> = {
  DRAFT: "articles.status.draft",
  PUBLISHED: "articles.status.published",
  PRIVATE: "articles.status.private",
  PASSWORD: "articles.status.password",
  SCHEDULED: "articles.status.scheduled"
};

function nonEmpty(value: string | null | undefined): string | undefined {
  const normalized = value?.trim();
  return normalized || undefined;
}

export function localizedName(
  names: LocalizedNames,
  locale: AdminLocale
): string {
  const requested =
    locale === "ja"
      ? names.nameJa
      : locale === "en"
        ? names.nameEn
        : names.nameZh;
  return (
    nonEmpty(requested) ??
    nonEmpty(names.nameZh) ??
    nonEmpty(names.nameJa) ??
    nonEmpty(names.nameEn) ??
    "—"
  );
}

export function statusTranslationKey(status: string): string {
  return STATUS_KEYS[status as ArticleStatus] ?? "articles.status.unknown";
}

export function formatJstDateTime(value: string | null): string {
  if (!value) return "—";
  return value.replace("T", " ").slice(0, 16);
}
