import { describe, expect, it } from "vitest";
import { lastDaysFilters, validateStatsDashboardFilters } from "./query";

describe("stats dashboard query", () => {
  it("builds quick ranges from the JST calendar", () => {
    expect(lastDaysFilters(7, new Date("2026-07-14T16:00:00Z"))).toEqual({
      from: "2026-07-09",
      to: "2026-07-15"
    });
  });

  it("rejects incomplete, reversed and overlong ranges", () => {
    expect(validateStatsDashboardFilters({})).toBeNull();
    expect(validateStatsDashboardFilters({ from: "2026-07-01" })).toBe(
      "incomplete"
    );
    expect(
      validateStatsDashboardFilters({ from: "2026-07-02", to: "2026-07-01" })
    ).toBe("reversed");
    expect(
      validateStatsDashboardFilters({ from: "2025-07-01", to: "2026-07-02" })
    ).toBe("tooLong");
  });
});
