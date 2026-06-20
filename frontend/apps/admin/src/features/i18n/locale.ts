export type AdminLocale = "zh" | "ja" | "en";

export function resolveAdminLocale(language: string): AdminLocale {
  const normalized = language.toLowerCase();
  if (normalized.startsWith("ja")) return "ja";
  if (normalized.startsWith("en")) return "en";
  return "zh";
}
