export type AdminLocale = "zh" | "ja" | "en";

const LOCALE_KEY = "myblog-admin-locale";

export function resolveAdminLocale(language: string): AdminLocale {
  const normalized = language.toLowerCase();
  if (normalized.startsWith("ja")) return "ja";
  if (normalized.startsWith("en")) return "en";
  return "zh";
}

export function saveAdminLocale(locale: AdminLocale): void {
  localStorage.setItem(LOCALE_KEY, locale);
}

export function loadAdminLocale(systemLanguage: string): AdminLocale {
  const saved = localStorage.getItem(LOCALE_KEY);
  if (saved === "zh" || saved === "ja" || saved === "en") return saved;
  return resolveAdminLocale(systemLanguage);
}
