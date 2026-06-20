import { describe, expect, it } from "vitest";
import { loadAdminLocale, resolveAdminLocale, saveAdminLocale } from "./locale";

describe("resolveAdminLocale", () => {
  it.each([
    ["zh-CN", "zh"],
    ["ja-JP", "ja"],
    ["en-US", "en"],
    ["fr-FR", "zh"]
  ])("maps %s to %s", (language, expected) => {
    expect(resolveAdminLocale(language)).toBe(expected);
  });

  it("persists an explicit locale", () => {
    saveAdminLocale("ja");
    expect(loadAdminLocale("en-US")).toBe("ja");
  });

  it("uses system language on first visit", () => {
    expect(loadAdminLocale("ja-JP")).toBe("ja");
  });
});
