import { describe, expect, it } from "vitest";
import { resolveAdminLocale } from "./locale";

describe("resolveAdminLocale", () => {
  it.each([
    ["zh-CN", "zh"],
    ["ja-JP", "ja"],
    ["en-US", "en"],
    ["fr-FR", "zh"]
  ])("maps %s to %s", (language, expected) => {
    expect(resolveAdminLocale(language)).toBe(expected);
  });
});
