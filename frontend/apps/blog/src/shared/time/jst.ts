import type { SupportedLocale } from '@/shared/i18n/locale'

const LOCALES: Record<SupportedLocale, string> = {
  zh: 'zh-CN',
  ja: 'ja-JP',
  en: 'en-US'
}

const LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?$/

export const parseJst = (value: string): Date => {
  if (!LOCAL_DATE_TIME.test(value)) throw new TypeError('Invalid JST date')
  const parsed = new Date(`${value}+09:00`)
  if (Number.isNaN(parsed.getTime())) throw new TypeError('Invalid JST date')
  return parsed
}

export const formatJst = (value: string, locale: SupportedLocale): string =>
  new Intl.DateTimeFormat(LOCALES[locale], {
    timeZone: 'Asia/Tokyo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(parseJst(value))
