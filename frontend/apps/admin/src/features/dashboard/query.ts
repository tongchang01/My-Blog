import type { StatsDashboardFilters } from "./model";

export type StatsDashboardFilterError =
  | "incomplete"
  | "reversed"
  | "tooLong";

function jstDate(daysOffset: number, now: Date): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(now);
  const value = Object.fromEntries(
    parts
      .filter(part => part.type !== "literal")
      .map(part => [part.type, part.value])
  );
  const date = new Date(
    Date.UTC(Number(value.year), Number(value.month) - 1, Number(value.day))
  );
  date.setUTCDate(date.getUTCDate() + daysOffset);
  return date.toISOString().slice(0, 10);
}

export function lastDaysFilters(
  days: number,
  now = new Date()
): StatsDashboardFilters {
  return { from: jstDate(1 - days, now), to: jstDate(0, now) };
}

export function validateStatsDashboardFilters(
  filters: StatsDashboardFilters
): StatsDashboardFilterError | null {
  if (!filters.from && !filters.to) return null;
  if (!filters.from || !filters.to) return "incomplete";
  if (filters.from > filters.to) return "reversed";
  const days =
    (Date.parse(`${filters.to}T00:00:00Z`) -
      Date.parse(`${filters.from}T00:00:00Z`)) /
      86_400_000 +
    1;
  return days > 366 ? "tooLong" : null;
}
