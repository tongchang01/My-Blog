import type { SupportedLocale } from '@/shared/i18n/locale'

export const localizedPath = (path: string, locale: SupportedLocale): string =>
  `/${locale}${path === '/' ? '' : path}`
