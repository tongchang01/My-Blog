export const SUPPORTED_LOCALES = ['zh', 'ja', 'en'] as const

export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const isSupportedLocale = (value: unknown): value is SupportedLocale =>
  typeof value === 'string' &&
  SUPPORTED_LOCALES.includes(value as SupportedLocale)

export const systemLocale = (language: string): SupportedLocale => {
  const normalized = language.toLowerCase()
  if (normalized.startsWith('zh')) return 'zh'
  if (normalized.startsWith('ja')) return 'ja'
  if (normalized.startsWith('en')) return 'en'
  return 'zh'
}

export const resolveInitialLocale = (
  saved: string | null,
  language: string
): SupportedLocale => {
  const migrated = saved === 'zh-CN' ? 'zh' : saved
  return isSupportedLocale(migrated) ? migrated : systemLocale(language)
}
